package io.mrarm.irc.newui;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;

public class ServerActiveListData implements ServerConnectionManager.ConnectionsListener {

    private final Context mContext;
    private List<ServerConnectionInfo> mConnections = new ArrayList<>();
    private Listener mListener;

    public ServerActiveListData(Context context) {
        mContext = context;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public int size() {
        return mConnections.size();
    }

    public ServerConnectionInfo get(int i) {
        return mConnections.get(i);
    }


    public void load() {
        ServerConnectionManager connManager = ServerConnectionManager.getInstance(mContext);
        connManager.addListener(this);
        mConnections = connManager.getConnections();
    }

    public void unload() {
        ServerConnectionManager connManager = ServerConnectionManager.getInstance(mContext);
        connManager.removeListener(this);
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        mConnections.add(connection);
        mListener.onActiveConnectionAdded(mConnections.size() - 1);
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        int iof = mConnections.indexOf(connection);
        if (iof == -1)
            return;
        mConnections.remove(iof);
        mListener.onActiveConnectionRemoved(iof);
    }

    public interface Listener {

        void onActiveConnectionAdded(int index);

        void onActiveConnectionRemoved(int index);

    }

}
