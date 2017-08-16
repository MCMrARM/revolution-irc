package io.mrarm.irc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.android.storage.SQLiteMessageStorageApi;
import io.mrarm.chatlib.irc.IRCConnectionRequest;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.irc.cap.SASLOptions;
import io.mrarm.chatlib.message.MessageStorageApi;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.ServerCertificateManager;
import io.mrarm.irc.preference.ReconnectIntervalPreference;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.StubMessageStorageApi;

public class ServerConnectionManager {

    public static final String CONNECTED_SERVERS_FILE_PATH = "connected_servers.json";

    private static ServerConnectionManager instance;

    private final Context mContext;
    private final File mConnectedServersFile;
    private final HashMap<UUID, ServerConnectionInfo> mConnectionsMap = new HashMap<>();
    private final ArrayList<ServerConnectionInfo> mConnections = new ArrayList<>();
    private final HashMap<UUID, ServerConnectionInfo> mDisconnectingConnections = new HashMap<>();
    private final List<ConnectionsListener> mListeners = new ArrayList<>();
    private final List<ServerConnectionInfo.ChannelListChangeListener> mChannelsListeners = new ArrayList<>();
    private final List<ServerConnectionInfo.InfoChangeListener> mInfoListeners = new ArrayList<>();

    public static ServerConnectionManager getInstance(Context context) {
        if (instance == null && context != null)
            instance = new ServerConnectionManager(context.getApplicationContext());
        return instance;
    }

    public ServerConnectionManager(Context context) {
        mContext = context;

        mConnectedServersFile = new File(context.getFilesDir(), CONNECTED_SERVERS_FILE_PATH);
        ConnectedServersList servers = null;
        try {
            servers = SettingsHelper.getGson().fromJson(new BufferedReader(new FileReader(mConnectedServersFile)), ConnectedServersList.class);
        } catch (Exception ignored) {
        }

        if (servers != null) {
            ServerConfigManager configManager = ServerConfigManager.getInstance(context);
            for (ConnectedServerInfo server : servers.servers) {
                ServerConfigData configData = configManager.findServer(server.uuid);
                if (configData != null)
                    createConnection(configData, server.channels, false);
            }
        }
    }

