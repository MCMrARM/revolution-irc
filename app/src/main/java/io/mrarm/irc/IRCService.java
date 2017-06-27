package io.mrarm.irc;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.util.IRCColorUtils;

public class IRCService extends Service implements ServerConnectionManager.ConnectionsListener {

    private static final String TAG = "IRCService";

    public static final int IDLE_NOTIFICATION_ID = 100;
    public static final int CHAT_SUMMARY_NOTIFICATION_ID = 101;
    public static final String ACTION_START_FOREGROUND = "start_foreground";

    private static final String NOTIFICATION_GROUP_CHAT = "chat";

    private ConnectivityChangeReceiver mConnectivityReceiver = new ConnectivityChangeReceiver();

    public static void start(Context context) {
        Intent intent = new Intent(context, IRCService.class);
        intent.setAction(ACTION_START_FOREGROUND);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        for (ServerConnectionInfo connection : ServerConnectionManager.getInstance(this).getConnections())
            onConnectionAdded(connection);
        ServerConnectionManager.getInstance(this).addListener(this);

        registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        for (ServerConnectionInfo connection : ServerConnectionManager.getInstance(this).getConnections())
            onConnectionRemoved(connection);

        unregisterReceiver(mConnectivityReceiver);
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
                    .setContentText(getString(R.string.service_status, ServerConnectionManager.getInstance(this).getConnections().size()))
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

    private RemoteViews createMessagesView(String header, List<NotificationManager.NotificationMessage> messages) {
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
            views.setTextViewText(messageIds[i], messages.get(ii).getNotificationText(this));
        }
        return views;
    }

    private void updateSummaryNotification() {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        notification
                .setContentIntent(PendingIntent.getActivity(this, CHAT_SUMMARY_NOTIFICATION_ID, new Intent(this, MainActivity.class), 0))
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_message)
                .setGroup(NOTIFICATION_GROUP_CHAT)
                .setGroupSummary(true)
                .setColor(getResources().getColor(R.color.colorNotificationMention));
        boolean first = true;
        boolean isLong = false;
        StringBuilder longBuilder = new StringBuilder();
        for (ServerConnectionInfo info : ServerConnectionManager.getInstance(this).getConnections()) {
            for (NotificationManager.ChannelNotificationData notificationData : info.getNotificationManager().getChannelNotificationDataList()) {
                if (notificationData.getNotificationMessages().size() > 0) {
                    if (first) {
                        first = false;
                        List<NotificationManager.NotificationMessage> list = notificationData.getNotificationMessages();
                        notification
                                .setContentText(notificationData.getChannel())
                                .setContentText(list.get(list.size() - 1).getNotificationText(this));
                    } else {
                        longBuilder.append(", ");
                        isLong = true;
                    }
                    longBuilder.append(notificationData.getChannel());
                }
            }
        }
        if (isLong) {
            notification
                    .setContentTitle(getString(R.string.notify_multiple_messages))
                    .setContentText(longBuilder.toString());
        }
        NotificationManagerCompat.from(this).notify(CHAT_SUMMARY_NOTIFICATION_ID, notification.build());
    }

    private void onMessage(ServerConnectionInfo connection, String channel, MessageInfo info) {
        if (info.getMessage() == null)
            return;
        if (info.getBatch() != null && info.getBatch().getType().equals("znc.in/playback"))
            return; // no notifications for znc playback
        NotificationRule rule = connection.getNotificationManager().findRule(channel, info);
        if (rule != null) {
            NotificationManager.ChannelNotificationData notificationData = connection.getNotificationManager().getChannelNotificationData(channel, true);
            NotificationManager.NotificationMessage messageData = notificationData.addNotificationMessage(info);

            int notificationId = notificationData.getNotificationId();
            String title = channel + " (" + connection.getName() + ")";

            updateSummaryNotification();

            RemoteViews notificationsView = createCollapsedMessagesView(title, messageData.getNotificationText(this));
            RemoteViews notificationsViewBig = createMessagesView(title, notificationData.getNotificationMessages());
            NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
            notification
                    .setContentTitle(title)
                    .setContentText(messageData.getNotificationText(this))
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
                if (rule.settings.soundUri != null)
                    notification.setSound(Uri.parse(rule.settings.soundUri));
                else
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

    public class ConnectivityChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Connectivity changed");

            if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                Log.d(TAG, "No network connectivity");
                return;
            }

            ServerConnectionManager.getInstance(context).notifyConnectivityChanged();
        }

    }

}
