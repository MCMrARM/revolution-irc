package io.mrarm.irc;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

public class IRCService extends Service {

    public static final int MY_NOTIFICATION_ID = 100;
    public static final String ACTION_START_FOREGROUND = "start_foreground";

    public static void start(Context context) {
        Intent intent = new Intent(context, IRCService.class);
        intent.setAction(ACTION_START_FOREGROUND);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_START_FOREGROUND)) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getString(R.string.service_status, ServerConnectionManager.getInstance().getConnections().size()))
                    .setSmallIcon(R.drawable.ic_server_connected)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(PendingIntent.getActivity(this, 0, mainIntent, 0))
                    .build();
            startForeground(MY_NOTIFICATION_ID, notification);
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
