package io.mrarm.irc.newui;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.mrarm.chatlib.ResponseCallback;
import io.mrarm.chatlib.dto.MessageFilterOptions;
import io.mrarm.chatlib.dto.MessageId;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageList;
import io.mrarm.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.chatlib.message.MessageListener;
import io.mrarm.chatlib.message.MessageStorageApi;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.util.TwoWayList;

public class MessagesData implements MessageListener {

    private static final int ITEMS_ON_SCREEN = 100;

    private final Context mContext;
    private final ServerConnectionInfo mConnection;
    private final MessageStorageApi mSource;
    private final String mChannel;
    private Listener mListener;

    private final TwoWayList<Item> mItems = new TwoWayList<>();
    private MessageFilterOptions mFilterOptions;
    private MessageListAfterIdentifier mNewerMessages;
    private MessageListAfterIdentifier mOlderMessages;
    private CancellableMessageListCallback mLoadingMessages;
    private boolean mListenerRegistered;
    private DayMarkerHandler mDayMarkerHandler = new DayMarkerHandler(this);

    public MessagesData(Context context,  ServerConnectionInfo connection, String channel) {
        mContext = context;
        mConnection = connection;
        mSource = connection.getApiInstance().getMessageStorageApi();
        mChannel = channel;
    }

    public ServerConnectionInfo getConnection() {
        return mConnection;
    }

    public String getChannel() {
        return mChannel;
    }

    public Item get(int i) {
        return mItems.get(i);
    }

