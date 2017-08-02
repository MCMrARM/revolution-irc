package io.mrarm.irc;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.ChannelListListener;
import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.android.storage.SQLiteMessageStorageApi;
import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.IRCConnectionRequest;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.irc.cap.SASLCapability;
import io.mrarm.chatlib.irc.cap.SASLOptions;
import io.mrarm.chatlib.irc.filters.ZNCPlaybackMessageFilter;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.util.IgnoreListMessageFilter;
import io.mrarm.irc.config.SettingsHelper;

public class ServerConnectionInfo {

    private static Handler mReconnectHandler = new Handler();

    private ServerConnectionManager mManager;
    private ServerConfigData mServerConfig;
    private List<String> mChannels;
    private ChatApi mApi;
    private IRCConnectionRequest mConnectionRequest;
    private SASLOptions mSASLOptions;
    private boolean mExpandedInDrawer = true;
    private boolean mConnected = false;
    private boolean mConnecting = false;
    private boolean mUserDisconnectRequest = false;
    private NotificationManager.ConnectionManager mNotificationData;
    private final List<InfoChangeListener> mInfoListeners = new ArrayList<>();
    private final List<ChannelListChangeListener> mChannelsListeners = new ArrayList<>();
    private int mCurrentReconnectAttempt = -1;
    int mChatLogStorageUpdateCounter = 0;

    public ServerConnectionInfo(ServerConnectionManager manager, ServerConfigData config,
                                IRCConnectionRequest connectionRequest, SASLOptions saslOptions,
                                List<String> joinChannels) {
        mManager = manager;
        mServerConfig = config;
        mConnectionRequest = connectionRequest;
        mSASLOptions = saslOptions;
        mNotificationData = new NotificationManager.ConnectionManager(this);
        mChannels = joinChannels;
    }

    private void setApi(ChatApi api) {
        synchronized (this) {
            mApi = api;
            api.getJoinedChannelList((List<String> channels) -> {
                setChannels(channels);
            }, null);
            api.subscribeChannelList(new ChannelListListener() {
                @Override
                public void onChannelListChanged(List<String> list) {
                    setChannels(list);
                }

                @Override
                public void onChannelJoined(String s) {
                }

                @Override
                public void onChannelLeft(String s) {
                }
            }, null, null);
        }
    }

    public ServerConnectionManager getConnectionManager() {
        return mManager;
    }

    public void connect() {
        synchronized (this) {
            if (mConnected || mConnecting)
                return;
            mConnecting = true;
        }
        Log.i("ServerConnectionInfo", "Connecting...");

        IRCConnection connection = null;
        boolean createdNewConnection = false;
        if (mApi == null || !(mApi instanceof IRCConnection)) {
            connection = new IRCConnection();
            ServerConfigManager configManager = ServerConfigManager.getInstance(mManager.getContext());
            connection.getServerConnectionData().setMessageStorageApi(new SQLiteMessageStorageApi(configManager.getServerChatLogDir(getUUID())));
            connection.getServerConnectionData().getMessageFilterList().addMessageFilter(new IgnoreListMessageFilter(mServerConfig));
            if (mSASLOptions != null)
                connection.getServerConnectionData().getCapabilityManager().registerCapability(
                        new SASLCapability(mSASLOptions));
            connection.getServerConnectionData().getMessageFilterList().addMessageFilter(
                    new ZNCPlaybackMessageFilter(connection.getServerConnectionData()));
            connection.addDisconnectListener((IRCConnection conn, Exception reason) -> {
                notifyDisconnected();
            });
            createdNewConnection = true;
        } else {
            connection = (IRCConnection) mApi;
        }

        IRCConnection fConnection = connection;

        List<String> rejoinChannels = getChannels();

        connection.connect(mConnectionRequest, (Void v) -> {
            synchronized (this) {
                mConnecting = false;
                setConnected(true);
                mCurrentReconnectAttempt = 0;
            }

            List<String> joinChannels = new ArrayList<>();
            if (mServerConfig.autojoinChannels != null)
                joinChannels.addAll(mServerConfig.autojoinChannels);
            if (rejoinChannels != null && mServerConfig.rejoinChannels)
                joinChannels.addAll(rejoinChannels);
            if (joinChannels.size() > 0)
                fConnection.joinChannels(joinChannels, null, null);

        }, (Exception e) -> {
            if (e instanceof UserOverrideTrustManager.UserRejectedCertificateException ||
                    (e.getCause() != null && e.getCause() instanceof
                            UserOverrideTrustManager.UserRejectedCertificateException)) {
                Log.d("ServerConnectionInfo", "User rejected the certificate");
                synchronized (this) {
                    mUserDisconnectRequest = true;
                }
            }
            notifyDisconnected();
        });

        if (createdNewConnection) {
            setApi(connection);
        }
    }

