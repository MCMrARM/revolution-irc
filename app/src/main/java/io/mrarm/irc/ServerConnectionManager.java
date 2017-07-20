package io.mrarm.irc;

import android.content.Context;
import android.net.ConnectivityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.irc.IRCConnectionRequest;
import io.mrarm.chatlib.irc.cap.SASLOptions;
import io.mrarm.irc.preference.ReconnectIntervalPreference;
import io.mrarm.irc.util.SettingsHelper;

public class ServerConnectionManager {

    private static ServerConnectionManager instance;

    private final Context mContext;
    private final HashMap<UUID, ServerConnectionInfo> mConnectionsMap = new HashMap<>();
    private final ArrayList<ServerConnectionInfo> mConnections = new ArrayList<>();
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
        synchronized (this) {
            uuids.addAll(mConnectionsMap.keySet());
        }
        SettingsHelper settings = SettingsHelper.getInstance(mContext);
        settings.setAutoConnectServerList(uuids);
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
            mConnectionsMap.put(connection.getUUID(), connection);
            mConnections.add(connection);
            if (saveAutoconnect)
                saveAutoconnectList();
        }
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
        else if (settings.getDefaultUser() != null && settings.getDefaultUser().length() > 0)
            request.setUser(settings.getDefaultUser());
        else
            request.setUser(settings.getDefaultNick());
        if (data.realname != null)
            request.setRealName(data.realname);
        else if (settings.getDefaultRealname() != null && settings.getDefaultRealname().length() > 0)
            request.setRealName(settings.getDefaultRealname());
        else
            request.setRealName(settings.getDefaultNick());

        SASLOptions saslOptions = null;
        if (data.authMode != null) {
            if (data.authMode.equals(ServerConfigData.AUTH_PASSWORD) && data.authPass != null)
                request.setServerPass(data.authPass);
            if (data.authMode.equals(ServerConfigData.AUTH_SASL) && data.authUser != null &&
                    data.authPass != null)
                saslOptions = SASLOptions.createPlainAuth(data.authUser, data.authPass);
        }

        if (data.ssl) {
            ServerSSLHelper sslHelper = new ServerSSLHelper(ServerConfigManager.getInstance(mContext).getServerSSLCertsFile(data.uuid));
            request.enableSSL(sslHelper.createSocketFactory(), sslHelper.createHostnameVerifier());
        }
        ServerConnectionInfo connectionInfo = new ServerConnectionInfo(this, data.uuid, data.name, request, saslOptions, data.autojoinChannels);
        connectionInfo.connect();
        addConnection(connectionInfo, saveAutoconnect);
        return connectionInfo;
    }

    public ServerConnectionInfo createConnection(ServerConfigData data) {
        return createConnection(data, true);
    }

    public void removeConnection(ServerConnectionInfo connection, boolean saveAutoconnect) {
        synchronized (this) {
            mConnections.remove(connection);
            mConnectionsMap.remove(connection.getUUID());
            if (saveAutoconnect)
                saveAutoconnectList();
        }
        for (ConnectionsListener listener : mListeners)
            listener.onConnectionRemoved(connection);
    }

    public void removeConnection(ServerConnectionInfo connection) {
        removeConnection(connection, true);
    }

    public void removeAllConnections() {
        synchronized (this) {
            while (mConnections.size() > 0)
                removeConnection(mConnections.get(mConnections.size() - 1), false);
            saveAutoconnectList();
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

}
