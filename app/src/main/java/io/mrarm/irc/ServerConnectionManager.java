package io.mrarm.irc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.IRCConnectionRequest;
import io.mrarm.irc.util.ReconnectIntervalPreference;
import io.mrarm.irc.util.SettingsHelper;

public class ServerConnectionManager {

    private static ServerConnectionManager instance;

    private Context mContext;
    private HashMap<UUID, ServerConnectionInfo> mConnectionsMap = new HashMap<>();
    private ArrayList<ServerConnectionInfo> mConnections = new ArrayList<>();
    private List<ConnectionsListener> mListeners = new ArrayList<>();
    private List<ServerConnectionInfo.ChannelListChangeListener> mChannelsListeners = new ArrayList<>();
    private List<ServerConnectionInfo.InfoChangeListener> mInfoListeners = new ArrayList<>();

    public static ServerConnectionManager getInstance(Context context) {
        if (instance == null && context != null)
            instance = new ServerConnectionManager(context.getApplicationContext());
        return instance;
    }

    public ServerConnectionManager(Context context) {
        mContext = context;

        ServerConfigManager configManager = ServerConfigManager.getInstance(context);
        SettingsHelper settings = SettingsHelper.getInstance(mContext);
        List<UUID> uuids = settings.getAutoConnectServerList();
        if (uuids != null) {
            for (UUID uuid : uuids) {
                ServerConfigData configData = configManager.findServer(uuid);
                if (configData != null)
                    createConnection(configData, false);
            }
        }
    }

    private void saveAutoconnectList() {
        List<UUID> uuids = new ArrayList<>();
        uuids.addAll(mConnectionsMap.keySet());
        SettingsHelper settings = SettingsHelper.getInstance(mContext);
        settings.setAutoConnectServerList(uuids);
    }

    public Context getContext() {
        return mContext;
    }

    public List<ServerConnectionInfo> getConnections() {
        return mConnections;
    }

    public void addConnection(ServerConnectionInfo connection, boolean saveAutoconnect) {
        mConnectionsMap.put(connection.getUUID(), connection);
        mConnections.add(connection);
        if (saveAutoconnect)
            saveAutoconnectList();
        for (ConnectionsListener listener : mListeners)
            listener.onConnectionAdded(connection);
    }

    public void addConnection(ServerConnectionInfo connection) {
        addConnection(connection, true);
    }

    private ServerConnectionInfo createConnection(ServerConfigData data, boolean saveAutoconnect) {
        SettingsHelper settings = SettingsHelper.getInstance(mContext);

        IRCConnectionRequest request = new IRCConnectionRequest();
        request
                .setServerAddress(data.address, data.port);
        if (data.nicks != null && data.nicks.size() > 0) {
            for (String nick : data.nicks)
                request.addNick(nick);
        } else
            request.addNick(settings.getDefaultNick());
        if (data.user != null)
            request.setUser(data.user);
        else
            request.setUser(settings.getDefaultUser());
        if (data.realname != null)
            request.setRealName(data.realname);
        else
            request.setRealName(settings.getDefaultRealname());

        if (data.pass != null)
            request.setServerPass(data.pass);

        if (data.ssl) {
            ServerSSLHelper sslHelper = new ServerSSLHelper(null);
            request.enableSSL(sslHelper.createSocketFactory(), sslHelper.createHostnameVerifier());
        }
        ServerConnectionInfo connectionInfo = new ServerConnectionInfo(this, data.uuid, data.name, request, data.autojoinChannels);
        connectionInfo.connect();
        addConnection(connectionInfo, saveAutoconnect);
        return connectionInfo;
    }

    public ServerConnectionInfo createConnection(ServerConfigData data) {
        return createConnection(data, true);
    }

    public void removeConnection(ServerConnectionInfo connection, boolean saveAutoconnect) {
        mConnections.remove(connection);
        mConnectionsMap.remove(connection.getUUID());
        for (ConnectionsListener listener : mListeners)
            listener.onConnectionRemoved(connection);
        if (saveAutoconnect)
            saveAutoconnectList();
    }


    public void removeConnection(ServerConnectionInfo connection) {
        removeConnection(connection, true);
    }

    public ServerConnectionInfo getConnection(UUID uuid) {
        return mConnectionsMap.get(uuid);
    }

    public boolean hasConnection(UUID uuid) {
        return mConnectionsMap.containsKey(uuid);
    }

    int getReconnectDelay(int attemptNumber) {
        SettingsHelper settings = SettingsHelper.getInstance(mContext);
        if (!settings.isReconnectEnabled())
            return -1;
        List<ReconnectIntervalPreference.Rule> rules = SettingsHelper.getInstance(mContext).getReconnectIntervalRules();
        if (rules.size() == 0)
            return -1;
        int att = 0;
        for (ReconnectIntervalPreference.Rule rule : rules) {
            att += rule.repeatCount;
            if (attemptNumber < att)
                return rule.reconnectDelay;
        }
        return rules.get(rules.size() - 1).reconnectDelay;
    }

    public void addListener(ConnectionsListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(ConnectionsListener listener) {
        mListeners.remove(listener);
    }

    public void addGlobalConnectionInfoListener(ServerConnectionInfo.InfoChangeListener listener) {
        mInfoListeners.add(listener);
    }

    public void removeGlobalConnectionInfoListener(ServerConnectionInfo.InfoChangeListener listener) {
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

    public void notifyConnectivityChanged() {
        SettingsHelper helper = SettingsHelper.getInstance(mContext);
        if (helper.isReconnectEnabled() && helper.shouldReconnectOnConnectivityChange()) {
            for (ServerConnectionInfo server : mConnectionsMap.values())
                server.connect(); // this will be ignored if we are already corrected
        }
    }

    public interface ConnectionsListener {

        void onConnectionAdded(ServerConnectionInfo connection);

        void onConnectionRemoved(ServerConnectionInfo connection);

    }

}
