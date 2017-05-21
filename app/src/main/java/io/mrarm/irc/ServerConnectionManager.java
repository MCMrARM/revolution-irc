package io.mrarm.irc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ServerConnectionManager {

    private static ServerConnectionManager instance;

    private HashMap<UUID, ServerConnectionInfo> mConnectionsMap = new HashMap<>();
    private ArrayList<ServerConnectionInfo> mConnections = new ArrayList<>();

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
    }

    public ServerConnectionInfo getConnection(UUID uuid) {
        return mConnectionsMap.get(uuid);
    }

}
