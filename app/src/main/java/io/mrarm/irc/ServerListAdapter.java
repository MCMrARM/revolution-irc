package io.mrarm.irc;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;

public class ServerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ServerConnectionManager.ConnectionsListener,
        ServerConnectionInfo.InfoChangeListener, ServerConnectionInfo.ChannelListChangeListener,
        ServerConfigManager.ConnectionsListener {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CONNECTED_SERVER = 1;
    private static final int TYPE_INACTIVE_SERVER = 2;
    private static final int TYPE_DIVIDER = 3;

    private static final Integer DISABLE_ANIM = 10;

    private Activity mContext;
    private boolean mHasRegisteredListeners = false;

    private List<ServerConfigData> mFilteredInactiveServers = new ArrayList<>();

    private ActiveServerClickListener mActiveServerClickListener;
    private ActiveServerLongClickListener mActiveServerLongClickListener;
    private InactiveServerClickListener mInactiveServerClickListener;
    private InactiveServerLongClickListener mInactiveServerLongClickListener;

    private int mColorConnected;
    private int mColorConnecting;
    private int mColorDisconnected;
    private int mColorInactive;

    private int mActiveConnectionCount = 0;
    private int mInactiveConnectionCount = 0;

    public ServerListAdapter(Activity context) {
        mContext = context;
        mColorConnected = context.getResources().getColor(R.color.serverListConnected);
        mColorConnecting = context.getResources().getColor(R.color.serverListConnecting);
        mColorDisconnected = context.getResources().getColor(R.color.serverListDisconnected);
        mColorInactive = context.getResources().getColor(R.color.serverListInactive);
        updateConnections();
    }

    public void registerListeners() {
        if (mHasRegisteredListeners)
            return;
        ServerConnectionManager.getInstance(mContext).addListener(this);
        ServerConnectionManager.getInstance(mContext).addGlobalConnectionInfoListener(this);
        ServerConnectionManager.getInstance(mContext).addGlobalChannelListListener(this);
        ServerConfigManager.getInstance(mContext).addListener(this);
        mHasRegisteredListeners = true;
    }

    public void unregisterListeners() {
        if (!mHasRegisteredListeners)
            return;
        ServerConnectionManager.getInstance(mContext).removeListener(this);
        ServerConnectionManager.getInstance(mContext).removeGlobalConnectionInfoListener(this);
        ServerConnectionManager.getInstance(mContext).removeGlobalChannelListListener(this);
        ServerConfigManager.getInstance(mContext).removeListener(this);
        mHasRegisteredListeners = false;
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        mContext.runOnUiThread(() -> {
            boolean hadHeader = (getActiveHeaderIndex() != -1);
            int oldInactiveIndex = -1;
            int oldDividerIndex = getDividerIndex();
            for (int i = 0; i < mFilteredInactiveServers.size(); i++) {
                if (mFilteredInactiveServers.get(i).uuid == connection.getUUID()) {
                    oldInactiveIndex = i;
                    break;
                }
            }
            updateConnections();
            if (hadHeader)
                notifyItemInserted(ServerConnectionManager.getInstance(mContext).getConnections().indexOf(connection) + 1 + getActiveHeaderIndex());
            else
                notifyItemRangeInserted(getActiveHeaderIndex(), 3);
            if (oldInactiveIndex != -1 && getInactiveHeaderIndex() != -1)
                notifyItemRemoved(getInactiveHeaderIndex() + 1 + oldInactiveIndex);
            else if (oldInactiveIndex != -1)
                notifyItemRangeRemoved(oldDividerIndex, 3);
        });
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        mContext.runOnUiThread(() -> {
            updateConnections();
            notifyDataSetChanged();
        });
    }

    @Override
    public void onConnectionInfoChanged(ServerConnectionInfo connection) {
        mContext.runOnUiThread(() -> {
            notifyItemChanged(getActiveHeaderIndex() + 1 + ServerConnectionManager.getInstance(mContext).getConnections().indexOf(connection));
        });
    }

    @Override
    public void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        mContext.runOnUiThread(() -> {
            notifyItemChanged(getActiveHeaderIndex() + 1 + ServerConnectionManager.getInstance(mContext).getConnections().indexOf(connection), DISABLE_ANIM);
        });
    }

    @Override
    public void onConnectionAdded(ServerConfigData data) {
        mContext.runOnUiThread(() -> {
            boolean hadHeader = (getActiveHeaderIndex() != -1);
            updateConnections();
            if (hadHeader)
                notifyItemInserted(mFilteredInactiveServers.indexOf(data) + 1 + getInactiveHeaderIndex());
            else
                notifyItemRangeChanged(getDividerIndex(), 3);
        });
    }

    @Override
    public void onConnectionUpdated(ServerConfigData data) {
        mContext.runOnUiThread(() -> {
            notifyItemChanged(getInactiveHeaderIndex() + 1 + mFilteredInactiveServers.indexOf(data));
        });
    }

    @Override
    public void onConnectionRemoved(ServerConfigData data) {
        mContext.runOnUiThread(() -> {
            int oldHeaderIndex = getInactiveHeaderIndex();
            int oldEntryIndex = mFilteredInactiveServers.indexOf(data);
            updateConnections();
            if (getInactiveHeaderIndex() == -1 && oldHeaderIndex != -1)
                notifyItemRangeRemoved(oldHeaderIndex - 1, 3);
            else
                notifyItemRemoved(getInactiveHeaderIndex() + 1 + oldEntryIndex);
        });
    }

    public void updateConnections() {
        ServerConnectionManager manager = ServerConnectionManager.getInstance(mContext);
        mActiveConnectionCount = manager.getConnections().size();
        mFilteredInactiveServers.clear();
        for (ServerConfigData data : ServerConfigManager.getInstance(mContext).getServers()) {
            if (!manager.hasConnection(data.uuid))
                mFilteredInactiveServers.add(data);
        }
        mInactiveConnectionCount = mFilteredInactiveServers.size();
    }

    public void setActiveServerClickListener(ActiveServerClickListener listener) {
        this.mActiveServerClickListener = listener;
    }

    public void setActiveServerLongClickListener(ActiveServerLongClickListener listener) {
        this.mActiveServerLongClickListener = listener;
    }

    public void setInactiveServerClickListener(InactiveServerClickListener listener) {
        this.mInactiveServerClickListener = listener;
    }

    public void setInactiveServerLongClickListener(InactiveServerLongClickListener listener) {
        this.mInactiveServerLongClickListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.server_list_header, viewGroup, false);
            return new HeaderHolder(view);
        } else if (viewType == TYPE_CONNECTED_SERVER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.server_list_connected_server, viewGroup, false);
            return new ConnectedServerHolder(this, view);
        } else if (viewType == TYPE_INACTIVE_SERVER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.server_list_server, viewGroup, false);
            return new ServerHolder(this, view);
        } else {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.server_list_divider, viewGroup, false);
            return new DividerHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        switch (viewHolder.getItemViewType()) {
            case TYPE_HEADER:
                ((HeaderHolder) viewHolder).bind((position == getInactiveHeaderIndex() ? R.string.server_list_header_inactive : R.string.server_list_header_active));
                break;
            case TYPE_CONNECTED_SERVER:
                ((ConnectedServerHolder) viewHolder).bind(this, ServerConnectionManager.getInstance(mContext).getConnections().get(position - getActiveHeaderIndex() - 1));
                break;
            case TYPE_INACTIVE_SERVER:
                ((ServerHolder) viewHolder).bind(this, mFilteredInactiveServers.get(position - getInactiveHeaderIndex() - 1));
                break;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        onBindViewHolder(holder, position);
        if (payloads.contains(DISABLE_ANIM)) {
            holder.itemView.clearAnimation();
        }
    }

    @Override
    public int getItemCount() {
        int ret = 0;
        if (mActiveConnectionCount > 0)
            ret += 1 + mActiveConnectionCount; // header + count
        if (mActiveConnectionCount > 0 && mInactiveConnectionCount > 0)
            ret++; // divider
        if (mInactiveConnectionCount > 0)
            ret += 1 + mInactiveConnectionCount; // header + count
        return ret;
    }

    private int getActiveHeaderIndex() {
        return (mActiveConnectionCount > 0 ? 0 : -1);
    }

    private int getInactiveHeaderIndex() {
        return (mInactiveConnectionCount > 0 ? (mActiveConnectionCount > 0 ? (1 + mActiveConnectionCount + 1) : 0) : -1);
    }

    private int getDividerIndex() {
        return (mInactiveConnectionCount > 0 && mActiveConnectionCount > 0 ? (1 + mActiveConnectionCount) : -1);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getActiveHeaderIndex() || position == getInactiveHeaderIndex())
            return TYPE_HEADER;
        if (position == getDividerIndex())
            return TYPE_DIVIDER;
        if (getInactiveHeaderIndex() != -1 && position > getInactiveHeaderIndex())
            return TYPE_INACTIVE_SERVER;
        return TYPE_CONNECTED_SERVER;
    }

    public static class DividerHolder extends RecyclerView.ViewHolder {

        public DividerHolder(View v) {
            super(v);
        }

    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public HeaderHolder(View v) {
            super(v);
            mText = v.findViewById(R.id.server_list_header);
        }

        public void bind(int stringId) {
            mText.setText(stringId);
        }

    }

    public static class ServerHolder extends RecyclerView.ViewHolder {

        private View mIconBg;
        private TextView mName;
        private ServerConfigData mConfigData;

        public ServerHolder(ServerListAdapter adapter, View v) {
            super(v);
            mIconBg = v.findViewById(R.id.server_icon_bg);
            mName = v.findViewById(R.id.server_name);

            View mainView = v.findViewById(R.id.server_entry);
            mainView.setOnClickListener((View view) -> {
                if (adapter.mInactiveServerClickListener != null)
                    adapter.mInactiveServerClickListener.onServerClicked(mConfigData);
            });
            mainView.setOnLongClickListener((View view) -> {
                if (adapter.mInactiveServerLongClickListener != null) {
                    adapter.mInactiveServerLongClickListener.onServerLongClicked(mConfigData);
                    return true;
                }
                return false;
            });
        }

        public void bind(ServerListAdapter adapter, ServerConfigData data) {
            mConfigData = data;
            Drawable d = DrawableCompat.wrap(mIconBg.getBackground());
            DrawableCompat.setTint(d, adapter.mColorInactive);
            mIconBg.setBackgroundDrawable(d);
            mName.setText(data.name);
        }

    }

    public static class ConnectedServerHolder extends RecyclerView.ViewHolder {

        private ImageView mIcon;
        private View mIconBg;
        private TextView mName;
        private TextView mDesc;
        private ServerConnectionInfo mConnectionInfo;

        public ConnectedServerHolder(ServerListAdapter adapter, View v) {
            super(v);
            mIcon = v.findViewById(R.id.server_icon);
            mIconBg = v.findViewById(R.id.server_icon_bg);
            mName = v.findViewById(R.id.server_name);
            mDesc = v.findViewById(R.id.server_desc);

            View mainView = v.findViewById(R.id.server_entry);
            mainView.setOnClickListener((View view) -> {
                if (adapter.mActiveServerClickListener != null)
                    adapter.mActiveServerClickListener.onServerClicked(mConnectionInfo);
            });
            mainView.setOnLongClickListener((View view) -> {
                if (adapter.mActiveServerLongClickListener != null) {
                    adapter.mActiveServerLongClickListener.onServerLongClicked(mConnectionInfo);
                    return true;
                }
                return false;
            });
        }

        public void bind(ServerListAdapter adapter, ServerConnectionInfo connectionInfo) {
            mConnectionInfo = connectionInfo;

            Drawable d = DrawableCompat.wrap(mIconBg.getBackground());
            if (connectionInfo.isConnected()) {
                DrawableCompat.setTint(d, adapter.mColorConnected);
                mIcon.setImageResource(R.drawable.ic_server_connected);
                int channels = connectionInfo.getChannels().size();
                mDesc.setText(mDesc.getResources().getQuantityString(R.plurals.server_list_connected, channels, channels));
            } else if (connectionInfo.isConnecting()) {
                DrawableCompat.setTint(d, adapter.mColorConnecting);
                mIcon.setImageResource(R.drawable.ic_refresh);
                mDesc.setText(R.string.server_list_connecting);
            } else {
                DrawableCompat.setTint(d, adapter.mColorDisconnected);
                mIcon.setImageResource(R.drawable.ic_close);
                mDesc.setText(R.string.server_list_disconnected);
            }
            mIconBg.setBackgroundDrawable(d);
            mName.setText(connectionInfo.getName());
        }

    }

    public interface ActiveServerClickListener {
        void onServerClicked(ServerConnectionInfo server);
    }

    public interface ActiveServerLongClickListener {
        void onServerLongClicked(ServerConnectionInfo server);
    }

    public interface InactiveServerClickListener {
        void onServerClicked(ServerConfigData server);
    }

    public interface InactiveServerLongClickListener {
        void onServerLongClicked(ServerConfigData server);
    }

}
