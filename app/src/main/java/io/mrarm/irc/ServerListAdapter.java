package io.mrarm.irc;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ServerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CONNECTED_SERVER = 1;
    private static final int TYPE_INACTIVE_SERVER = 2;
    private static final int TYPE_DIVIDER = 3;

    private Context mContext;

    private ServerClickListener mServerClickListener;

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

    public void updateConnections() {
        mActiveConnectionCount = ServerConnectionManager.getInstance().getConnections().size();
        mInactiveConnectionCount = ServerConfigManager.getInstance(mContext).getServers().size();
    }

    public void setServerClickListener(ServerClickListener listener) {
        this.mServerClickListener = listener;
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
            return new ServerHolder(view);
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

        public ServerHolder(View v) {
            super(v);
            mIconBg = v.findViewById(R.id.server_icon_bg);
            mName = (TextView) v.findViewById(R.id.server_name);
        }

        public void bind(ServerListAdapter adapter, ServerConfigData data) {
            Drawable d = DrawableCompat.wrap(mIconBg.getBackground());
            DrawableCompat.setTint(d, adapter.mColorInactive);
            mIconBg.setBackgroundDrawable(d);
            mName.setText(data.name);
        }

    }

    public static class ConnectedServerHolder extends RecyclerView.ViewHolder {

        private View mIconBg;
        private TextView mName;
        private TextView mDesc;
        private ServerConnectionInfo mConnectionInfo;

        public ConnectedServerHolder(ServerListAdapter adapter, View v) {
            super(v);
            mIconBg = v.findViewById(R.id.server_icon_bg);
            mName = (TextView) v.findViewById(R.id.server_name);
            mDesc = (TextView) v.findViewById(R.id.server_desc);
            v.findViewById(R.id.server_entry).setOnClickListener((View view) -> {
                if (adapter.mServerClickListener != null)
                    adapter.mServerClickListener.openServer(mConnectionInfo);
            });
        }

        public void bind(ServerListAdapter adapter, ServerConnectionInfo connectionInfo) {
            mConnectionInfo = connectionInfo;

            Drawable d = DrawableCompat.wrap(mIconBg.getBackground());
            if (connectionInfo.isConnected())
                DrawableCompat.setTint(d, adapter.mColorConnected);
            else
                DrawableCompat.setTint(d, adapter.mColorConnecting);
            mIconBg.setBackgroundDrawable(d);
            mName.setText(connectionInfo.getName());
            mDesc.setText(mDesc.getContext().getString(R.string.server_list_connected, connectionInfo.getChannels().size()));
        }

    }

    public interface ServerClickListener {

        void openServer(ServerConnectionInfo server);

    }

}
