package io.mrarm.irc.newui;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.mrarm.chatlib.ChannelInfoListener;
import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.util.ListUtils;
import io.mrarm.irc.util.UiThreadHelper;

public class ServerChannelListData implements ServerConnectionInfo.DetailedChannelListListener {

    private final Context mContext;
    private final ServerConnectionInfo mConnection;
    private final List<Listener> mListeners = new ArrayList<>();
    private final List<ChannelGroup> mGroups = new ArrayList<>();

    public ServerChannelListData(Context context, ServerConnectionInfo connection) {
        mContext = context;
        mConnection = connection;
    }

    public String getName() {
        return mConnection.getName();
    }

    public void load() {
        if (mListeners.size() > 0)
            throw new IllegalStateException("mListeners must be null");
        mConnection.addChannelListListener(this);
        mGroups.clear();
        mGroups.add(new ChannelGroup(mContext.getString(R.string.server_list_uncategorized)));
        onChannelListReset(mConnection, mConnection.getChannels());
    }

    public void unload() {
        mConnection.removeChannelListListener(this);
        for (ChannelGroup s : mGroups)
            s.removeAllListeners();
    }

    public List<ChannelGroup> getGroups() {
        return mGroups;
    }

    @Override
    public void onChannelJoined(ServerConnectionInfo connection, String channel) {
        UiThreadHelper.runOnUiThread(() -> {
            ChannelGroup group = mGroups.get(0);
            ChannelEntry newEntry = new ChannelEntry(group, channel);
            int i = ListUtils.lowerBound(group.mChannels, newEntry);
            group.mChannels.add(i, newEntry);
            for (Listener l : mListeners)
                l.onChannelAdded(group, i);
        });
    }

    @Override
    public void onChannelLeft(ServerConnectionInfo connection, String channel) {
        UiThreadHelper.runOnUiThread(() -> {
            ChannelGroup group = mGroups.get(0);
            for (int i = 0; i < group.mChannels.size(); i++) {
                ChannelEntry e = group.mChannels.get(i);
                if (e.getName().equals(channel)) {
                    group.mChannels.remove(i);
                    for (Listener l : mListeners)
                        l.onChannelRemoved(group, i);
                    break;
                }
            }
        });
    }

    @Override
    public void onChannelListReset(ServerConnectionInfo connection, List<String> channels) {
        UiThreadHelper.runOnUiThread(() -> {
            ChannelGroup group = mGroups.get(0);
            int oldCount = group.mChannels.size();
            group.mChannels.clear();
            for (String e : mConnection.getChannels())
                group.mChannels.add(new ChannelEntry(group, e));
            for (Listener l : mListeners)
                l.onChannelListReset(group, oldCount);
        });
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    public interface Group<T> {

        int size();

        T get(int i);

        String getName();

    }

    public class ChannelGroup implements Group<ChannelEntry> {

        private final String mName;
        private final List<ChannelEntry> mChannels = new ArrayList<>();

        public ChannelGroup(String name) {
            mName = name;
        }

        public ServerConnectionInfo getConnection() {
            return mConnection;
        }

        @Override
        public String getName() {
            return mName;
        }

        private void removeAllListeners() {
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

    }

    public static class ChannelEntry implements ChannelInfoListener, Comparable<ChannelEntry> {

        private final ChannelGroup mGroup;
        private final String mName;
        private String mTopic;
        private final List<Listener> mListeners = new ArrayList<>();

        public ChannelEntry(ChannelGroup group, String name) {
            mGroup = group;
            mName = name;
        }

        public ChannelGroup getGroup() {
            return mGroup;
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

            ChatApi api = mGroup.getConnection().getApiInstance();
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

            ChatApi api = mGroup.getConnection().getApiInstance();
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

        void onGroupAdded(Group<ChannelEntry> group);

        void onGroupRemoved(Group<ChannelEntry> group);


        void onChannelAdded(Group<ChannelEntry> group, int index);

        void onChannelRemoved(Group<ChannelEntry> group, int index);

        void onChannelListReset(Group<ChannelEntry> group, int oldCount);

    }

}
