package io.mrarm.irc.newui;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.ChannelNotificationManager;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.chat.ChatMessagesFragment;
import io.mrarm.irc.util.MessageBuilder;

public class ChatListData {

    private final Context mContext;
    private Item[] mCachedItems;
    private final TreeSet<Item> mItems = new TreeSet<>();
    private final List<Listener> mListeners = new ArrayList<>();

    public ChatListData(Context context) {
        mContext = context;
        List<Item> items = new ArrayList<>();
        for (ServerConnectionInfo c : ServerConnectionManager.getInstance(context)
                .getConnections()) {
            for (String channel : c.getChannels())
                items.add(new Item(c, channel));
        }
        for (Item i : items)
            i.loadLastMessage();
    }

    public synchronized void updateItems() {
        if (mCachedItems == null)
            mCachedItems = mItems.toArray(new Item[0]);
    }

    public Item get(int i) {
        updateItems();
        return mCachedItems[i];
    }

    public int size() {
        updateItems();
        return mCachedItems.length;
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void onDataChanged() {
        synchronized (this) {
            mCachedItems = null;
        }
        for (Listener l : mListeners)
            l.onDataChanged();
    }


    public class Item implements Comparable<Item> {

        private WeakReference<ServerConnectionInfo> mConnection;
        private String mChannel;
        private CharSequence mLastMessageText;
        private Date mLastMessageDate;
        private final List<Runnable> mCallbacks = new ArrayList<>();

        public Item(ServerConnectionInfo connection, String channel) {
            mConnection = new WeakReference<>(connection);
            mChannel = channel;
        }

        private void loadLastMessage() {
            ServerConnectionInfo connection = mConnection.get();
            if (connection == null)
                return;
            connection.getApiInstance().getMessageStorageApi().getMessages(mChannel, 1, ChatMessagesFragment.sFilterJoinParts, null, (msgs) -> {
                try {
                    if (msgs.getMessages().size() < 1)
                        return;
                    synchronized (ChatListData.this) {
                        mItems.remove(this);
                    }
                    onDataChanged();
                    synchronized (this) {
                        MessageInfo msg = msgs.getMessages().get(0);
                        mLastMessageText = MessageBuilder.getInstance(mContext).buildMessage(msg);
                        mLastMessageDate = msg.getDate();
                    }
                    onChanged();
                    synchronized (ChatListData.this) {
                        mItems.add(this);
                    }
                    onDataChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, (Exception e) -> {
                e.printStackTrace();
            });
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

        @Override
        public synchronized int compareTo(@NonNull Item o) {
            if (mLastMessageDate == null)
                return o.mLastMessageDate == null ? 0 : -1;
            if (o.mLastMessageDate == null)
                return 1;
            return -mLastMessageDate.compareTo(o.mLastMessageDate);
        }

    }

    public interface Listener {

        void onDataChanged();

    }

}