    private void saveAutoconnectList() {
        ConnectedServersList list = new ConnectedServersList();
        list.servers = new ArrayList<>();
        synchronized (this) {
            for (ServerConnectionInfo connectionInfo : mConnections) {
                ConnectedServerInfo server = new ConnectedServerInfo();
                server.uuid = connectionInfo.getUUID();
                server.channels = connectionInfo.getChannels();
                list.servers.add(server);
            }
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(mConnectedServersFile));
            SettingsHelper.getGson().toJson(list, writer);
            writer.close();
        } catch (Exception ignored) {
        }
    }

    void saveAutoconnectListAsync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                saveAutoconnectList();
                return null;
            }
        };
        task.execute();
    }

    public Context getContext() {
        return mContext;
    }

    public List<ServerConnectionInfo> getConnections() {
        synchronized (this) {
            return new ArrayList<>(mConnections);
        }
    }

    public void addConnection(ServerConnectionInfo connection, boolean saveAutoconnect) {
        synchronized (this) {
            if (mConnectionsMap.containsKey(connection.getUUID()))
                throw new RuntimeException("A connection with this UUID already exists");
            mConnectionsMap.put(connection.getUUID(), connection);
            mConnections.add(connection);
            if (saveAutoconnect)
                saveAutoconnectListAsync();
        }
        synchronized (mListeners) {
            for (ConnectionsListener listener : mListeners)
                listener.onConnectionAdded(connection);
        }
        IRCService.start(mContext);
    }

    public void addConnection(ServerConnectionInfo connection) {
        addConnection(connection, true);
    }

    private ServerConnectionInfo createConnection(ServerConfigData data, List<String> joinChannels, boolean saveAutoconnect) {
        killDisconnectingConnection(data.uuid);

        SettingsHelper settings = SettingsHelper.getInstance(mContext);

        IRCConnectionRequest request = new IRCConnectionRequest();
        request
                .setServerAddress(data.address, data.port);
        if (data.nicks != null && data.nicks.size() > 0) {
            for (String nick : data.nicks)
                request.addNick(nick);
        } else {
            for (String nick : settings.getDefaultNicks())
                request.addNick(nick);
        }
        if (data.user != null)
            request.setUser(data.user);
        else if (settings.getDefaultUser() != null && settings.getDefaultUser().length() > 0)
            request.setUser(settings.getDefaultUser());
        else
            request.setUser(settings.getDefaultPrimaryNick());
        if (data.realname != null)
            request.setRealName(data.realname);
        else if (settings.getDefaultRealname() != null && settings.getDefaultRealname().length() > 0)
            request.setRealName(settings.getDefaultRealname());
        else
            request.setRealName(settings.getDefaultPrimaryNick());

        SASLOptions saslOptions = null;
        if (data.authMode != null) {
            if (data.authMode.equals(ServerConfigData.AUTH_PASSWORD) && data.authPass != null)
                request.setServerPass(data.authPass);
            if (data.authMode.equals(ServerConfigData.AUTH_SASL) && data.authUser != null &&
                    data.authPass != null)
                saslOptions = SASLOptions.createPlainAuth(data.authUser, data.authPass);
        }

        if (data.ssl) {
            UserOverrideTrustManager sslHelper = new UserOverrideTrustManager(mContext, data.uuid);
            request.enableSSL(sslHelper.createSocketFactory(), sslHelper);
        }
        ServerConnectionInfo connectionInfo = new ServerConnectionInfo(this, data, request, saslOptions, joinChannels);
        connectionInfo.connect();
        addConnection(connectionInfo, saveAutoconnect);
        return connectionInfo;
    }

    public ServerConnectionInfo createConnection(ServerConfigData data) {
        return createConnection(data, null, true);
    }

    public void removeConnection(ServerConnectionInfo connection, boolean saveAutoconnect) {
        NotificationManager.getInstance().clearAllNotifications(mContext, connection);
        synchronized (this) {
            if (connection.isDisconnecting()) {
                synchronized (mDisconnectingConnections) {
                    if (mDisconnectingConnections.containsKey(connection.getUUID()))
                        throw new RuntimeException("mDisconnectingConnections already contains a disconnecting connection with this UUID");
                    mDisconnectingConnections.put(connection.getUUID(), connection);
                }
            } else if (connection.isConnecting() || connection.isConnected()) {
                throw new RuntimeException("Trying to remove a non-disconnected connection");
            }
            mConnections.remove(connection);
            mConnectionsMap.remove(connection.getUUID());
            if (saveAutoconnect)
                saveAutoconnectListAsync();
            if (mConnections.size() == 0)
                IRCService.stop(mContext);
            else
                IRCService.start(mContext); // update connection count
        }
        synchronized (mListeners) {
            for (ConnectionsListener listener : mListeners)
                listener.onConnectionRemoved(connection);
        }
    }

    public void removeConnection(ServerConnectionInfo connection) {
        removeConnection(connection, true);
    }

    /**
     * Stop keeping track of a disconnected connection. A call to this function is required if you
     * want to do something with this server's logs to make sure they are properly released.
     */
    public void killDisconnectingConnection(UUID uuid) {
        synchronized (mDisconnectingConnections) {
            ServerConnectionInfo connection = mDisconnectingConnections.get(uuid);
            if (connection == null)
                return;
            MessageStorageApi storageApi = ((ServerConnectionApi) connection.getApiInstance()).getServerConnectionData().getMessageStorageApi();
            if (storageApi instanceof SQLiteMessageStorageApi)
                ((SQLiteMessageStorageApi) storageApi).close();
            ((ServerConnectionApi) connection.getApiInstance()).getServerConnectionData().setMessageStorageApi(new StubMessageStorageApi());
            mDisconnectingConnections.remove(uuid);
        }
    }

    public void disconnectAndRemoveAllConnections(boolean kill) {
        synchronized (this) {
            while (mConnections.size() > 0) {
                ServerConnectionInfo connection = mConnections.get(mConnections.size() - 1);
                connection.disconnect();
                removeConnection(connection, false);
                if (kill)
                    killDisconnectingConnection(connection.getUUID());
            }
            saveAutoconnectListAsync();
        }
    }

    public ServerConnectionInfo getConnection(UUID uuid) {
        synchronized (this) {
            return mConnectionsMap.get(uuid);
        }
    }

    public boolean hasConnection(UUID uuid) {
        synchronized (this) {
            return mConnectionsMap.containsKey(uuid);
        }
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
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    public void removeListener(ConnectionsListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    public void addGlobalConnectionInfoListener(ServerConnectionInfo.InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.add(listener);
        }
    }

    public void removeGlobalConnectionInfoListener(ServerConnectionInfo.InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.remove(listener);
        }
    }

    public void addGlobalChannelListListener(ServerConnectionInfo.ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.add(listener);
        }
    }

    public void removeGlobalChannelListListener(ServerConnectionInfo.ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.remove(listener);
        }
    }

    void notifyConnectionInfoChanged(ServerConnectionInfo connection) {
        synchronized (mInfoListeners) {
            for (ServerConnectionInfo.InfoChangeListener listener : mInfoListeners)
                listener.onConnectionInfoChanged(connection);
        }
    }

    void notifyChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        synchronized (mChannelsListeners) {
            for (ServerConnectionInfo.ChannelListChangeListener listener : mChannelsListeners)
                listener.onChannelListChanged(connection, newChannels);
        }
    }

    void notifyConnectionFullyDisconnected(ServerConnectionInfo connection) {
        synchronized (mDisconnectingConnections) {
            mDisconnectingConnections.remove(connection.getUUID());
        }
    }

    public void notifyConnectivityChanged() {
        SettingsHelper helper = SettingsHelper.getInstance(mContext);
        if (helper.isReconnectEnabled() && helper.shouldReconnectOnConnectivityChange()) {
            if (helper.isReconnectWifiRequired() && !isWifiConnected(mContext))
                return;
            synchronized (this) {
                for (ServerConnectionInfo server : mConnectionsMap.values())
                    server.connect(); // this will be ignored if we are already corrected
            }
        }
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
    }

    public interface ConnectionsListener {

        void onConnectionAdded(ServerConnectionInfo connection);

        void onConnectionRemoved(ServerConnectionInfo connection);

    }

    private static class ConnectedServerInfo {

        public UUID uuid;
        public List<String> channels;

    }

    private static class ConnectedServersList {

        public List<ConnectedServerInfo> servers;

    }

}
