package io.mrarm.irc;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.util.IRCColorUtils;

public class IRCService extends Service implements ServerConnectionManager.ConnectionsListener {

    public static final int IDLE_NOTIFICATION_ID = 100;
    public static final String ACTION_START_FOREGROUND = "start_foreground";

    private static final String NOTIFICATION_GROUP_CHAT = "chat";

    public static void start(Context context) {
        Intent intent = new Intent(context, IRCService.class);
        intent.setAction(ACTION_START_FOREGROUND);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        for (ServerConnectionInfo connection : ServerConnectionManager.getInstance().getConnections())
            onConnectionAdded(connection);
        ServerConnectionManager.getInstance().addListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (ServerConnectionInfo connection : ServerConnectionManager.getInstance().getConnections())
            onConnectionRemoved(connection);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null)
            return START_STICKY;
        if (action.equals(ACTION_START_FOREGROUND)) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_status, ServerConnectionManager.getInstance().getConnections().size()))
                    .setSmallIcon(R.drawable.ic_server_connected)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(PendingIntent.getActivity(this, 0, mainIntent, 0))
                    .build();
            startForeground(IDLE_NOTIFICATION_ID, notification);
        }
        return START_STICKY;
    }

    private RemoteViews createCollapsedMessagesView(String header, CharSequence message) {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_layout_collapsed);
        views.setTextViewText(R.id.message_channel, header);
        views.setTextViewText(R.id.message_text, message);
        return views;
    }

    private RemoteViews createMessagesView(String header, List<CharSequence> messages) {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_layout);
        int[] messageIds = new int[] { R.id.message_0, R.id.message_1, R.id.message_2, R.id.message_3, R.id.message_4, R.id.message_5 };
        views.setTextViewText(R.id.message_channel, header);
        for (int i = messageIds.length - 1; i >= 0; i--) {
            int ii = messages.size() - messageIds.length + i;
            if (ii < 0) {
                views.setViewVisibility(messageIds[i], View.GONE);
                continue;
            }
            views.setViewVisibility(messageIds[i], View.VISIBLE);
            views.setTextViewText(messageIds[i], messages.get(ii));
        }
        return views;
    }

    private void onMessage(ServerConnectionInfo connection, String channel, MessageInfo info) {
        if (info.getMessage() == null)
            return;
        NotificationRule rule = connection.getNotificationManager().findRule(info.getMessage());
        if (rule != null) {
            int nickColor = IRCColorUtils.getNickColor(this, info.getSender().getNick());
            ColoredTextBuilder builder = new ColoredTextBuilder();
            builder.append(info.getSender().getNick() + ": ", new ForegroundColorSpan(nickColor));
            builder.append(info.getMessage());

            List<CharSequence> backlog = connection.getNotificationManager().getServiceNotificationBacklog();
            backlog.add(builder.getSpannable());

            int notificationId = connection.getNotificationManager().getServiceNotificationId();
            String title = channel + " (" + connection.getName() + ")";

            RemoteViews notificationsView = createCollapsedMessagesView(title, builder.getSpannable());
            RemoteViews notificationsViewBig = createMessagesView(title, backlog);
            NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
            notification
                    .setContentTitle(title)
                    .setContentText(builder.getSpannable())
                    .setContentIntent(PendingIntent.getActivity(this, notificationId, MainActivity.getLaunchIntent(this, connection, channel), PendingIntent.FLAG_ONE_SHOT)) // TODO: Do not replace the activity if already open?
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_message)
                    .setCustomContentView(notificationsView)
                    .setCustomBigContentView(notificationsViewBig)
                    .setGroup(NOTIFICATION_GROUP_CHAT)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setColor(getResources().getColor(R.color.colorNotificationMention));
            int defaults = 0;
            if (rule.settings.soundEnabled) {
                defaults |= NotificationCompat.DEFAULT_SOUND;
            }
            if (rule.settings.vibrationEnabled) {
                if (rule.settings.vibrationDuration != 0)
                    notification.setVibrate(new long[] { rule.settings.vibrationDuration });
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
                notification.setVibrate(new long[] { 0 }); // a hack to get a headsup to show
            }
            notification.setDefaults(defaults);
            NotificationManagerCompat.from(this).notify(notificationId, notification.build());
        }
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        connection.getApiInstance().subscribeChannelMessages(null, (String channel, MessageInfo info) -> {
            onMessage(connection, channel, info);
        }, null, null);
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