    public int size() {
        return mItems.size();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void load(MessageFilterOptions filterOptions) {
        Log.d("MessagesData", "Loading messages");
        synchronized (this) {
            mFilterOptions = filterOptions;
            unload();
            mLoadingMessages = new CancellableMessageListCallback() {
                @Override
                public void onResponse(MessageList l) {
                    synchronized (MessagesData.this) {
                        if (isCancelled())
                            return;
                        mNewerMessages = l.getNewer();
                        mOlderMessages = l.getOlder();
                        mLoadingMessages = null;
                        Log.d("MessagesData", "Loaded " +
                                l.getMessages().size() + " messages");
                        setMessages(l.getMessages(), l.getMessageIds());
                        mConnection.getApiInstance().getMessageStorageApi().subscribeChannelMessages(
                                mChannel, MessagesData.this, null, null);
                        mListenerRegistered = true;
                    }
                }
            };
        }
        mSource.getMessages(mChannel, ITEMS_ON_SCREEN, filterOptions, null, mLoadingMessages,
                (e) -> {
                    Toast.makeText(mContext, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    Log.e("MessagesData", "Failed to load messages");
                    e.printStackTrace();
                });
    }

    public void unload() {
        synchronized (this) {
            mNewerMessages = null;
            mOlderMessages = null;
            if (mLoadingMessages != null)
                mLoadingMessages.setCancelled();
            if (mListenerRegistered) {
                try {
                    mConnection.getApiInstance().getMessageStorageApi().unsubscribeChannelMessages(
                            mChannel, MessagesData.this, null, null).get();
                } catch (Exception e) {
                    Log.e("MessagesData", "unsubscribeChannelMessages error");
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized boolean loadMoreMessages(boolean newer) {
        Log.d("MessagesData", "Loading more messages " + (newer ? "(newer)" : "(older)"));
        MessageListAfterIdentifier after = newer ? mNewerMessages : mOlderMessages;
        if (after == null || mLoadingMessages != null)
            return false;
        mLoadingMessages = new CancellableMessageListCallback() {
            @Override
            public void onResponse(MessageList l) {
                synchronized (MessagesData.this) {
                    if (isCancelled())
                        return;
                    mLoadingMessages = null;
                    Log.d("MessagesData", "Loaded " +
                            l.getMessages().size() + " messages");
                    if (newer) {
                        mNewerMessages = l.getNewer();
                        appendMessages(l.getMessages(), l.getMessageIds());
                    } else {
                        mOlderMessages = l.getOlder();
                        prependMessages(l.getMessages(), l.getMessageIds());
                    }
                }
            }
        };
        mSource.getMessages(mChannel, ITEMS_ON_SCREEN, mFilterOptions, after,
                mLoadingMessages, null);
        return true;
    }

    private int appendMessageInternal(MessageInfo m, MessageId mi) {
        int ret = 1;
        ret += mDayMarkerHandler.onMessageAppend(m);
        mItems.addLast(new MessageItem(m, mi));
        return ret;
    }

    private int prependMessageInternal(MessageInfo m, MessageId mi) {
        int ret = 1;
        ret += mDayMarkerHandler.onMessagePrepend(m);
        mItems.addFirst(new MessageItem(m, mi));
        return ret;
    }

    private void appendMessage(MessageInfo m, MessageId mi) {
        int pos = mItems.size();
        int cnt = appendMessageInternal(m, mi);
        mListener.onItemsAdded(pos, cnt);
    }

    private void appendMessages(List<MessageInfo> m, List<MessageId> mi) {
        int pos = mItems.size();
        int cnt = 0;
        for (int i = 0; i < m.size(); i++)
            cnt += appendMessageInternal(m.get(i), mi.get(i));
        mListener.onItemsAdded(pos, cnt);
    }

    private void prependMessages(List<MessageInfo> m, List<MessageId> mi) {
        mDayMarkerHandler.onBeforeMessagePrepend();
        int cnt = 0;
        for (int i = m.size() - 1; i >= 0; i--)
            cnt += prependMessageInternal(m.get(i), mi.get(i));
        mListener.onItemsAdded(0, cnt);
        mDayMarkerHandler.onAfterMessagePrepend();
    }

    private void setMessages(List<MessageInfo> m, List<MessageId> mi) {
        mItems.clear();
        mDayMarkerHandler.onClear();
        for (int i = 0; i < m.size(); i++)
            appendMessageInternal(m.get(i), mi.get(i));
        mListener.onReloaded();
    }

    @Override
    public void onMessage(String channel, MessageInfo message, MessageId messageId) {
        if (mNewerMessages == null)
            appendMessage(message, messageId);
    }

    public static class Item {
    }

    public static class MessageItem extends Item {

        private final MessageInfo mMessage;
        private final MessageId mMessageId;

        public MessageItem(MessageInfo message, MessageId messageId) {
            mMessage = message;
            mMessageId = messageId;
        }

        public MessageInfo getMessage() {
            return mMessage;
        }

        public MessageId getMessageId() {
            return mMessageId;
        }

    }

    public static class DayMarkerItem extends Item {

        int mDate;

        public DayMarkerItem(int date) {
            mDate = date;
        }

        public String getMessageText(Context ctx) {
            return DateUtils.formatDateTime(ctx, DayMarkerHandler.getDateIntMs(mDate),
                    DateUtils.FORMAT_SHOW_DATE);
        }

    }

    private static class DayMarkerHandler {

        private static final Calendar sDayIntCalendar = Calendar.getInstance();
        private static final int sDaysInYear = sDayIntCalendar.getMaximum(Calendar.DAY_OF_YEAR);

        private MessagesData mData;

        private int mFirstMessageDate = -1;
        private int mLastMessageDate = -1;

        public DayMarkerHandler(MessagesData data) {
            mData = data;
        }

        public void onClear() {
            mFirstMessageDate = -1;
            mLastMessageDate = -1;
        }

        public int onMessageAppend(MessageInfo messageInfo) {
            int date = getDayInt(messageInfo.getDate());
            if (mFirstMessageDate == -1)
                mFirstMessageDate = date;
            if (date != mLastMessageDate) {
                mLastMessageDate = date;
                mData.mItems.addLast(new DayMarkerItem(date));
                return 1;
            }
            return 0;
        }

        public void onBeforeMessagePrepend() {
            if (mData.mItems.size() == 0)
                return;
            if (mData.mItems.get(0) instanceof DayMarkerItem) {
                mData.mItems.removeFirst();
                mData.mListener.onItemsRemoved(0, 1);
            }
        }

        public void onAfterMessagePrepend() {
            if (mData.mItems.size() == 0)
                return;
            mData.mItems.addFirst(new DayMarkerItem(mFirstMessageDate));
        }

        public int onMessagePrepend(MessageInfo messageInfo) {
            int date = getDayInt(messageInfo.getDate());
            if (mLastMessageDate == -1)
                mLastMessageDate = date;
            if (date != mFirstMessageDate) {
                mData.mItems.addFirst(new DayMarkerItem(mFirstMessageDate));
                mFirstMessageDate = date;
                return 1;
            }
            return 0;
        }


        private static int getDayInt(Date date) {
            sDayIntCalendar.setTime(date);
            return sDayIntCalendar.get(Calendar.YEAR) * (sDaysInYear + 1) +
                    sDayIntCalendar.get(Calendar.DAY_OF_YEAR);
        }

        private static long getDateIntMs(int date) {
            sDayIntCalendar.setTimeInMillis(0);
            sDayIntCalendar.set(Calendar.YEAR, date / (sDaysInYear + 1));
            sDayIntCalendar.set(Calendar.DAY_OF_YEAR, date % (sDaysInYear + 1));
            return sDayIntCalendar.getTimeInMillis();
        }

    }

    public interface Listener {

        /**
         * The initial message list has been reloaded and items may have been reordered arbitrarily.
         */
        void onReloaded();

        /**
         * Called when new items have been added at the specified position. This might also be
         * called for a single new item.
         * @param pos the position at which the items has been added
         * @param count the number of added items
         */
        void onItemsAdded(int pos, int count);

        /**
         * Called when an item range has been removed.
         * @param pos the position at which the items have been removed
         * @param count the number of removed items
         */
        void onItemsRemoved(int pos, int count);

    }

    private abstract class CancellableMessageListCallback implements ResponseCallback<MessageList> {

        private boolean mCancelled = false;

        public boolean isCancelled() {
            synchronized (MessagesData.this) {
                return mCancelled;
            }
        }

        public void setCancelled() {
            synchronized (MessagesData.this) {
                mCancelled = true;
            }
        }

    }

}
