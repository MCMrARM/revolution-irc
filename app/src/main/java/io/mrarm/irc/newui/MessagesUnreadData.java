package io.mrarm.irc.newui;

import io.mrarm.chatlib.dto.MessageId;
import io.mrarm.irc.ChannelNotificationManager;
import io.mrarm.irc.NotificationManager;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.chat.ChannelUIData;

public final class MessagesUnreadData implements NotificationManager.UnreadMessageCountCallback {

    private final ServerConnectionInfo mConnection;
    private final ChannelNotificationManager mNotificationManager;
    private final ChannelUIData mUIInfo;
    private MessageId mFirstUnreadMessageId;
    private FirstUnreadMessageListener mFirstUnreadMessageListener;
    private UnreadMessageCountListener mUnreadMessageCountListener;

    public MessagesUnreadData(ServerConnectionInfo connection, String channel) {
        mConnection = connection;
        mNotificationManager = connection.getNotificationManager().getChannelManager(
                channel, true);
        mUIInfo = connection.getChatUIData().getChannelData(channel);
    }

    public void setFirstUnreadMessageListener(FirstUnreadMessageListener listener) {
        mFirstUnreadMessageListener = listener;
    }

    public void setUnreadMessageCountListener(UnreadMessageCountListener listener) {
        mUnreadMessageCountListener = listener;
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
            if (mUIInfo.getFirstVisibleMessage() == null && newUnreadMessageId != null)
                mUIInfo.setHasUnreadMessagesAbove(true);
            if (mFirstUnreadMessageListener != null)
                mFirstUnreadMessageListener.onFirstUnreadMesssageSet(newUnreadMessageId);
        }
    }

    public int getUnreadCount() {
        return mNotificationManager.getUnreadMessageCount();
    }

    @Override
    public void onUnreadMessageCountChanged(ServerConnectionInfo info, String channel,
                                            int messageCount, int oldMessageCount) {
        updateFirstUnreadMessage();
        if (mUnreadMessageCountListener != null)
            mUnreadMessageCountListener.onUnreadMessageCountChanged(messageCount);
    }

    public interface FirstUnreadMessageListener {

        void onFirstUnreadMesssageSet(MessageId m);

    }

    public interface UnreadMessageCountListener {

        void onUnreadMessageCountChanged(int count);

    }

}
