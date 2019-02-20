package io.mrarm.irc.newui;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.mrarm.chatlib.ChannelInfoListener;
import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.util.ListUtils;
import io.mrarm.irc.util.UiThreadHelper;

public class ServerListChannelData implements ServerConnectionManager.ConnectionsListener {

    private final Context mContext;
    private final List<ServerGroup> mServers = new ArrayList<>();
    private final List<Listener> mListeners = new ArrayList<>();

    public ServerListChannelData(Context context) {
        mContext = context;
    }

    public void load() {
        if (mListeners.size() > 0)
            throw new IllegalStateException("mListeners must be null");
        ServerConnectionManager mgr = ServerConnectionManager.getInstance(mContext);
        mgr.addListener(this);
        for (ServerConnectionInfo conn : mgr.getConnections())
            mServers.add(new ServerGroup(conn));
    }

    public void unload() {
        for (ServerGroup s : mServers)
            s.removeAllListeners();
        mServers.clear();
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    public List<ServerGroup> getServers() {
        return mServers;
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        ServerGroup g = new ServerGroup(connection);
        mServers.add(g);
        for (Listener l : mListeners)
            l.onServerAdded(g);
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        for (ServerGroup g : mServers) {
            if (g.mConnection == connection) {
                mServers.remove(g);
                for (Listener l : mListeners)
                    l.onServerRemoved(g);
                g.removeAllListeners();
                return;
            }
        }
    }

    public interface Group<T> {

        int size();

        T get(int i);

    }

    public class ServerGroup implements Group<ChannelEntry>,
            ServerConnectionInfo.DetailedChannelListListener {

        private ServerConnectionInfo mConnection;
        private final List<ChannelEntry> mChannels = new ArrayList<>();

        public ServerGroup(ServerConnectionInfo connection) {
            mConnection = connection;
            connection.addChannelListListener(this);
            onChannelListReset(connection.getChannels());
        }

        public ServerConnectionInfo getConnection() {
            return mConnection;
        }

        private void removeAllListeners() {
            mConnection.removeChannelListListener(this);
            for (ChannelEntry c : mChannels) {
                // force remove listeners
                c.mListeners.clear();
                c.unbind(null);
            }
        }

        @Override
        public int size() {
            return mChannels.size();
        }

        @Override
        public ChannelEntry get(int i) {
            return mChannels.get(i);
        }

        @Override
        public void onChannelJoined(ServerConnectionInfo connection, String channel) {
            UiThreadHelper.runOnUiThread(() -> {
                ChannelEntry newEntry = new ChannelEntry(this, channel);
                int i = ListUtils.lowerBound(mChannels, newEntry);
                mChannels.add(i, newEntry);
                for (Listener l : mListeners)
                    l.onChannelAdded(this, i);
            });
        }

        @Override
        public void onChannelLeft(ServerConnectionInfo connection, String channel) {
            UiThreadHelper.runOnUiThread(() -> {
                for (int i = 0; i < mChannels.size(); i++) {
                    ChannelEntry e = mChannels.get(i);
                    if (e.getName().equals(channel)) {
                        mChannels.remove(i);
                        for (Listener l : mListeners)
                            l.onChannelRemoved(this, i);
                        break;
                    }
                }
            });
        }

        @Override
        public void onChannelListReset(List<String> channels) {
            UiThreadHelper.runOnUiThread(() -> {
                int oldCount = mChannels.size();
                mChannels.clear();
                for (String e : mConnection.getChannels())
                    mChannels.add(new ChannelEntry(this, e));
                for (Listener l : mListeners)
                    l.onChannelListReset(this, oldCount);
            });
        }

    }

    public static class ChannelEntry implements ChannelInfoListener, Comparable<ChannelEntry> {

        private final ServerGroup mGroup;
        private final String mName;
        private String mTopic;
        private final List<Listener> mListeners = new ArrayList<>();

        public ChannelEntry(ServerGroup group, String name) {
            mGroup = group;
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public String getTopic() {
            return mTopic;
        }

        public void bind(Listener listener) {
            mListeners.add(listener);
            if (mListeners.size() > 1)
                return;

            ChatApi api = mGroup.mConnection.getApiInstance();
            if (api != null) {
                api.subscribeChannelInfo(mName, this, null, null);
                api.getChannelInfo(mName, (info) -> {
                    onTopicChanged(info.getTopic(), info.getTopicSetBy(), info.getTopicSetOn());
                }, null);
            }
        }

        public void unbind(Listener listener) {
            mListeners.remove(listener);
            if (mListeners.size() > 0)
                return;

            ChatApi api = mGroup.mConnection.getApiInstance();
            if (api != null) {
                api.unsubscribeChannelInfo(mName, this, null, null);
            }
        }

        @Override
        public void onTopicChanged(String s, MessageSenderInfo messageSenderInfo, Date date) {
            mTopic = s;
            UiThreadHelper.runOnUiThread(() -> {
                for (Listener l : mListeners)
                    l.onInfoChanged();
            });
        }

        @Override
        public void onMemberListChanged(List<NickWithPrefix> list) {
        }

        @Override
        public int compareTo(ChannelEntry o) {
            return mName.compareTo(o.mName);
        }

        public interface Listener {

            void onInfoChanged();

        }

    }


    public interface Listener {

        void onServerAdded(ServerGroup group);

        void onServerRemoved(ServerGroup group);


        void onChannelAdded(ServerGroup group, int index);

        void onChannelRemoved(ServerGroup group, int index);

        void onChannelListReset(ServerGroup group, int oldCount);

    }

}
