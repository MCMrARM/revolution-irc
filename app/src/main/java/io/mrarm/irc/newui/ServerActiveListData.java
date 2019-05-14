package io.mrarm.irc.newui;

import android.content.Context;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;

public class ServerActiveListData implements ServerConnectionManager.ConnectionsListener {

    private final Context mContext;
    private final ObservableList<ServerConnectionInfo> mConnections = new ObservableArrayList<>();

    public ServerActiveListData(Context context) {
        mContext = context;
    }

    public ObservableList<ServerConnectionInfo> getConnections() {
        return mConnections;
    }

    public void load() {
        ServerConnectionManager connManager = ServerConnectionManager.getInstance(mContext);
        connManager.addListener(this);
        mConnections.clear();
        mConnections.addAll(connManager.getConnections());
    }

    public void unload() {
        ServerConnectionManager connManager = ServerConnectionManager.getInstance(mContext);
        connManager.removeListener(this);
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        mConnections.add(connection);
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        int iof = mConnections.indexOf(connection);
        if (iof == -1)
            return;
        mConnections.remove(iof);
    }

}
