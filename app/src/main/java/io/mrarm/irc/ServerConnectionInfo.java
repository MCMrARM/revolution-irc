package io.mrarm.irc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.ChannelListListener;
import io.mrarm.chatlib.ChatApi;

public class ServerConnectionInfo {

    private ServerConnectionManager mManager;
    private UUID mUUID;
    private String mName;
    private List<String> mChannels;
    private ChatApi mApi;
    private boolean mExpandedInDrawer = true;
    private boolean mConnected = false;
    private NotificationManager mNotificationManager;
    private List<InfoChangeListener> mInfoListeners = new ArrayList<>();
    private List<ChannelListChangeListener> mChannelsListeners = new ArrayList<>();

    public ServerConnectionInfo(ServerConnectionManager manager, UUID uuid, String name, ChatApi api) {
        mManager = manager;
        mUUID = uuid;
        mName = name;
        mApi = api;
        mNotificationManager = new NotificationManager(this);

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
                //
            }

            @Override
            public void onChannelLeft(String s) {
                //
            }
        }, null, null);
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