    public void disconnect() {
        synchronized (this) {
            mUserDisconnectRequest = true;
            String message = SettingsHelper.getInstance(mManager.getContext()).getDefaultQuitMessage();
            mApi.quit(message, null, null);
        }
    }

    private void notifyDisconnected() {
        synchronized (this) {
            setConnected(false);
            mConnecting = false;
            if (mUserDisconnectRequest)
                return;
        }
        int reconnectDelay = mManager.getReconnectDelay(mCurrentReconnectAttempt++);
        if (reconnectDelay == -1)
            return;
        Log.i("ServerConnectionInfo", "Queuing reconnect in " + reconnectDelay + " ms");
        mReconnectHandler.postDelayed(() -> {
            SettingsHelper helper = SettingsHelper.getInstance(mManager.getContext());
            if (!helper.isReconnectEnabled() || !helper.shouldReconnectOnConnectivityChange() ||
                    (helper.isReconnectWifiRequired() && !ServerConnectionManager.isWifiConnected(
                            mManager.getContext())))
                return;
            this.connect();
        }, reconnectDelay);
    }

    public UUID getUUID() {
        return mServerConfig.uuid;
    }

    public String getName() {
        return mServerConfig.name;
    }

    public ChatApi getApiInstance() {
        synchronized (this) {
            return mApi;
        }
    }

    public boolean isConnected() {
        synchronized (this) {
            return mConnected;
        }
    }

    public void setConnected(boolean connected) {
        synchronized (this) {
            mConnected = connected;
        }
        notifyInfoChanged();
    }

    public boolean isConnecting() {
        synchronized (this) {
            return mConnecting;
        }
    }

    public List<String> getChannels() {
        synchronized (this) {
            return mChannels;
        }
    }

    public void setChannels(List<String> channels) {
        synchronized (this) {
            mChannels = channels;
            for (ChannelListChangeListener listener : mChannelsListeners)
                listener.onChannelListChanged(this, channels);
            mManager.notifyChannelListChanged(this, channels);
            mManager.saveAutoconnectListAsync();
        }
    }

    public boolean isExpandedInDrawer() {
        synchronized (this) {
            return mExpandedInDrawer;
        }
    }

    public void setExpandedInDrawer(boolean expanded) {
        synchronized (this) {
            mExpandedInDrawer = expanded;
        }
    }

    public NotificationManager.ConnectionManager getNotificationManager() {
        return mNotificationData;
    }

    public String getUserNick() {
        return ((ServerConnectionApi) getApiInstance()).getServerConnectionData().getUserNick();
    }

    public void addOnChannelInfoChangeListener(InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.add(listener);
        }
    }

    public void removeOnChannelInfoChangeListener(InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.remove(listener);
        }
    }

    public void addOnChannelListChangeListener(ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.add(listener);
        }
    }

    public void removeOnChannelListChangeListener(ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.remove(listener);
        }
    }

    private void notifyInfoChanged() {
        synchronized (mInfoListeners) {
            for (InfoChangeListener listener : mInfoListeners)
                listener.onConnectionInfoChanged(this);
            mManager.notifyConnectionInfoChanged(this);
        }
    }

    public interface InfoChangeListener {
        void onConnectionInfoChanged(ServerConnectionInfo connection);
    }

    public interface ChannelListChangeListener {
        void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels);
    }

}
