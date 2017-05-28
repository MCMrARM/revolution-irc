package io.mrarm.irc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
    private List<ServerConnectionInfo.ChannelListChangeListener> mChannelsListeners = new ArrayList<>();
    private List<ServerConnectionInfo.InfoChangeListener> mInfoListeners = new ArrayList<>();

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

    public ServerConnectionInfo createConnection(ServerConfigData data, Context context) {
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        IRCConnection connection = new IRCConnection();
        IRCConnectionRequest request = new IRCConnectionRequest();
        request
                .setServerAddress(data.address, data.port);
        if (data.nick != null)
            request.addNick(data.nick);
        else
            request.addNick(defaultPrefs.getString("default_nick", null));
        if (data.user != null)
            request.setUser(data.user);
        else
            request.setUser(defaultPrefs.getString("default_user", null));
        if (data.realname != null)
            request.setRealName(data.realname);
        else
            request.setRealName(defaultPrefs.getString("default_user", null));

        if (data.ssl) {
            ServerSSLHelper sslHelper = new ServerSSLHelper(null);
            request.enableSSL(sslHelper.createSocketFactory(), sslHelper.createHostnameVerifier());
        }
        ServerConnectionInfo connectionInfo = new ServerConnectionInfo(this, data.uuid, data.name, connection);
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

    public boolean hasConnection(UUID uuid) {
        return mConnectionsMap.containsKey(uuid);
    }

    public void addListener(ConnectionsListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(ConnectionsListener listener) {
        mListeners.remove(listener);
    }

    public void addGlobalChannelInfoListener(ServerConnectionInfo.InfoChangeListener listener) {
        mInfoListeners.add(listener);
    }

    public void removeGlobalChannelInfoListener(ServerConnectionInfo.InfoChangeListener listener) {
        mInfoListeners.remove(listener);
    }

    public void addGlobalChannelListListener(ServerConnectionInfo.ChannelListChangeListener listener) {
        mChannelsListeners.add(listener);
    }

    public void removeGlobalChannelListListener(ServerConnectionInfo.ChannelListChangeListener listener) {
        mChannelsListeners.remove(listener);
    }

    void notifyConnectionInfoChanged(ServerConnectionInfo connection) {
        for (ServerConnectionInfo.InfoChangeListener listener : mInfoListeners)
            listener.onConnectionInfoChanged(connection);
    }

    void notifyChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        for (ServerConnectionInfo.ChannelListChangeListener listener : mChannelsListeners)
            listener.onChannelListChanged(connection, newChannels);
    }

    public interface ConnectionsListener {

        void onConnectionAdded(ServerConnectionInfo connection);

        void onConnectionRemoved(ServerConnectionInfo connection);

    }

}
