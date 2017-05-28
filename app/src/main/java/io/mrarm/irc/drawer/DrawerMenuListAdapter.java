package io.mrarm.irc.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class DrawerMenuListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DRAWER_HEADER = 0;
    private static final int TYPE_SERVER_HEADER = 1;
    private static final int TYPE_CHANNEL = 2;
    private static final int TYPE_DIVIDER = 3;
    private static final int TYPE_MENU_ITEM = 4;

    private List<ServerConnectionInfo> mServers;
    private ArrayList<DrawerMenuItem> mMenuItems = new ArrayList<>();
    private TreeMap<Integer, ServerConnectionInfo> mItemIndexToServerMap = new TreeMap<>();
    private int mCurrentItemCount;
    private ChannelClickListener mChannelClickListener;
    private ServerConnectionInfo mSelectedItemServer;
    private String mSelectedItemChannel;
    private WeakReference<DrawerMenuItem> mSelectedMenuItem;
    private Drawable mChannelBackground;
    private Drawable mChannelSelectedBackground;
    private int mSelectedIconColor;

    public DrawerMenuListAdapter(Context context, List<ServerConnectionInfo> servers) {
        mServers = servers;
        notifyServerListChanged();

        TypedArray ta = context.obtainStyledAttributes(new int[] { R.attr.selectableItemBackground, R.attr.colorControlHighlight, R.attr.colorAccent });
        mChannelBackground = ta.getDrawable(0);
        int color = ta.getColor(1, 0);
        color = ColorUtils.setAlphaComponent(color, Color.alpha(color) / 2);
        mChannelSelectedBackground = new ColorDrawable(color);
        mSelectedIconColor = ta.getColor(2, 0);
        ta.recycle();
    }

    public void addMenuItem(DrawerMenuItem item) {
        this.mMenuItems.add(item);
    }

    public List<DrawerMenuItem> getMenuItems() {
        return mMenuItems;
    }

    public void setChannelClickListener(ChannelClickListener listener) {
        this.mChannelClickListener = listener;
    }

    public void setSelectedChannel(ServerConnectionInfo server, String channel) {
        if (mSelectedMenuItem != null)
            setSelectedMenuItem(null);
        int currentIndex = 1;
        int oldServerIndex = -1;
        int newServerIndex = -1;
        for (ServerConnectionInfo info : mServers) {
            if (info == server)
                newServerIndex = currentIndex;
            if (info == mSelectedItemServer)
                oldServerIndex = currentIndex;
            if (info.isExpandedInDrawer() && info.getChannels() != null)
                currentIndex += info.getChannels().size();
            currentIndex += 2;
        }
        int oldChannelIndex = -1;
        if (mSelectedItemServer != null)
            oldChannelIndex = mSelectedItemServer.getChannels().indexOf(mSelectedItemChannel);
        int newChannelIndex = -1;
        if (server != null)
            newChannelIndex = server.getChannels().indexOf(channel);
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
            notifyItemChanged(mCurrentItemCount + oldSelectedMenuItem);
        if (newSelectedMenuItem != -1)
            notifyItemChanged(mCurrentItemCount + newSelectedMenuItem);
    }

    private void updateItemIndexToServerMap() {
        int currentIndex = 1;
        mItemIndexToServerMap.clear();
        for (ServerConnectionInfo info : mServers) {
            mItemIndexToServerMap.put(currentIndex, info);
            if (info.isExpandedInDrawer() && info.getChannels() != null)
                currentIndex += info.getChannels().size();
            currentIndex += 2;
        }
        mCurrentItemCount = currentIndex;
    }

    public void notifyServerListChanged() {
        updateItemIndexToServerMap();
        notifyDataSetChanged();
    }

    public void notifyServerInfoChanged(ServerConnectionInfo changedInfo) {
        for (Map.Entry<Integer, ServerConnectionInfo> p : mItemIndexToServerMap.entrySet()) {
            if (p.getValue() == changedInfo)
                notifyItemChanged(p.getKey());
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_DRAWER_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.drawer_header, viewGroup, false);
            return new SimpleViewHolder(view);
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
                    mItemIndexToServerMap.floorEntry(position);
            if (viewType == TYPE_SERVER_HEADER)
                ((ServerHeaderHolder) holder).bind(entry.getValue());
            else
                ((ChannelHolder) holder).bind(entry.getValue(), position - entry.getKey() - 1);
        } else if (viewType == TYPE_MENU_ITEM) {
            ((MenuItemHolder) holder).bind(this, mMenuItems.get(position - mCurrentItemCount));
        }
    }

    @Override
    public int getItemCount() {
        return mCurrentItemCount + mMenuItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_DRAWER_HEADER;
        if (position >= mCurrentItemCount)
            return TYPE_MENU_ITEM;
        Map.Entry<Integer, ServerConnectionInfo> entry = mItemIndexToServerMap.floorEntry(position);
        if (entry == null || entry.getKey() == position)
            return TYPE_SERVER_HEADER;
        if (position == entry.getKey() + (entry.getValue().isExpandedInDrawer() &&
                entry.getValue().getChannels() != null ?
                entry.getValue().getChannels().size() : 0) + 1)
            return TYPE_DIVIDER;
        return TYPE_CHANNEL;
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {

        public SimpleViewHolder(View v) {
            super(v);
        }

    }

    public static class ServerHeaderHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private DrawerMenuListAdapter mAdapter;
        private ServerConnectionInfo mServerInfo;
        private TextView mServerName;
        private ImageView mExpandIcon;

        public ServerHeaderHolder(DrawerMenuListAdapter adapter, View v) {
            super(v);
            mAdapter = adapter;
            mServerName = (TextView) v.findViewById(R.id.server_name);
            mExpandIcon = (ImageView) v.findViewById(R.id.server_expand_icon);
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
            mAdapter.updateItemIndexToServerMap();
            for (Map.Entry<Integer, ServerConnectionInfo> entry :
                    mAdapter.mItemIndexToServerMap.entrySet()) {
                if (entry.getValue() == mServerInfo && mServerInfo.getChannels() != null) {
                    if (mServerInfo.isExpandedInDrawer())
                        mAdapter.notifyItemRangeInserted(entry.getKey() + 1,
                                mServerInfo.getChannels().size());
                    else
                        mAdapter.notifyItemRangeRemoved(entry.getKey() + 1,
                                mServerInfo.getChannels().size());
                    break;
                }
            }

            mExpandIcon.setRotation(mServerInfo.isExpandedInDrawer() ? 180.f : 0.f);
            int toDegrees = (mServerInfo.isExpandedInDrawer() ? 0 : 360);
            RotateAnimation rotate = new RotateAnimation(180, toDegrees,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(250);
            rotate.setInterpolator(new AccelerateDecelerateInterpolator());
            mExpandIcon.startAnimation(rotate);
        }

    }

    public static class ChannelHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private DrawerMenuListAdapter mAdapter;
        private View mView;
        private TextView mName;
        private ServerConnectionInfo mConnection;
        private String mChannel;

        public ChannelHolder(DrawerMenuListAdapter adapter, View v) {
            super(v);
            this.mAdapter = adapter;
            mView = v.findViewById(R.id.channel_entry);
            mName = (TextView) v.findViewById(R.id.channel_name);
            mView.setOnClickListener(this);
        }

        public void bind(ServerConnectionInfo info, int channelIndex) {
            mConnection = info;
            mChannel = info.getChannels().get(channelIndex);
            mName.setText(mChannel);

            if (mAdapter.mSelectedItemServer != null && mAdapter.mSelectedItemChannel != null &&
                    mAdapter.mSelectedItemServer == info &&
                    mAdapter.mSelectedItemChannel.equals(mChannel)) {
                mView.setSelected(true);
                mView.setBackgroundDrawable(mAdapter.mChannelSelectedBackground);
            } else {
                mView.setSelected(false);
                mView.setBackgroundDrawable(mAdapter.mChannelBackground.getConstantState().newDrawable());
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
            mName = (TextView) v.findViewById(R.id.item_name);
            mIcon = (ImageView) v.findViewById(R.id.item_icon);
        }

        public void bind(DrawerMenuListAdapter adapter, DrawerMenuItem item) {
            mName.setText(item.getName());
            mView.setOnClickListener(item.mListener);
            mIcon.setImageResource(item.getIcon());
            if (adapter.mSelectedMenuItem != null && adapter.mSelectedMenuItem.get() == item) {
                mView.setSelected(true);
                mView.setBackgroundDrawable(adapter.mChannelSelectedBackground);

                Drawable d = DrawableCompat.wrap(mIcon.getDrawable()).mutate();
                DrawableCompat.setTint(d, adapter.mSelectedIconColor);
                mIcon.setImageDrawable(d);
            } else {
                mView.setSelected(false);
                mView.setBackgroundDrawable(adapter.mChannelBackground.getConstantState().newDrawable());
            }
        }

    }

    public interface ChannelClickListener {

        void openChannel(ServerConnectionInfo server, String channel);

    }

}
