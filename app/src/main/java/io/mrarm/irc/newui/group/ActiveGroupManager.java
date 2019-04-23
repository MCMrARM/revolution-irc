package io.mrarm.irc.newui.group;

import android.content.Context;

import java.util.Collections;
import java.util.List;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.util.UiThreadHelper;

public class ActiveGroupManager implements ServerConnectionManager.ConnectionsListener,
        ServerConnectionInfo.DetailedChannelListListener {

    private static ActiveGroupManager sInstance;

    public static ActiveGroupManager getInstance(Context ctx) {
        if (sInstance == null)
            sInstance = new ActiveGroupManager(GroupManager.getInstance(ctx));
        return sInstance;
    }

    private final GroupManager mGroupManager;
    private final UiThreadWrapper mUiThreadWrapper = new UiThreadWrapper();

    public ActiveGroupManager(GroupManager groupManager) {
        mGroupManager = groupManager;
        ServerConnectionManager mgr = ServerConnectionManager.getInstance(groupManager.getContext());
        mgr.addListener(this);
        for (ServerConnectionInfo server : mgr.getConnections())
            onConnectionAdded(server);
    }


    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        connection.addChannelListListener(mUiThreadWrapper);
        List<String> channels = connection.getChannels();
        UiThreadHelper.runOnUiThread(() -> onChannelListReset(connection, channels));
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        connection.removeChannelListListener(mUiThreadWrapper);
        UiThreadHelper.runOnUiThread(() -> onChannelListReset(connection, Collections.emptyList()));
    }


    @Override
    public void onChannelJoined(ServerConnectionInfo connection, String channel) {
        ServerGroupData serverData = mGroupManager.getServerData(connection.getUUID());
        if (serverData != null && serverData.mDefaultSubGroup != null) {
            serverData.mDefaultSubGroup.mCurrentChannels.add(channel);
        }
    }

    @Override
    public void onChannelLeft(ServerConnectionInfo connection, String channel) {
        ServerGroupData serverData = mGroupManager.getServerData(connection.getUUID());
        if (serverData != null && serverData.mDefaultSubGroup != null) {
            serverData.mDefaultSubGroup.mCurrentChannels.remove(channel);
        }
    }

    @Override
    public void onChannelListReset(ServerConnectionInfo connection, List<String> channels) {
        ServerGroupData serverData = mGroupManager.getServerData(connection.getUUID());
        if (serverData != null && serverData.mDefaultSubGroup != null) {
            serverData.mDefaultSubGroup.mCurrentChannels.clear();
        }

        for (String channel : connection.getChannels())
            onChannelJoined(connection, channel);
    }

    private class UiThreadWrapper implements ServerConnectionInfo.DetailedChannelListListener {

        @Override
        public void onChannelJoined(ServerConnectionInfo connection, String channel) {
            UiThreadHelper.runOnUiThread(() ->
                    ActiveGroupManager.this.onChannelJoined(connection, channel));
        }

        @Override
        public void onChannelLeft(ServerConnectionInfo connection, String channel) {
            UiThreadHelper.runOnUiThread(() ->
                    ActiveGroupManager.this.onChannelLeft(connection, channel));
        }

        @Override
        public void onChannelListReset(ServerConnectionInfo connection, List<String> channels) {
            UiThreadHelper.runOnUiThread(() ->
                    ActiveGroupManager.this.onChannelListReset(connection, channels));
        }

    }

}
