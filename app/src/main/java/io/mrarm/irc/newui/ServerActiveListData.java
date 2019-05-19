package io.mrarm.irc.newui;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import javax.inject.Inject;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.dagger.FragmentScope;

@FragmentScope
public class ServerActiveListData implements ServerConnectionManager.ConnectionsListener {

    @Inject ServerConnectionManager mConnManager;
    private final ObservableList<ServerConnectionInfo> mConnections = new ObservableArrayList<>();

    @Inject ServerActiveListData() { }

    public ObservableList<ServerConnectionInfo> getConnections() {
        return mConnections;
    }

    public void load() {
        mConnManager.addListener(this);
        mConnections.clear();
        mConnections.addAll(mConnManager.getConnections());
    }

    public void unload() {
        mConnManager.removeListener(this);
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
