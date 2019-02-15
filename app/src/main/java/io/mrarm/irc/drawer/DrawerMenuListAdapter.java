package io.mrarm.irc.drawer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.mrarm.irc.ChannelNotificationManager;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.ExpandIconStateHelper;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.view.LockableDrawerLayout;

public class DrawerMenuListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DRAWER_HEADER = 0;
    private static final int TYPE_SERVER_HEADER = 1;
    private static final int TYPE_CHANNEL = 2;
    private static final int TYPE_DIVIDER = 3;
    private static final int TYPE_MENU_ITEM = 4;

    private Context mContext;
    private LockableDrawerLayout mDrawerLayout;
    private List<ServerConnectionInfo> mServers;
    private ArrayList<DrawerMenuItem> mMenuItems = new ArrayList<>();
    private ArrayList<DrawerMenuItem> mTopMenuItems = new ArrayList<>();
    private TreeMap<Integer, ServerConnectionInfo> mItemIndexToServerMap = new TreeMap<>();
    private int mCurrentItemCount;
    private ChannelClickListener mChannelClickListener;
    private ServerConnectionInfo mSelectedItemServer;
    private String mSelectedItemChannel;
    private WeakReference<DrawerMenuItem> mSelectedMenuItem;
    private Drawable mChannelBackground;
    private Drawable mChannelSelectedBackground;
    private int mDefaultForegroundColor;
    private int mSelectedForegroundColor;
    private int mHeaderPaddingTop = 0;
    private int mCounterWidestLetter = -1;
    private boolean mAlwaysShowServer;

    public DrawerMenuListAdapter(Context context, LockableDrawerLayout drawerLayout,
                                 boolean alwaysShowServer) {
        mContext = context;
        mDrawerLayout = drawerLayout;
        mAlwaysShowServer = alwaysShowServer;
        notifyServerListChanged();

        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(context,
                new int[] { R.attr.colorAccent, R.attr.selectableItemBackground, R.attr.colorControlHighlight, android.R.attr.textColorPrimary });
        mChannelBackground = ta.getDrawable(R.attr.selectableItemBackground);
        int color = ta.getColor(R.attr.colorControlHighlight, 0);
        color = ColorUtils.setAlphaComponent(color, Color.alpha(color) / 2);
        mChannelSelectedBackground = new ColorDrawable(color);
        mSelectedForegroundColor = ta.getColor(R.attr.colorAccent, 0);
        mDefaultForegroundColor = ta.getColor(android.R.attr.textColorPrimary, 0);
        ta.recycle();
    }

    public void setAlwaysShowServer(boolean value) {
        if (mAlwaysShowServer == value)
            return;
        mAlwaysShowServer = value;
        updateItemIndexToServerMap();
        notifyDataSetChanged();
    }

    public void addMenuItem(DrawerMenuItem item) {
        mMenuItems.add(item);
    }

    public void addTopMenuItem(DrawerMenuItem item) {
        mTopMenuItems.add(item);
    }

    public void setHeaderPaddingTop(int paddingTop) {
        if (mHeaderPaddingTop == paddingTop)
            return;
        mHeaderPaddingTop = paddingTop;
        notifyItemChanged(0);
    }

    public void setChannelClickListener(ChannelClickListener listener) {
        mChannelClickListener = listener;
    }

    public int getSelectedItemIndex() {
        if (mSelectedMenuItem != null) {
            DrawerMenuItem menuItem = mSelectedMenuItem.get();
            if (menuItem != null)
                return getBottomMenuItemsStart() + mMenuItems.indexOf(menuItem);
        }
        if (mSelectedItemServer == null || mSelectedItemChannel == null || !mSelectedItemServer.isExpandedInDrawer())
            return -1;
        int currentIndex = getServerListStart();
        int serverIndex = -1;
        for (ServerConnectionInfo info : mServers) {
            if (info == mSelectedItemServer) {
                serverIndex = currentIndex;
                break;
            }
            if (info.isExpandedInDrawer()) {
                if (shouldShowServerItem(info))
                    currentIndex++;
                if (info.getChannels() != null)
                    currentIndex += info.getChannels().size();
            }
            currentIndex += 2;
        }
        return serverIndex + 1 + (shouldShowServerItem(mSelectedItemServer) ? 1 : 0) +
                mSelectedItemServer.getChannels().indexOf(mSelectedItemChannel);
    }

    private boolean shouldShowServerItem(ServerConnectionInfo info) {
        return info.getChannels() == null || info.getChannels().size() == 0 || mAlwaysShowServer;
    }

    public void setSelectedChannel(ServerConnectionInfo server, String channel) {
        if (mSelectedMenuItem != null)
            setSelectedMenuItem(null);
        int currentIndex = getServerListStart();
        int oldServerIndex = -1;
        int newServerIndex = -1;
        for (ServerConnectionInfo info : mServers) {
            if (info == server)
                newServerIndex = currentIndex;
            if (info == mSelectedItemServer)
                oldServerIndex = currentIndex;
            if (info.isExpandedInDrawer()) {
                if (shouldShowServerItem(info))
                    currentIndex++;
                if (info.getChannels() != null)
                    currentIndex += info.getChannels().size();
            }
            currentIndex += 2;
        }
        int oldChannelIndex = -1;
        if (mSelectedItemServer != null) {
            oldChannelIndex = mSelectedItemServer.getChannels().indexOf(mSelectedItemChannel);
            if (shouldShowServerItem(mSelectedItemServer))
                ++oldChannelIndex;
        }
        int newChannelIndex = -1;
        if (server != null) {
            newChannelIndex = server.getChannels().indexOf(channel);
            if (shouldShowServerItem(server))
                ++newChannelIndex;
        }
        mSelectedItemServer = server;
        mSelectedItemChannel = channel;
        if (oldServerIndex != -1 && oldChannelIndex != -1)
            notifyItemChanged(oldChannelIndex + 1 + oldServerIndex);
        if (newServerIndex != -1 && newChannelIndex != -1)
            notifyItemChanged(newChannelIndex + 1 + newServerIndex);
    }

    public void setSelectedMenuItem(DrawerMenuItem item) {
        if (mSelectedItemChannel != null)
            setSelectedChannel(null, null);
        int oldSelectedMenuItem = mSelectedMenuItem == null ? -1 : mMenuItems.indexOf(mSelectedMenuItem.get());
        mSelectedMenuItem = item != null ? new WeakReference<>(item) : null;
        int newSelectedMenuItem = mMenuItems.indexOf(item);
        if (oldSelectedMenuItem != -1)
            notifyItemChanged(getBottomMenuItemsStart() + oldSelectedMenuItem);
        if (newSelectedMenuItem != -1)
            notifyItemChanged(getBottomMenuItemsStart() + newSelectedMenuItem);
    }

    private void updateItemIndexToServerMap() {
        int currentIndex = 0;
        mItemIndexToServerMap.clear();
        for (ServerConnectionInfo info : mServers) {
            mItemIndexToServerMap.put(currentIndex, info);
            if (info.isExpandedInDrawer()) {
                if (shouldShowServerItem(info))
                    currentIndex++;
                if (info.getChannels() != null)
                    currentIndex += info.getChannels().size();
            }
            currentIndex += 2;
        }
        mCurrentItemCount = currentIndex;
    }

    public void notifyServerListChanged() {
        mServers = ServerConnectionManager.getInstance(mContext).getConnections();
        updateItemIndexToServerMap();
        notifyDataSetChanged();
    }

    public void notifyServerInfoChanged(ServerConnectionInfo changedInfo) {
        for (Map.Entry<Integer, ServerConnectionInfo> p : mItemIndexToServerMap.entrySet()) {
            if (p.getValue() == changedInfo)
                notifyItemChanged(getServerListStart() + p.getKey());
        }
    }

    public void notifyChannelUnreadCountChanged(ServerConnectionInfo connection, String channel) {
        int channelIndex = connection.getChannels().indexOf(channel);
        if (channelIndex == -1)
            return;
        if (shouldShowServerItem(connection))
            channelIndex++;
        for (Map.Entry<Integer, ServerConnectionInfo> p : mItemIndexToServerMap.entrySet()) {
            if (p.getValue() == connection) {
                notifyItemChanged(getServerListStart() + p.getKey() + 1 + channelIndex);
            }
        }
    }



    private int getTopMenuItemsStart() {
        return 1;
    }

    private int getServerListStart() {
        return getTopMenuItemsStart() + mTopMenuItems.size() + 1;
    }

    private int getBottomMenuItemsStart() {
        return getServerListStart() + mCurrentItemCount;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_DRAWER_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_header, viewGroup, false);
            return new DrawerHeaderViewHolder(view);
        } else if (viewType == TYPE_DIVIDER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_divider, viewGroup, false);
            return new SimpleViewHolder(view);
        } else if (viewType == TYPE_SERVER_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_server_item, viewGroup, false);
            return new ServerHeaderHolder(this, view);
        } else if (viewType == TYPE_CHANNEL) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_channel_item, viewGroup, false);
            return new ChannelHolder(this, view);
        } else if (viewType == TYPE_MENU_ITEM) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_menu_item, viewGroup, false);
            return new MenuItemHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        if (viewType == TYPE_SERVER_HEADER || viewType == TYPE_CHANNEL) {
            Map.Entry<Integer, ServerConnectionInfo> entry =
                    mItemIndexToServerMap.floorEntry(position - getServerListStart());
            if (viewType == TYPE_SERVER_HEADER)
                ((ServerHeaderHolder) holder).bind(entry.getValue());
            else
                ((ChannelHolder) holder).bind(entry.getValue(),
                        position - getServerListStart() - entry.getKey() - 1);
        } else if (viewType == TYPE_MENU_ITEM) {
            DrawerMenuItem item;
            if (position >= getBottomMenuItemsStart())
                item = mMenuItems.get(position - getBottomMenuItemsStart());
            else
                item = mTopMenuItems.get(position - getTopMenuItemsStart());
            ((MenuItemHolder) holder).bind(this, item);
        } else if (viewType == TYPE_DRAWER_HEADER) {
            holder.itemView.setPadding(0, mHeaderPaddingTop, 0, 0);
        } else if (viewType == TYPE_DIVIDER) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            if (position == getTopMenuItemsStart() + mTopMenuItems.size())
                p.topMargin = mContext.getResources().getDimensionPixelSize(R.dimen.drawer_search_margin);
            else
                p.topMargin = mContext.getResources().getDimensionPixelSize(R.dimen.drawer_divider_margin);
            holder.itemView.setLayoutParams(p);
        }
    }

    @Override
    public int getItemCount() {
        return getBottomMenuItemsStart() + mMenuItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_DRAWER_HEADER;
        if (position == getTopMenuItemsStart() + mTopMenuItems.size())
            return TYPE_DIVIDER;
        if (position < getServerListStart() || position >= getBottomMenuItemsStart())
            return TYPE_MENU_ITEM;
        position -= getServerListStart();
        Map.Entry<Integer, ServerConnectionInfo> entry = mItemIndexToServerMap.floorEntry(position);
        if (entry == null || entry.getKey() == position)
            return TYPE_SERVER_HEADER;
        int cnt = 0;
        if (entry.getValue().isExpandedInDrawer()) {
            if (shouldShowServerItem(entry.getValue()))
                cnt++;
            if (entry.getValue().getChannels() != null)
                cnt += entry.getValue().getChannels().size();
        }
        if (position == entry.getKey() + cnt + 1)
            return TYPE_DIVIDER;
        return TYPE_CHANNEL;
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {

        public SimpleViewHolder(View v) {
            super(v);
        }

    }

    public class DrawerHeaderViewHolder extends RecyclerView.ViewHolder
            implements LockableDrawerLayout.LockableStateListener {

        private View mPinIcon;

        public DrawerHeaderViewHolder(View v) {
            super(v);
            mPinIcon = v.findViewById(R.id.pin_icon);
            mPinIcon.setSelected(mDrawerLayout.isLocked());
            mPinIcon.setOnClickListener((View view) -> {
                mDrawerLayout.setLocked(!mDrawerLayout.isLocked());
                AppSettings.setDrawerPinned(mDrawerLayout.isLocked());
                mPinIcon.setSelected(mDrawerLayout.isLocked());
            });
            onLockableStateChanged(mDrawerLayout.isLockable());
            mDrawerLayout.addLockableStateListener(this); // no need to unregister as it adds a weakref
        }

        @Override
        public void onLockableStateChanged(boolean lockable) {
            mPinIcon.setVisibility(mDrawerLayout.isLockable() ? View.VISIBLE : View.GONE);
        }

    }

    public class ServerHeaderHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ServerConnectionInfo mServerInfo;
        private TextView mServerName;
        private ImageView mExpandIcon;

        public ServerHeaderHolder(DrawerMenuListAdapter adapter, View v) {
            super(v);
            mServerName = v.findViewById(R.id.server_name);
            mExpandIcon = v.findViewById(R.id.server_expand_icon);
            v.findViewById(R.id.server_entry).setOnClickListener(this);
        }

        public void bind(ServerConnectionInfo info) {
            mServerInfo = info;
            mServerName.setText(info.getName());
            mExpandIcon.setRotation(mServerInfo.isExpandedInDrawer() ? 180.f : 0.f);
        }

        @Override
        public void onClick(View v) {
            mServerInfo.setExpandedInDrawer(!mServerInfo.isExpandedInDrawer());
            updateItemIndexToServerMap();
            for (Map.Entry<Integer, ServerConnectionInfo> entry :
                    mItemIndexToServerMap.entrySet()) {
                if (entry.getValue() == mServerInfo && mServerInfo.getChannels() != null) {
                    int channelCount = mServerInfo.getChannels().size();
                    if (shouldShowServerItem(mServerInfo))
                        ++channelCount;
                    if (mServerInfo.isExpandedInDrawer())
                        notifyItemRangeInserted(getServerListStart() + entry.getKey() + 1,
                                channelCount);
                    else
                        notifyItemRangeRemoved(getServerListStart() + entry.getKey() + 1,
                                channelCount);
                    break;
                }
            }

            ExpandIconStateHelper.animateSetExpanded(mExpandIcon, mServerInfo.isExpandedInDrawer());
        }

    }

    public class ChannelHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private DrawerMenuListAdapter mAdapter;
        private View mView;
        private TextView mName;
        private TextView mUnreadCounter;
        private ServerConnectionInfo mConnection;
        private String mChannel;

        public ChannelHolder(DrawerMenuListAdapter adapter, View v) {
            super(v);
            this.mAdapter = adapter;
            mView = v.findViewById(R.id.entry);
            mName = v.findViewById(R.id.text);
            mUnreadCounter = v.findViewById(R.id.unread_counter);
            mView.setOnClickListener(this);
            if (mCounterWidestLetter == -1) {
                for (int i = 0; i < 9; i++)
                    mCounterWidestLetter = Math.max(mCounterWidestLetter, (int) mUnreadCounter.getPaint().measureText(String.valueOf(i)));
            }
        }

        public void bind(ServerConnectionInfo info, int channelIndex) {
            mConnection = info;
            if (shouldShowServerItem(info))
                --channelIndex;
            List<String> channels = info.getChannels();
            if (channelIndex >= 0 && channelIndex < channels.size()) {
                mChannel = channels.get(channelIndex);
                mName.setText(mChannel);
            } else {
                mChannel = null;
                mName.setText(R.string.tab_server);
            }

            if (mAdapter.mSelectedItemServer != null && mAdapter.mSelectedItemServer == info &&
                    (mAdapter.mSelectedItemChannel == mChannel ||
                    (mAdapter.mSelectedItemChannel != null &&
                    mAdapter.mSelectedItemChannel.equals(mChannel)))) {
                mView.setSelected(true);
                mView.setBackgroundDrawable(mAdapter.mChannelSelectedBackground);
                mName.setTextColor(mAdapter.mSelectedForegroundColor);
            } else {
                mView.setSelected(false);
                mView.setBackgroundDrawable(mAdapter.mChannelBackground.getConstantState().newDrawable());
                mName.setTextColor(mAdapter.mDefaultForegroundColor);
            }

            ChannelNotificationManager notificationManager = info.getNotificationManager().getChannelManager(mChannel, false);
            int unreadMessageCount = notificationManager == null ? 0 : notificationManager.getUnreadMessageCount();
            if (unreadMessageCount > 0 && !mView.isSelected()) {
                mUnreadCounter.setVisibility(View.VISIBLE);
                mUnreadCounter.setText(String.valueOf(unreadMessageCount));
                ViewGroup.LayoutParams params = mUnreadCounter.getLayoutParams();
                params.width = mCounterWidestLetter * mUnreadCounter.getText().length();
                mUnreadCounter.setLayoutParams(params);
            } else {
                mUnreadCounter.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            if (mAdapter.mChannelClickListener != null)
                mAdapter.mChannelClickListener.openChannel(mConnection, mChannel);
        }

    }

    public static class MenuItemHolder extends RecyclerView.ViewHolder {

        private View mView;
        private TextView mName;
        private ImageView mIcon;

        public MenuItemHolder(View v) {
            super(v);
            mView = v.findViewById(R.id.item_entry);
            mName = v.findViewById(R.id.item_name);
            mIcon = v.findViewById(R.id.item_icon);
        }

        public void bind(DrawerMenuListAdapter adapter, DrawerMenuItem item) {
            mName.setText(item.getName());
            mView.setOnClickListener(item.mListener);
            mIcon.setImageResource(item.getIcon());
            if (adapter.mSelectedMenuItem != null && adapter.mSelectedMenuItem.get() == item) {
                mView.setSelected(true);
                mView.setBackgroundDrawable(adapter.mChannelSelectedBackground);
                mName.setTextColor(adapter.mSelectedForegroundColor);

                Drawable d = DrawableCompat.wrap(mIcon.getDrawable()).mutate();
                DrawableCompat.setTint(d, adapter.mSelectedForegroundColor);
                mIcon.setImageDrawable(d);
            } else {
                mView.setSelected(false);
                mView.setBackgroundDrawable(adapter.mChannelBackground.getConstantState().newDrawable());
                mName.setTextColor(adapter.mDefaultForegroundColor);

                if (adapter.mDefaultForegroundColor != 0xFF000000) {
                    Drawable d = DrawableCompat.wrap(mIcon.getDrawable()).mutate();
                    DrawableCompat.setTint(d, adapter.mDefaultForegroundColor);
                    mIcon.setImageDrawable(d);
                }
            }
        }

    }

    public interface ChannelClickListener {

        void openChannel(ServerConnectionInfo server, String channel);

    }

}
