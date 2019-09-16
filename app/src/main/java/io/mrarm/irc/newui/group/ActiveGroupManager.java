package io.mrarm.irc.newui.group;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.util.UiThreadHelper;

@Singleton
public class ActiveGroupManager implements ServerConnectionManager.ConnectionsListener,
        ServerConnectionInfo.DetailedChannelListListener {

    private final GroupManager mGroupManager;
    private final UiThreadWrapper mUiThreadWrapper = new UiThreadWrapper();

    @Inject
    public ActiveGroupManager(GroupManager groupManager, ServerConnectionManager connectionManager) {
        mGroupManager = groupManager;
        connectionManager.addListener(this);
        for (ServerConnectionInfo server : connectionManager.getConnections())
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
        if (serverData != null) {
            serverData.onChannelJoined(channel);
        }
    }

    @Override
    public void onChannelLeft(ServerConnectionInfo connection, String channel) {
        ServerGroupData serverData = mGroupManager.getServerData(connection.getUUID());
        if (serverData != null) {
            serverData.onChannelLeft(channel);
        }
    }

    @Override
    public void onChannelListReset(ServerConnectionInfo connection, List<String> channels) {
        ServerGroupData serverData = mGroupManager.getServerData(connection.getUUID());
        if (serverData != null) {
            serverData.onChannelListReset();
        }

        for (String channel : channels)
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
