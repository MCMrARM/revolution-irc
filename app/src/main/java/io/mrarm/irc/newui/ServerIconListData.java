package io.mrarm.irc.newui;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.util.UiThreadHelper;

public class ServerIconListData implements ServerConnectionManager.ConnectionsListener {

    private final ServerConnectionManager mManager;
    private List<ServerConnectionInfo> mConnections;
    private final List<Runnable> mListeners = new ArrayList<>();

    public ServerIconListData(Context context) {
        mManager = ServerConnectionManager.getInstance(context);
    }

    public void addListener(Runnable r) {
        mListeners.add(r);
    }

    public void load() {
        mManager.addListener(this);
        updateConnections();
    }

    public void unload() {
        mManager.removeListener(this);
    }

    public List<ServerConnectionInfo> getList() {
        return mConnections;
    }

    private void updateConnections() {
        UiThreadHelper.runOnUiThread(() -> {
            mConnections = mManager.getConnections();
            for (Runnable r : mListeners)
                r.run();
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
