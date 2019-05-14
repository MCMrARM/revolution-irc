package io.mrarm.irc.newui;

import android.content.Context;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.UUID;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.observabletransform.ObservableLists;
import io.mrarm.observabletransform.TransformedObservableList;

public class ServerInactiveListData implements ServerConfigManager.ConnectionsListener,
        ServerConnectionManager.ConnectionsListener {

    private final Context mContext;
    private final ObservableList<ServerConfigData> mConnections = new ObservableArrayList<>();
    private final TransformedObservableList<ServerConfigData> mInactiveConnections;

    public ServerInactiveListData(Context context) {
        mContext = context;
        mInactiveConnections = ObservableLists.filter(mConnections,
                (c) -> !ServerConnectionManager.getInstance(context).hasConnection(c.uuid));
    }

    public ObservableList<ServerConfigData> getConnections() {
        return mConnections;
    }

    public ObservableList<ServerConfigData> getInactiveConnections() {
        return mInactiveConnections;
    }

    public void load() {
        ServerConfigManager mgr = ServerConfigManager.getInstance(mContext);
        mgr.addListener(this);
        mConnections.clear();
        mConnections.addAll(mgr.getServers());
        mInactiveConnections.bind();
        ServerConnectionManager.getInstance(mContext).addListener(this);
    }

    public void unload() {
        ServerConfigManager.getInstance(mContext).removeListener(this);
        ServerConnectionManager.getInstance(mContext).removeListener(this);
        mInactiveConnections.unbind();
    }

    @Override
    public void onConnectionAdded(ServerConfigData data) {
        // make sure the connection is not added already
        if (mConnections.contains(data))
            return;
        mConnections.add(data);
    }

    @Override
    public void onConnectionRemoved(ServerConfigData data) {
        int pos = mConnections.indexOf(data);
        if (pos == -1)
            return;
        mConnections.remove(pos);
    }

    @Override
    public void onConnectionUpdated(ServerConfigData data) {
        int pos = mConnections.indexOf(data);
        mConnections.set(pos, mConnections.get(pos)); // make sure we get notified about the chg
    }

    private int findConnection(UUID uuid) {
        for (int i = mConnections.size() - 1; i >= 0; --i) {
            if (mConnections.get(i).uuid.equals(uuid))
                return i;
        }
        return -1;
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        int index = findConnection(connection.getUUID());
        if (index != -1)
            mInactiveConnections.reapply(index, 1);
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        int index = findConnection(connection.getUUID());
        if (index != -1)
            mInactiveConnections.reapply(index, 1);
    }
}
