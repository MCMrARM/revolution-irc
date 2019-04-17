package io.mrarm.irc.newui;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;

public class ServerInactiveListData implements ServerConfigManager.ConnectionsListener {

    private final Context mContext;
    private final List<ServerConfigData> mConnections = new ArrayList<>();
    private boolean mLoaded = false;
    private Listener mListener;
    private Filter mFilter;

    public ServerInactiveListData(Context context) {
        mContext = context;
    }

    public void load() {
        ServerConfigManager mgr = ServerConfigManager.getInstance(mContext);
        mgr.addListener(this);
        mConnections.clear();
        for (ServerConfigData s : mgr.getServers()) {
            if (mFilter == null || !mFilter.isFiltered(s))
                mConnections.add(s);
        }
        mLoaded = true;
        if (mFilter != null)
            mFilter.setData(this);
    }

    public void unload() {
        ServerConfigManager mgr = ServerConfigManager.getInstance(mContext);
        mgr.removeListener(this);
        if (mFilter != null)
            mFilter.setData(null);
        mLoaded = false;
    }

    public void setFilter(Filter filter) {
        if (mLoaded)
            throw new RuntimeException("You cannot set a filter after loading data");
        mFilter = filter;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public int size() {
        return mConnections.size();
    }

    public ServerConfigData get(int i) {
        return mConnections.get(i);
    }

    @Override
    public void onConnectionAdded(ServerConfigData data) {
        // make sure the connection is not added already
        if (mConnections.contains(data))
            return;
        if (mFilter != null && mFilter.isFiltered(data))
            return;
        int pos = mConnections.size();
        mConnections.add(data);
        mListener.onServerAdded(pos);
    }

    @Override
    public void onConnectionRemoved(ServerConfigData data) {
        int pos = mConnections.indexOf(data);
        if (pos == -1)
            return;
        mConnections.remove(pos);
        mListener.onServerRemoved(pos);
    }

    @Override
    public void onConnectionUpdated(ServerConfigData data) {
        int pos = mConnections.indexOf(data);
        mListener.onServerUpdated(pos);
    }

    public interface Listener {

        void onServerAdded(int index);

        void onServerRemoved(int index);

        void onServerUpdated(int index);

    }

    public static abstract class Filter {

        private ServerInactiveListData mData;

        private void setData(ServerInactiveListData data) {
            mData = data;
            if (mData != null)
                bind();
            else
                unbind();
        }

        protected void bind() {
        }

        protected void unbind() {
        }

        public abstract boolean isFiltered(ServerConfigData connection);

        public void notifyFilterStatusChanged(UUID connectionUUID, boolean filtered) {
            if (mData == null)
                return;
            if (filtered) {
                for (int i = mData.mConnections.size() - 1; i >= 0; --i) {
                    ServerConfigData d = mData.mConnections.get(i);
                    if (d.uuid.equals(connectionUUID)) {
                        mData.mConnections.remove(i);
                        mData.mListener.onServerRemoved(i);
                        break;
                    }
                }
            } else {
                ServerConfigData s =
                        ServerConfigManager.getInstance(mData.mContext).findServer(connectionUUID);
                mData.onConnectionAdded(s);
            }
        }

    }

    public static class ConnectedFilter extends Filter
            implements ServerConnectionManager.ConnectionsListener {

        private final Context mContext;

        public ConnectedFilter(Context context) {
            mContext = context;
        }

        @Override
        protected void bind() {
            ServerConnectionManager.getInstance(mContext).addListener(this);
        }

        @Override
        protected void unbind() {
            ServerConnectionManager.getInstance(mContext).removeListener(this);
        }

        @Override
        public boolean isFiltered(ServerConfigData connection) {
            return ServerConnectionManager.getInstance(mContext).hasConnection(connection.uuid);
        }

        @Override
        public void onConnectionAdded(ServerConnectionInfo connection) {
            notifyFilterStatusChanged(connection.getUUID(), true);
        }

        @Override
        public void onConnectionRemoved(ServerConnectionInfo connection) {
            notifyFilterStatusChanged(connection.getUUID(), false);
        }
    }
}
