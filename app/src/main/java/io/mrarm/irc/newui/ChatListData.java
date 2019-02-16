package io.mrarm.irc.newui;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.ChannelNotificationManager;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.chat.ChatMessagesFragment;
import io.mrarm.irc.util.MessageBuilder;

public class ChatListData {

    private final List<Item> mItems = new ArrayList<>();

    public ChatListData(Context context) {
        for (ServerConnectionInfo c : ServerConnectionManager.getInstance(context)
                .getConnections()) {
            for (String channel : c.getChannels()) {
                mItems.add(new Item(context, c, channel));
            }
        }
    }

    public Item get(int i) {
        return mItems.get(i);
    }

    public int size() {
        return mItems.size();
    }


    public static class Item {

        private WeakReference<ServerConnectionInfo> mConnection;
        private String mChannel;
        private CharSequence mLastMessageText;
        private Date mLastMessageDate;
        private final List<Runnable> mCallbacks = new ArrayList<>();

        public Item(Context context, ServerConnectionInfo connection, String channel) {
            mConnection = new WeakReference<>(connection);
            mChannel = channel;
            connection.getApiInstance().getMessageStorageApi().getMessages(channel, 1, ChatMessagesFragment.sFilterJoinParts, null, (msgs) -> {
                if (msgs.getMessages().size() < 1)
                    return;
                synchronized (this) {
                    MessageInfo msg = msgs.getMessages().get(0);
                    mLastMessageText = MessageBuilder.getInstance(context).buildMessage(msg);
                    mLastMessageDate = msg.getDate();
                }
            }, null);
        }

        public ServerConnectionInfo getConnection() {
            return mConnection.get();
        }

        public String getConnectionName() {
            ServerConnectionInfo connection = mConnection.get();
            if (connection != null)
                return connection.getName();
            return null;
        }

        public String getChannel() {
            return mChannel;
        }

        public synchronized CharSequence getLastMessageText() {
            return mLastMessageText;
        }

        public synchronized Date getLastMessageDate() {
            return mLastMessageDate;
        }

        public int getUnreadMessageCount() {
            ServerConnectionInfo connection = mConnection.get();
            if (connection == null)
                return -1;
            ChannelNotificationManager mgr = connection.getNotificationManager()
                    .getChannelManager(mChannel, false);
            if (mgr != null)
                return mgr.getUnreadMessageCount();
            return -1;
        }

        public void observe(Runnable cb) {
            synchronized (mCallbacks) {
                mCallbacks.add(cb);
            }
        }

        public void stopObserving(Runnable cb) {
            synchronized (mCallbacks) {
                mCallbacks.remove(cb);
            }
        }

        private void onChanged() {
            synchronized (mCallbacks) {
                for (Runnable r : mCallbacks)
                    r.run();
            }
        }

    }

}
