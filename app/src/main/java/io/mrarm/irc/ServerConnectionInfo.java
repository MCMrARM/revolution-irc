package io.mrarm.irc;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.ChannelListListener;
import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.IRCConnectionRequest;

// TODO: this runs stuff on another thread but does not synchronize contents
public class ServerConnectionInfo {

    private static Handler mReconnectHandler = new Handler();

    private ServerConnectionManager mManager;
    private UUID mUUID;
    private String mName;
    private List<String> mChannels;
    private ChatApi mApi;
    private IRCConnectionRequest mConnectionRequest;
    private List<String> mAutojoinChannels;
    private boolean mExpandedInDrawer = true;
    private boolean mConnected = false;
    private boolean mConnecting = false;
    private NotificationManager mNotificationManager;
    private List<InfoChangeListener> mInfoListeners = new ArrayList<>();
    private List<ChannelListChangeListener> mChannelsListeners = new ArrayList<>();
    private int mCurrentReconnectAttempt = -1;

    public ServerConnectionInfo(ServerConnectionManager manager, UUID uuid, String name,
                                IRCConnectionRequest connectionRequest, List<String> autojoinChannels) {
        mManager = manager;
        mUUID = uuid;
        mName = name;
        mConnectionRequest = connectionRequest;
        mAutojoinChannels = autojoinChannels;
        mNotificationManager = new NotificationManager(this);
    }

    public ServerConnectionInfo(ServerConnectionManager manager, UUID uuid, String name, ChatApi api) {
        mManager = manager;
        mUUID = uuid;
        mName = name;
        mNotificationManager = new NotificationManager(this);
        setApi(api);
    }

    void setApi(ChatApi api) {
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

    public ServerConnectionManager getConnectionManager() {
        return mManager;
    }

    public void connect() {
        if (mConnected || mConnecting)
            return;
        mConnecting = true;
        Log.i("ServerConnectionInfo", "Connecting...");

        IRCConnection connection = null;
        boolean createdNewConnection = false;
        if (mApi == null || !(mApi instanceof IRCConnection)) {
            connection = new IRCConnection();
            connection.addDisconnectListener((IRCConnection conn, Exception reason) -> {
                notifyDisconnected();
            });
            createdNewConnection = true;
        } else {
            connection = (IRCConnection) mApi;
        }

        IRCConnection fConnection = connection;

        connection.connect(mConnectionRequest, (Void v) -> {
            mConnecting = false;
            setConnected(true);
            mCurrentReconnectAttempt = 0;
            fConnection.joinChannels(mAutojoinChannels, null, null);
        }, (Exception e) -> {
            notifyDisconnected();
        });

        if (createdNewConnection) {
            setApi(connection);
        }
    }

    private void notifyDisconnected() {
        mConnecting = false;
        setConnected(false);
        int reconnectDelay = mManager.getReconnectDelay(mCurrentReconnectAttempt++);
        if (reconnectDelay == -1)
            return;
        Log.i("ServerConnectionInfo", "Queuing reconnect in " + reconnectDelay + " ms");
        mReconnectHandler.postDelayed(this::connect, reconnectDelay);
    }

    public UUID getUUID() {
        return mUUID;
    }

    public String getName() {
        return mName;
    }

    public ChatApi getApiInstance() {
        return mApi;
    }

    public boolean isConnected() { return mConnected; }

    public void setConnected(boolean connected) {
        mConnected = connected;
        notifyInfoChanged();
    }

    public List<String> getChannels() {
        return mChannels;
    }

    public void setChannels(List<String> channels) {
        mChannels = channels;
        for (ChannelListChangeListener listener : mChannelsListeners)
            listener.onChannelListChanged(this, channels);
        mManager.notifyChannelListChanged(this, channels);
    }

    public boolean isExpandedInDrawer() {
        return mExpandedInDrawer;
    }

    public void setExpandedInDrawer(boolean expanded) {
        mExpandedInDrawer = expanded;
    }

    public NotificationManager getNotificationManager() {
        return mNotificationManager;
    }

    public void addOnChannelInfoChangeListener(InfoChangeListener listener) {
        mInfoListeners.add(listener);
    }

    public void removeOnChannelInfoChangeListener(InfoChangeListener listener) {
        mInfoListeners.remove(listener);
    }

    public void addOnChannelListChangeListener(ChannelListChangeListener listener) {
        mChannelsListeners.add(listener);
    }

    public void removeOnChannelListChangeListener(ChannelListChangeListener listener) {
        mChannelsListeners.remove(listener);
    }

    private void notifyInfoChanged() {
        for (InfoChangeListener listener : mInfoListeners)
            listener.onConnectionInfoChanged(this);
        mManager.notifyConnectionInfoChanged(this);
    }

    public interface InfoChangeListener {
        void onConnectionInfoChanged(ServerConnectionInfo connection);
    }

    public interface ChannelListChangeListener {
        void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels);
    }

}
