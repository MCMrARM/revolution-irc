package io.mrarm.irc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.IRCConnectionRequest;

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

    public ServerConnectionInfo createConnection(ServerConfigData data) {
        IRCConnection connection = new IRCConnection();
        IRCConnectionRequest request = new IRCConnectionRequest();
        request
                .setServerAddress(data.address, data.port)
                .addNick(data.nick).setUser(data.user).setRealName(data.realname); // TODO: a way to set default values
        if (data.ssl) {
            ServerSSLHelper sslHelper = new ServerSSLHelper(null);
            request.enableSSL(sslHelper.createSocketFactory(), sslHelper.createHostnameVerifier());
        }
        ServerConnectionInfo connectionInfo = new ServerConnectionInfo(data.uuid, data.name, connection);
        connection.connect(request, (Void v) -> {
            connectionInfo.setConnected(true);
            connection.joinChannels(data.autojoinChannels, null, null);
        }, null);

        addConnection(connectionInfo);
        return connectionInfo;
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
