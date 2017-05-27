package io.mrarm.irc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ServerConnectionManager {

    private static ServerConnectionManager instance;

    private HashMap<UUID, ServerConnectionInfo> mConnectionsMap = new HashMap<>();
    private ArrayList<ServerConnectionInfo> mConnections = new ArrayList<>();
    private List<ConnectionsListener> mListeners = new ArrayList<>();

    public static ServerConnectionManager getInstance() {
        if (instance == null)
            instance = new ServerConnectionManager();
        return instance;
    }

    public List<ServerConnectionInfo> getConnections() {
        return mConnections;
    }

    public void addConnection(ServerConnectionInfo connection) {
        mConnectionsMap.put(connection.getUUID(), connection);
        mConnections.add(connection);
        for (ConnectionsListener listener : mListeners)
            listener.onConnectionAdded(connection);
    }

    public ServerConnectionInfo getConnection(UUID uuid) {
        return mConnectionsMap.get(uuid);
    }

    public void addListener(ConnectionsListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(ConnectionsListener listener) {
        mListeners.remove(listener);
    }

    public interface ConnectionsListener {

        void onConnectionAdded(ServerConnectionInfo connection);

        void onConnectionRemoved(ServerConnectionInfo connection);

    }

}
