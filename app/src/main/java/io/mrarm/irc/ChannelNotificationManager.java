package io.mrarm.irc;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.MessageId;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.chat.SendMessageHelper;
import io.mrarm.irc.config.NotificationCountStorage;
import io.mrarm.irc.config.NotificationRule;
import io.mrarm.irc.config.NotificationRuleManager;
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
    private boolean mShowingNotification = false;
    private static final Object mShowingNotificationLock = new Object();
    private List<NotificationMessage> mMessages = new ArrayList<>();
    private boolean mOpened = false;
    private int mUnreadMessageCount;
    private MessageId mFirstUnreadMessage;

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

    public boolean addNotificationMessage(MessageInfo messageInfo, MessageId messageId) {
        synchronized (this) {
            if (mOpened)
                return false;
            NotificationMessage ret = new NotificationMessage(messageInfo, messageId);
            mMessages.add(ret);
        }
        return true;
    }

    public void addUnreadMessage(MessageId msgId) {
        synchronized (this) {
            if (mOpened && mUnreadMessageCount == 0)
                return;
            mUnreadMessageCount++;
            NotificationManager.getInstance().callUnreadMessageCountCallbacks(mConnection, mChannel,
                    mUnreadMessageCount, mUnreadMessageCount - 1);
            if (mChannel != null) {
                mStorage.requestIncrementChannelCounter(mConnection.getUUID(), getChannel());
            }
            if (mFirstUnreadMessage == null) {
                mFirstUnreadMessage = msgId;
                mStorage.requestSetFirstMessageId(mConnection.getUUID(), getChannel(),
                        mFirstUnreadMessage.toString());
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

    public MessageId getFirstUnreadMessage() {
        return mFirstUnreadMessage;
    }

    public void setOpened(Context context, boolean opened) {
        boolean updateSummary = false;
        synchronized (this) {
            mOpened = opened;
            if (mOpened) {
                mMessages.clear();

                synchronized (mShowingNotificationLock) {
                    // cancel the notification
                    if (mShowingNotification) {
                        NotificationManagerCompat.from(context).cancel(mNotificationId);
                        mShowingNotification = false;
                        updateSummary = true;
                    }
                }
            }
        }
        if (updateSummary)
            NotificationManager.getInstance().updateSummaryNotification(context, null);
    }

    public void clearUnreadMessages() {
        synchronized (this) {
            int prevCount = mUnreadMessageCount;
            mUnreadMessageCount = 0;
            NotificationManager.getInstance().callUnreadMessageCountCallbacks(mConnection,
                    mChannel, 0, prevCount);
            mStorage.requestResetChannelCounter(mConnection.getUUID(), getChannel());
            mFirstUnreadMessage = null;
            mStorage.requestResetFirstMessageId(mConnection.getUUID(), getChannel());
        }
    }

    void showNotification(Context context, NotificationRule rule) {
        NotificationMessage lastMessage;
        synchronized (this) {
            if (mMessages.size() == 0)
                return;
            lastMessage = mMessages.get(mMessages.size() - 1);
        }

        if (rule.settings.notificationChannelId == null)
            createChannel(context, rule);

        String title = getChannel() + " (" + mConnection.getName() + ")"; // TODO: Move to strings.xml
        RemoteViews notificationsView = createCollapsedMessagesView(context, title, lastMessage);
        RemoteViews notificationsViewBig = createMessagesView(context, title);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                rule.settings.notificationChannelId);
        PendingIntent intent = PendingIntent.getActivity(context, mNotificationId,
                MainActivity.getLaunchIntent(context, mConnection, mChannel,
                        lastMessage.mMessageId.toString()),
                PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent dismissIntent = PendingIntent.getBroadcast(context,
                CHAT_DISMISS_INTENT_ID_START + mNotificationId,
                NotificationActionReceiver.getDismissIntent(context, mConnection, mChannel),
                PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent replyIntent = PendingIntent.getBroadcast(context,
                CHAT_REPLY_INTENT_ID_START + mNotificationId,
                NotificationActionReceiver.getReplyIntent(context, mConnection, mChannel,
                        mNotificationId),
                PendingIntent.FLAG_UPDATE_CURRENT);
        int replyIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                R.drawable.ic_reply : R.drawable.ic_notification_reply;
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                replyIcon, context.getString(R.string.action_reply), replyIntent)
                .addRemoteInput(new RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY_TEXT)
                        .setLabel(context.getString(R.string.action_reply))
                        .build())
                .build();
        notification
                .setContentTitle(title)
                .setContentText(lastMessage.getNotificationText(context))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setPriority(rule.settings.priority + 1)
                .setSmallIcon(R.drawable.ic_notification_message)
                .setCustomContentView(notificationsView)
                .setCustomBigContentView(notificationsViewBig)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(context.getResources().getColor(R.color.colorNotificationMention))
                .addAction(replyAction)
                .setDeleteIntent(dismissIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification
                    .setGroup(NotificationManager.NOTIFICATION_GROUP_CHAT)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
        }
        int defaults = 0;
        if (rule.settings.soundEnabled) {
            if (rule.settings.soundUri != null)
                notification.setSound(Uri.parse(rule.settings.soundUri));
            else
                defaults |= NotificationCompat.DEFAULT_SOUND;
        }
        if (rule.settings.vibrationEnabled) {
            if (rule.settings.vibrationDuration != 0)
                notification.setVibrate(new long[]{0, rule.settings.vibrationDuration});
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
        synchronized (this) {
            NotificationManagerCompat.from(context).notify(mNotificationId, notification.build());
            mShowingNotification = true;
        }
    }

    public static void createChannel(Context context, NotificationRule rule) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || rule.settings.noNotification)
            return;
        NotificationRuleManager.loadUserRuleSettings(context);
        String id = rule.settings.notificationChannelId;
        if (id == null) {
            id = UUID.randomUUID().toString();
            rule.settings.notificationChannelId = id;
            NotificationRuleManager.saveUserRuleSettings(context);
        }
        String name = rule.getName();
        if (name == null)
            name = context.getString(rule.getNameId());
        NotificationChannel channel = new NotificationChannel(id, name,
                android.app.NotificationManager.IMPORTANCE_HIGH);
        if (rule.getNameId() != -1)
            channel.setGroup(NotificationManager.getDefaultNotificationChannelGroup(context));
        else
            channel.setGroup(NotificationManager.getUserNotificationChannelGroup(context));
        if (rule.settings.soundEnabled) {
            if (rule.settings.soundUri != null)
                channel.setSound(Uri.parse(rule.settings.soundUri), new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .build());
        } else {
            channel.setSound(null, null);
        }
        if (rule.settings.vibrationEnabled) {
            channel.enableVibration(true);
            if (rule.settings.vibrationDuration != 0)
                channel.setVibrationPattern(new long[]{0, rule.settings.vibrationDuration});
        }
        if (rule.settings.lightEnabled) {
            channel.enableLights(true);
            if (rule.settings.light != 0)
                channel.setLightColor(rule.settings.light);
        }
        android.app.NotificationManager mgr = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        mgr.createNotificationChannel(channel);
    }

    public void cancelNotification(Context context) {
        boolean updateSummary = false;
        synchronized (this) {
            mMessages.clear();
        }
        synchronized (mShowingNotificationLock) {
            // cancel the notification
            if (mShowingNotification) {
                NotificationManagerCompat.from(context).cancel(mNotificationId);
                mShowingNotification = false;
                updateSummary = true;
            }
        }
        if (updateSummary)
            NotificationManager.getInstance().updateSummaryNotification(context, null);
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
    public void onChannelCounterResult(UUID server, String channel, int result, String msgId) {
        synchronized (this) {
            mUnreadMessageCount += result;
            if (msgId != null)
                mFirstUnreadMessage = mConnection.getMessageIdParser().parse(msgId);
            if (mChannel != null) {
                NotificationManager.getInstance().callUnreadMessageCountCallbacks(mConnection,
                        mChannel, mUnreadMessageCount, mUnreadMessageCount - result);
            }
        }
    }


    public static class NotificationMessage {

        private MessageId mMessageId;
        private String mSender;
        private String mText;
        private CharSequence mBuilt;

        public NotificationMessage(MessageId messageId, String sender, String text) {
            this.mMessageId = messageId;
            this.mSender = sender;
            this.mText = text;
        }

        public NotificationMessage(MessageInfo messageInfo, MessageId messageId) {
            this(messageId, messageInfo.getSender().getNick(), messageInfo.getMessage());
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
        private static final String ARG_NOTIFICATION_ID = "notification_id";
        private static final String ARG_SERVER_ALL = "all_servers";

        private static final String ACTION_DISMISS = "dismiss";
        private static final String ACTION_REPLY = "reply";

        public static final String KEY_REPLY_TEXT = "reply_text";

        public static Intent getDismissIntent(Context context, ServerConnectionInfo server,
                                              String channel) {
            Intent ret = new Intent(context, NotificationActionReceiver.class);
            ret.putExtra(ARG_ACTION, ACTION_DISMISS);
            ret.putExtra(ARG_SERVER_UUID, server.getUUID().toString());
            ret.putExtra(ARG_CHANNEL, channel);
            return ret;
        }

        public static Intent getDismissIntentForSummary(Context context) {
            Intent ret = new Intent(context, NotificationActionReceiver.class);
            ret.putExtra(ARG_ACTION, ACTION_DISMISS);
            ret.putExtra(ARG_SERVER_ALL, true);
            return ret;
        }

        public static Intent getReplyIntent(Context context, ServerConnectionInfo server,
                                            String channel, int notificationId) {
            Intent ret = new Intent(context, NotificationActionReceiver.class);
            ret.putExtra(ARG_ACTION, ACTION_REPLY);
            ret.putExtra(ARG_SERVER_UUID, server.getUUID().toString());
            ret.putExtra(ARG_CHANNEL, channel);
            ret.putExtra(ARG_NOTIFICATION_ID, notificationId);
            return ret;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(ARG_SERVER_ALL, false)) {
                ServerConnectionManager mgr = ServerConnectionManager.getInstance(context);
                for (ServerConnectionInfo conn : mgr.getConnections())
                    NotificationManager.getInstance().clearAllNotifications(context, conn);
                return;
            }
            UUID uuid = UUID.fromString(intent.getStringExtra(ARG_SERVER_UUID));
            ServerConnectionInfo conn = ServerConnectionManager.getInstance(context).getConnection(uuid);
            if (conn == null)
                return;
            String channel = intent.getStringExtra(ARG_CHANNEL);
            String action = intent.getStringExtra(ARG_ACTION);
            if (ACTION_DISMISS.equals(action)) {
                NotificationManager.getInstance().onNotificationDismissed(context, conn, channel);
            } else if (ACTION_REPLY.equals(action)) {
                Bundle inputData = RemoteInput.getResultsFromIntent(intent);
                if (inputData == null || !(inputData.containsKey(KEY_REPLY_TEXT)))
                    return;

                SendMessageHelper.sendMessage(context, conn, channel,
                        new SpannableString(inputData.getCharSequence(KEY_REPLY_TEXT)),
                        new NotificationSendMessageCallback(context, conn, channel,
                                intent.getIntExtra(ARG_NOTIFICATION_ID, -1)));
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
            mNotificationId = notifId;
        }

        @Override
        public void onMessageSent() {
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(mContext);
            notificationManager.cancel(mNotificationId);

            NotificationManager.getInstance().onNotificationDismissed(mContext, mConnection,
                    mChannel);
            NotificationManager.getInstance().updateSummaryNotification(mContext, null);
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
