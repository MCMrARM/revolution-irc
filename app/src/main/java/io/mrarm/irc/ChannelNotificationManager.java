package io.mrarm.irc;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.config.NotificationRule;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.IRCColorUtils;

public class ChannelNotificationManager {

    public static final int CHAT_NOTIFICATION_ID_START = 10000;

    private static int mNextChatNotificationId = CHAT_NOTIFICATION_ID_START;

    private final ServerConnectionInfo mConnection;
    private final String mChannel;
    private final int mNotificationId = mNextChatNotificationId++;
    private List<NotificationMessage> mMessages = new ArrayList<>();
    private boolean mOpened = false;

    public ChannelNotificationManager(ServerConnectionInfo connection, String channel) {
        mConnection = connection;
        mChannel = channel;
    }

    public ServerConnectionInfo getConnection() {
        return mConnection;
    }

    public String getChannel() {
        return mChannel;
    }

    public List<NotificationMessage> getNotificationMessages() {
        return mMessages;
    }

    public boolean addNotificationMessage(MessageInfo messageInfo) {
        if (mOpened)
            return false;
        NotificationMessage ret = new NotificationMessage(messageInfo);
        mMessages.add(ret);
        return true;
    }

    void showNotification(Context context, NotificationRule rule) {
        String title = getChannel() + " (" + mConnection.getName() + ")"; // TODO: Move to strings.xml
        RemoteViews notificationsView = createCollapsedMessagesView(context, title);
        RemoteViews notificationsViewBig = createMessagesView(context, title);
        NotificationMessage lastMessage = mMessages.get(mMessages.size() - 1);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification
                .setContentTitle(title)
                .setContentText(lastMessage.getNotificationText(context))
                .setContentIntent(PendingIntent.getActivity(context, mNotificationId, MainActivity.getLaunchIntent(context, mConnection, mChannel), PendingIntent.FLAG_CANCEL_CURRENT))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_message)
                .setCustomContentView(notificationsView)
                .setCustomBigContentView(notificationsViewBig)
                .setGroup(NotificationManager.NOTIFICATION_GROUP_CHAT)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(context.getResources().getColor(R.color.colorNotificationMention));
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

    private RemoteViews createCollapsedMessagesView(Context context, CharSequence header) {
        NotificationMessage lastMessage = mMessages.get(mMessages.size() - 1);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_layout_collapsed);
        views.setTextViewText(R.id.message_channel, header);
        views.setTextViewText(R.id.message_text, lastMessage.getNotificationText(context));
        return views;
    }

    private RemoteViews createMessagesView(Context context, String header) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_layout);
        int[] messageIds = new int[] { R.id.message_0, R.id.message_1, R.id.message_2, R.id.message_3, R.id.message_4, R.id.message_5 };
        views.setTextViewText(R.id.message_channel, header);
        for (int i = messageIds.length - 1; i >= 0; i--) {
            int ii = mMessages.size() - messageIds.length + i;
            if (ii < 0) {
                views.setViewVisibility(messageIds[i], View.GONE);
                continue;
            }
            views.setViewVisibility(messageIds[i], View.VISIBLE);
            views.setTextViewText(messageIds[i], mMessages.get(ii).getNotificationText(context));
        }
        return views;
    }

    public void setOpened(Context context, boolean opened) {
        mOpened = opened;
        if (mOpened) {
            mMessages.clear();
            // clear the notification
            //NotificationManagerCompat.from(context). (mNotificationId);
            Log.d("Test", "Cleared notification: "+mNotificationId);
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
            return mBuilt = builder.getSpannable();
        }

        public CharSequence getNotificationText(Context context) {
            if (mBuilt == null)
                return buildNotificationText(context);
            return mBuilt;
        }

    }

}
