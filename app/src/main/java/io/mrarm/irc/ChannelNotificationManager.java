package io.mrarm.irc;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.chat.SendMessageHelper;
import io.mrarm.irc.config.NotificationCountStorage;
import io.mrarm.irc.config.NotificationRule;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.IRCColorUtils;

public class ChannelNotificationManager implements NotificationCountStorage.OnChannelCounterResult {

    public static final int CHAT_NOTIFICATION_ID_START = 10000;
    public static final int CHAT_DISMISS_INTENT_ID_START = 10000000;
    public static final int CHAT_REPLY_INTENT_ID_START = 20000000;

    private static int mNextChatNotificationId = CHAT_NOTIFICATION_ID_START;

    private final ServerConnectionInfo mConnection;
    private final NotificationCountStorage mStorage;
    private final String mChannel;
    private final int mNotificationId = mNextChatNotificationId++;
    private List<NotificationMessage> mMessages = new ArrayList<>();
    private boolean mOpened = false;
    private int mUnreadMessageCount;

    public ChannelNotificationManager(ServerConnectionInfo connection, String channel) {
        mConnection = connection;
        mChannel = channel;
        mStorage = NotificationCountStorage.getInstance(connection.getConnectionManager().getContext());
        if (mChannel != null) {
            mStorage.requestGetChannelCounter(connection.getUUID(), channel, new WeakReference<>(this));
        }
    }

    public ServerConnectionInfo getConnection() {
        return mConnection;
    }

    public String getChannel() {
        return mChannel;
    }

    public int getNotificationMessageCount() {
        synchronized (this) {
            return mMessages.size();
        }
    }

    public NotificationMessage getNotificationMessage(int index) {
        synchronized (this) {
            return mMessages.get(index);
        }
    }

    public boolean addNotificationMessage(MessageInfo messageInfo) {
        synchronized (this) {
            if (mOpened)
                return false;
            NotificationMessage ret = new NotificationMessage(messageInfo);
            mMessages.add(ret);
        }
        return true;
    }

    public void addUnreadMessage() {
        synchronized (this) {
            if (mOpened)
                return;
            mUnreadMessageCount++;
            NotificationManager.getInstance().callUnreadMessageCountCallbacks(mConnection, mChannel,
                    mUnreadMessageCount, mUnreadMessageCount - 1);
            if (mChannel != null) {
                mStorage.requestIncrementChannelCounter(mConnection.getUUID(), getChannel());
            }
        }
    }

    public int getUnreadMessageCount() {
        synchronized (this) {
            return mUnreadMessageCount;
        }
    }

    public boolean hasUnreadMessages() {
        return getUnreadMessageCount() > 0;
    }

    public void setOpened(Context context, boolean opened) {
        synchronized (this) {
            mOpened = opened;
            if (mOpened) {
                mMessages.clear();
                int prevCount = mUnreadMessageCount;
                mUnreadMessageCount = 0;
                NotificationManager.getInstance().callUnreadMessageCountCallbacks(mConnection,
                        mChannel, 0, prevCount);
                mStorage.requestResetChannelCounter(mConnection.getUUID(), getChannel());

                // cancel the notification
                NotificationManagerCompat.from(context).cancel(mNotificationId);
            }
        }
        NotificationManager.getInstance().updateSummaryNotification(context);
    }

