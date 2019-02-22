package io.mrarm.irc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import io.mrarm.chatlib.irc.IRCConnectionRequest;
import io.mrarm.chatlib.irc.cap.SASLOptions;
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.setting.ReconnectIntervalSetting;

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
    private boolean mDestroying = false;

    public static boolean hasInstance() {
        return instance != null;
    }

    public static synchronized ServerConnectionManager getInstance(Context context) {
        if (instance == null && context != null)
            instance = new ServerConnectionManager(context.getApplicationContext());
        return instance;
    }

    public static synchronized void destroyInstance() {
        if (instance == null)
            return;
        instance.mDestroying = true;
        while (instance.mConnections.size() > 0) {
            ServerConnectionInfo connection = instance.mConnections.get(instance.mConnections.size() - 1);
            connection.disconnect();
            instance.removeConnection(connection, false);
            instance.killDisconnectingConnection(connection.getUUID());
        }
        instance = null;
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
                if (configData != null) {
                    try {
                        createConnection(configData, server.channels, false);
                    } catch (NickNotSetException ignored) {
                    }
                }
            }
        }
    }

    private void saveAutoconnectList() {
        ConnectedServersList list = new ConnectedServersList();
        list.servers = new ArrayList<>();
        for (ServerConnectionInfo connectionInfo : getConnections()) {
            ConnectedServerInfo server = new ConnectedServerInfo();
            server.uuid = connectionInfo.getUUID();
            server.channels = connectionInfo.getChannels();
            list.servers.add(server);
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

        IRCConnectionRequest request = new IRCConnectionRequest();
        request
                .setServerAddress(data.address, data.port);
        if (data.charset != null)
            request.setCharset(Charset.forName(data.charset));
        if (data.nicks != null && data.nicks.size() > 0) {
            for (String nick : data.nicks)
                request.addNick(nick);
        } else {
            for (String nick : AppSettings.getDefaultNicks())
                request.addNick(nick);
            if (request.getNickList() == null)
                throw new NickNotSetException();
        }
        if (data.user != null)
            request.setUser(data.user);
        else if (AppSettings.getDefaultUser() != null && AppSettings.getDefaultUser().length() > 0)
            request.setUser(AppSettings.getDefaultUser());
        else
            request.setUser(request.getNickList().get(0));
        if (data.realname != null)
            request.setRealName(data.realname);
        else if (AppSettings.getDefaultRealname() != null && AppSettings.getDefaultRealname().length() > 0)
            request.setRealName(AppSettings.getDefaultRealname());
        else
            request.setRealName(request.getNickList().get(0));

        if (data.pass != null)
            request.setServerPass(data.pass);

        SASLOptions saslOptions = null;
        UserKeyManager userKeyManager = null;
        if (data.authMode != null) {
            if (data.authMode.equals(ServerConfigData.AUTH_SASL) && data.authUser != null &&
                    data.authPass != null)
                saslOptions = SASLOptions.createPlainAuth(data.authUser, data.authPass);
            if (data.authMode.equals(ServerConfigData.AUTH_SASL_EXTERNAL)) {
                saslOptions = SASLOptions.createExternal();
                userKeyManager = new UserKeyManager(data.getAuthCert(), data.getAuthPrivateKey());
            }
        }

        if (data.ssl) {
            UserOverrideTrustManager sslHelper = new UserOverrideTrustManager(mContext, data.uuid);
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                KeyManager[] keyManagers = new KeyManager[0];
                if (userKeyManager != null)
                    keyManagers = new KeyManager[] { userKeyManager };
                sslContext.init(keyManagers, new TrustManager[] { sslHelper }, null);
                request.enableSSL(sslContext.getSocketFactory(), sslHelper);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
        ServerConnectionInfo connectionInfo = new ServerConnectionInfo(this, data, request, saslOptions, joinChannels);
        connectionInfo.connect();
        addConnection(connectionInfo, saveAutoconnect);
        return connectionInfo;
    }

    public ServerConnectionInfo createConnection(ServerConfigData data) {
        return createConnection(data, null, true);
    }

    public void tryCreateConnection(ServerConfigData data, Context activity) {
        if (ServerConnectionManager.getInstance(getContext()).hasConnection(data.uuid))
            return;
        try {
            createConnection(data);
        } catch (NickNotSetException e) {
            Toast.makeText(activity, R.string.connection_error_no_nick, Toast.LENGTH_SHORT).show();
        }
    }

    public void removeConnection(ServerConnectionInfo connection, boolean saveAutoconnect) {
        NotificationManager.getInstance().clearAllNotifications(mContext, connection);
        synchronized (this) {
            synchronized (connection) {
                if (connection.isDisconnecting()) {
                    synchronized (mDisconnectingConnections) {
                        if (mDisconnectingConnections.containsKey(connection.getUUID()))
                            throw new RuntimeException("mDisconnectingConnections already contains a disconnecting connection with this UUID");
                        mDisconnectingConnections.put(connection.getUUID(), connection);
                    }
                } else if (connection.isConnecting() || connection.isConnected() || !connection.hasUserDisconnectRequest()) {
                    throw new RuntimeException("Trying to remove a non-disconnected connection");
                } else {
                    connection.close();
                }
            }
            mConnections.remove(connection);
            mConnectionsMap.remove(connection.getUUID());
            if (saveAutoconnect)
                saveAutoconnectListAsync();
            if (mConnections.size() == 0)
                IRCService.stop(mContext);
            else if (!mDestroying)
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
            connection.close();
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
        if (!AppSettings.isReconnectEnabled())
            return -1;
        List<ReconnectIntervalSetting.Rule> rules = AppSettings.getReconnectIntervalRules();
        if (rules.size() == 0)
            return -1;
        int att = 0;
        for (ReconnectIntervalSetting.Rule rule : rules) {
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
        if (!hasConnection(connection.getUUID()))
            return;
        synchronized (mInfoListeners) {
            for (ServerConnectionInfo.InfoChangeListener listener : mInfoListeners)
                listener.onConnectionInfoChanged(connection);
            if (!mDestroying)
                IRCService.start(mContext);
        }
    }

    void notifyChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        if (!hasConnection(connection.getUUID()))
            return;
        synchronized (mChannelsListeners) {
            for (ServerConnectionInfo.ChannelListChangeListener listener : mChannelsListeners)
                listener.onChannelListChanged(connection, newChannels);
        }
    }

    void notifyConnectionFullyDisconnected(ServerConnectionInfo connection) {
        ServerConnectionInfo removed;
        synchronized (mDisconnectingConnections) {
            removed = mDisconnectingConnections.remove(connection.getUUID());
        }
        if (removed != null)
            connection.close();
    }

    public void notifyConnectivityChanged(boolean hasAnyConnectivity) {
        boolean hasWifi = isWifiConnected(mContext);
        synchronized (this) {
            for (ServerConnectionInfo server : mConnectionsMap.values())
                server.notifyConnectivityChanged(hasAnyConnectivity, hasWifi);
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

    public class NickNotSetException extends RuntimeException {
    }

}
