package io.mrarm.irc;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ServerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ServerConnectionManager.ConnectionsListener, ServerConfigManager.ConnectionsListener {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CONNECTED_SERVER = 1;
    private static final int TYPE_INACTIVE_SERVER = 2;
    private static final int TYPE_DIVIDER = 3;

    private Context mContext;

    private ActiveServerClickListener mActiveServerClickListener;
    private InactiveServerClickListener mInactiveServerClickListener;
    private InactiveServerLongClickListener mInactiveServerLongClickListener;

    private int mColorConnected;
    private int mColorConnecting;
    private int mColorInactive;

    private int mActiveConnectionCount = 0;
    private int mInactiveConnectionCount = 0;

    public ServerListAdapter(Context context) {
        mContext = context;
        mColorConnected = context.getResources().getColor(R.color.serverListConnected);
        mColorConnecting = context.getResources().getColor(R.color.serverListConnecting);
        mColorInactive = context.getResources().getColor(R.color.serverListInactive);
        updateConnections();
    }

    public void registerListeners() {
        ServerConnectionManager.getInstance().addListener(this);
        ServerConfigManager.getInstance(mContext).addListener(this);
    }

    public void unregisterListeners() {
        ServerConnectionManager.getInstance().removeListener(this);
        ServerConfigManager.getInstance(mContext).removeListener(this);
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        boolean hadHeader = (getActiveHeaderIndex() != -1);
        updateConnections();
        if (hadHeader)
            notifyItemInserted(ServerConnectionManager.getInstance().getConnections().indexOf(connection) + 1 + getActiveHeaderIndex());
        else
            notifyItemRangeChanged(getActiveHeaderIndex(), 2);
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        int oldHeaderIndex = getActiveHeaderIndex();
        updateConnections();
        mActiveConnectionCount--;
        if (getActiveHeaderIndex() == -1 && oldHeaderIndex != -1)
            notifyItemRangeRemoved(oldHeaderIndex, 2);
        else
            notifyItemRemoved(getActiveHeaderIndex() + 1 + ServerConnectionManager.getInstance().getConnections().indexOf(connection));
    }

    @Override
    public void onConnectionAdded(ServerConfigData data) {
        boolean hadHeader = (getActiveHeaderIndex() != -1);
        updateConnections();
        if (hadHeader)
            notifyItemInserted(ServerConfigManager.getInstance(mContext).getServers().indexOf(data) + 1 + getInactiveHeaderIndex());
        else
            notifyItemRangeChanged(getInactiveHeaderIndex(), 2);
    }

    @Override
    public void onConnectionUpdated(ServerConfigData data) {
        notifyItemChanged(getInactiveHeaderIndex() + 1 + ServerConfigManager.getInstance(mContext).getServers().indexOf(data));
    }

    @Override
    public void onConnectionRemoved(ServerConfigData data) {
        int oldHeaderIndex = getInactiveHeaderIndex();
        updateConnections();
        mInactiveConnectionCount--;
        if (getInactiveHeaderIndex() == -1 && oldHeaderIndex != -1)
            notifyItemRangeRemoved(oldHeaderIndex, 2);
        else
            notifyItemRemoved(getInactiveHeaderIndex() + 1 + ServerConfigManager.getInstance(mContext).getServers().indexOf(data));
    }

    public void updateConnections() {
        mActiveConnectionCount = ServerConnectionManager.getInstance().getConnections().size();
        mInactiveConnectionCount = ServerConfigManager.getInstance(mContext).getServers().size();
    }

    public void setActiveServerClickListener(ActiveServerClickListener listener) {
        this.mActiveServerClickListener = listener;
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
                ((ConnectedServerHolder) viewHolder).bind(this, ServerConnectionManager.getInstance().getConnections().get(position - getActiveHeaderIndex() - 1));
                break;
            case TYPE_INACTIVE_SERVER:
                ((ServerHolder) viewHolder).bind(this, ServerConfigManager.getInstance(mContext).getServers().get(position - getInactiveHeaderIndex() - 1));
                break;
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
            mText = (TextView) v.findViewById(R.id.server_list_header);
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
            mName = (TextView) v.findViewById(R.id.server_name);

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
            mIcon = (ImageView) v.findViewById(R.id.server_icon);
            mIconBg = v.findViewById(R.id.server_icon_bg);
            mName = (TextView) v.findViewById(R.id.server_name);
            mDesc = (TextView) v.findViewById(R.id.server_desc);
            v.findViewById(R.id.server_entry).setOnClickListener((View view) -> {
                if (adapter.mActiveServerClickListener != null)
                    adapter.mActiveServerClickListener.onServerClicked(mConnectionInfo);
            });
        }

        public void bind(ServerListAdapter adapter, ServerConnectionInfo connectionInfo) {
            mConnectionInfo = connectionInfo;

            Drawable d = DrawableCompat.wrap(mIconBg.getBackground());
            if (connectionInfo.isConnected()) {
                DrawableCompat.setTint(d, adapter.mColorConnected);
                mIcon.setImageResource(R.drawable.ic_server_connected);
                mDesc.setText(mDesc.getContext().getString(R.string.server_list_connected, connectionInfo.getChannels().size()));
            } else {
                DrawableCompat.setTint(d, adapter.mColorConnecting);
                mIcon.setImageResource(R.drawable.ic_server_connecting);
                mDesc.setText(R.string.server_list_connecting);
            }
            mIconBg.setBackgroundDrawable(d);
            mName.setText(connectionInfo.getName());
        }

    }

    public interface ActiveServerClickListener {
        void onServerClicked(ServerConnectionInfo server);
    }

    public interface InactiveServerClickListener {
        void onServerClicked(ServerConfigData server);
    }

    public interface InactiveServerLongClickListener {
        void onServerLongClicked(ServerConfigData server);
    }

}