    void showNotification(Context context, NotificationRule rule) {
        NotificationMessage lastMessage;
        synchronized (this) {
            if (mMessages.size() == 0)
                return;
            lastMessage = mMessages.get(mMessages.size() - 1);
        }

        String title = getChannel() + " (" + mConnection.getName() + ")"; // TODO: Move to strings.xml
        RemoteViews notificationsView = createCollapsedMessagesView(context, title, lastMessage);
        RemoteViews notificationsViewBig = createMessagesView(context, title);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        PendingIntent intent = PendingIntent.getActivity(context, mNotificationId,
                MainActivity.getLaunchIntent(context, mConnection, mChannel),
                PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent dismissIntent = PendingIntent.getBroadcast(context,
                CHAT_DISMISS_INTENT_ID_START + mNotificationId,
                NotificationActionReceiver.getDismissIntent(context, mConnection, mChannel),
                PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent replyIntent = PendingIntent.getBroadcast(context,
                CHAT_REPLY_INTENT_ID_START + mNotificationId,
                NotificationActionReceiver.getReplyIntent(context, mConnection, mChannel),
                PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_reply, context.getString(R.string.action_reply), replyIntent)
                .addRemoteInput(new RemoteInput.Builder(NotificationActionReceiver.ACTION_REPLY)
                        .setLabel(context.getString(R.string.action_reply))
                        .build())
                .build();
        notification
                .setContentTitle(title)
                .setContentText(lastMessage.getNotificationText(context))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_notification_message)
                .setCustomContentView(notificationsView)
                .setCustomBigContentView(notificationsViewBig)
                .setGroup(NotificationManager.NOTIFICATION_GROUP_CHAT)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(context.getResources().getColor(R.color.colorNotificationMention))
                .addAction(replyAction)
                .setDeleteIntent(dismissIntent);
        int defaults = 0;
        if (rule.settings.soundEnabled) {
            if (rule.settings.soundUri != null)
                notification.setSound(Uri.parse(rule.settings.soundUri));
            else
                defaults |= NotificationCompat.DEFAULT_SOUND;
        }
        if (rule.settings.vibrationEnabled) {
            if (rule.settings.vibrationDuration != 0)
                notification.setVibrate(new long[]{rule.settings.vibrationDuration});
            else
                defaults |= NotificationCompat.DEFAULT_VIBRATE;
        }
        if (rule.settings.lightEnabled) {
            if (rule.settings.light != 0)
                notification.setLights(rule.settings.light, 500, 2000); // TODO: Make those on/off values customizable?
            else
                defaults |= NotificationCompat.DEFAULT_LIGHTS;
        }
        if (!rule.settings.soundEnabled && !rule.settings.vibrationEnabled) {
            notification.setVibrate(new long[]{0}); // a hack to get a headsup to show
        }
        notification.setDefaults(defaults);
        NotificationManagerCompat.from(context).notify(mNotificationId, notification.build());
    }

    void cancelNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(mNotificationId);
    }

