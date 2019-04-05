package io.mrarm.irc.newui;

import android.content.Context;

import java.util.List;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.util.UiThreadHelper;

public class ServerIconListData implements ServerConnectionManager.ConnectionsListener {

    private final ServerConnectionManager mManager;
    private List<ServerConnectionInfo> mConnections;

    public ServerIconListData(Context context) {
        mManager = ServerConnectionManager.getInstance(context);
    }

    public void load() {
        mManager.addListener(this);
        updateConnections();
    }

    public void unload() {
        mManager.removeListener(this);
    }

    public int size() {
        return mConnections.size();
    }

    public ServerConnectionInfo get(int i) {
        return mConnections.get(i);
    }

    private void updateConnections() {
        UiThreadHelper.runOnUiThread(() -> {
            mConnections = mManager.getConnections();
        });
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        updateConnections();
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        updateConnections();
    }
}
