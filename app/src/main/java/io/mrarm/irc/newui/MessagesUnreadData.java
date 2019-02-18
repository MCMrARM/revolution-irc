package io.mrarm.irc.newui;

import io.mrarm.chatlib.dto.MessageId;
import io.mrarm.irc.ChannelNotificationManager;
import io.mrarm.irc.NotificationManager;
import io.mrarm.irc.ServerConnectionInfo;

public final class MessagesUnreadData implements NotificationManager.UnreadMessageCountCallback {

    private final ServerConnectionInfo mConnection;
    private final ChannelNotificationManager mNotificationManager;
    private MessageId mFirstUnreadMessageId;
    private FirstUnreadMessageListener mFirstUnreadMessageListener;

    public MessagesUnreadData(ServerConnectionInfo connection, String channel) {
        mConnection = connection;
        mNotificationManager = connection.getNotificationManager().getChannelManager(
                channel, true);
    }

    public void setFirstUnreadMessageListener(FirstUnreadMessageListener listener) {
        mFirstUnreadMessageListener = listener;
    }

    public void load() {
        mConnection.getNotificationManager().addUnreadMessageCountCallback(this);
        updateFirstUnreadMessage();
    }

    public void unload() {
        mConnection.getNotificationManager().removeUnreadMessageCountCallback(this);
    }

    public synchronized MessageId getFirstUnreadMessageId() {
        return mFirstUnreadMessageId;
    }

    private synchronized void updateFirstUnreadMessage() {
        MessageId newUnreadMessageId = mNotificationManager.getFirstUnreadMessage();
        if (mFirstUnreadMessageId == null) {
            mFirstUnreadMessageId = newUnreadMessageId;
            if (mFirstUnreadMessageListener != null)
                mFirstUnreadMessageListener.onFirstUnreadMesssageSet(newUnreadMessageId);
        }
    }

    @Override
    public void onUnreadMessageCountChanged(ServerConnectionInfo info, String channel,
                                            int messageCount, int oldMessageCount) {
        updateFirstUnreadMessage();
    }

    public interface FirstUnreadMessageListener {

        void onFirstUnreadMesssageSet(MessageId m);

    }

}