    private RemoteViews createCollapsedMessagesView(Context context, CharSequence header,
                                                    NotificationMessage message) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_layout_collapsed);
        views.setTextViewText(R.id.message_channel, header);
        views.setTextViewText(R.id.message_text, message.getNotificationText(context));
        return views;
    }

    private RemoteViews createMessagesView(Context context, String header) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_layout);
        int[] messageIds = new int[] { R.id.message_0, R.id.message_1, R.id.message_2, R.id.message_3, R.id.message_4, R.id.message_5 };
        views.setTextViewText(R.id.message_channel, header);
        synchronized (this) {
            for (int i = messageIds.length - 1; i >= 0; i--) {
                int ii = mMessages.size() - messageIds.length + i;
                if (ii < 0) {
                    views.setViewVisibility(messageIds[i], View.GONE);
                    continue;
                }
                views.setViewVisibility(messageIds[i], View.VISIBLE);
                views.setTextViewText(messageIds[i], mMessages.get(ii).getNotificationText(context));
            }
        }
        return views;
    }

    void onNotificationDismissed() {
        synchronized (this) {
            mMessages.clear();
        }
    }

    @Override
    public void onChannelCounterResult(UUID server, String channel, int result) {
        synchronized (this) {
            if (mOpened)
                return;
            mUnreadMessageCount += result;
            if (mChannel != null) {
                NotificationManager.getInstance().callUnreadMessageCountCallbacks(mConnection,
                        mChannel, mUnreadMessageCount, mUnreadMessageCount - result);
            }
        }
    }


    public static class NotificationMessage {

        private String mSender;
        private String mText;
        private CharSequence mBuilt;

        public NotificationMessage(String sender, String text) {
            this.mSender = sender;
            this.mText = text;
        }

        public NotificationMessage(MessageInfo messageInfo) {
            this(messageInfo.getSender().getNick(), messageInfo.getMessage());
        }

        private CharSequence buildNotificationText(Context context) {
            int nickColor = IRCColorUtils.getNickColor(context, mSender);
            ColoredTextBuilder builder = new ColoredTextBuilder();
            builder.append(mSender + ": ", new ForegroundColorSpan(nickColor));
            builder.append(mText);
            return builder.getSpannable();
        }

        public synchronized CharSequence getNotificationText(Context context) {
            if (mBuilt == null)
                return mBuilt = buildNotificationText(context);
            return mBuilt;
        }

    }


    public static class NotificationActionReceiver extends BroadcastReceiver {

        private static final String ARG_ACTION = "action";
        private static final String ARG_SERVER_UUID = "server_uuid";
        private static final String ARG_CHANNEL = "channel";

        private static final String ACTION_DISMISS = "dismiss";
        private static final String ACTION_REPLY = "reply";

        public static final String ARG_REPLY_TEXT = "reply_text";

        public static Intent getDismissIntent(Context context, ServerConnectionInfo server,
                                              String channel) {
            Intent ret = new Intent(context, NotificationActionReceiver.class);
            ret.putExtra(ARG_ACTION, ACTION_DISMISS);
            ret.putExtra(ARG_SERVER_UUID, server.getUUID().toString());
            ret.putExtra(ARG_CHANNEL, channel);
            return ret;
        }

        public static Intent getReplyIntent(Context context, ServerConnectionInfo server,
                                              String channel) {
            Intent ret = new Intent(context, NotificationActionReceiver.class);
            ret.putExtra(ARG_ACTION, ACTION_REPLY);
            ret.putExtra(ARG_SERVER_UUID, server.getUUID().toString());
            ret.putExtra(ARG_CHANNEL, channel);
            return ret;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            UUID uuid = UUID.fromString(intent.getStringExtra(ARG_SERVER_UUID));
            ServerConnectionInfo conn = ServerConnectionManager.getInstance(context).getConnection(uuid);
            if (conn == null)
                return;
            String channel = intent.getStringExtra(ARG_CHANNEL);
            String action = intent.getStringExtra(ARG_ACTION);
            if (ACTION_DISMISS.equals(action)) {
                NotificationManager.getInstance().onNotificationDismissed(context, conn, channel);
            } else if (ACTION_REPLY.equals(action)) {
                ChannelNotificationManager mgr =
                        conn.getNotificationManager().getChannelManager(channel, false);
                if (mgr == null)
                    return;
                SendMessageHelper.sendMessage(context, conn, channel,
                        new SpannableString(intent.getCharSequenceExtra(ARG_REPLY_TEXT)),
                        new NotificationSendMessageCallback(context, conn, channel,
                                mgr.mNotificationId));
            }
        }

    }

    private static class NotificationSendMessageCallback implements SendMessageHelper.Callback {

        private Context mContext;
        private ServerConnectionInfo mConnection;
        private String mChannel;
        private int mNotificationId;

        public NotificationSendMessageCallback(Context context, ServerConnectionInfo conn,
                                               String channel, int notifId) {
            mContext = context;
            mConnection = conn;
            mChannel = channel;
        }

        @Override
        public void onMessageSent() {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(mContext);
            notification
                    .setContentText(mContext.getString(R.string.message_sent))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_notification_message)
                    .setColor(mContext.getResources().getColor(R.color.colorNotificationMention));
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(mContext);
            notificationManager.notify(mNotificationId, notification.build());
        }

        @Override
        public void onRawCommandExecuted(String clientCommand, String sentCommand) {
        }

        @Override
        public void onNoCommandHandlerFound(String message) {
        }

        @Override
        public void onClientCommandError(CharSequence error) {
        }

    }

}
