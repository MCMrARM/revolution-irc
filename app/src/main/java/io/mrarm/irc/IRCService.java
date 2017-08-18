package io.mrarm.irc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.util.WarningHelper;

public class IRCService extends Service implements ServerConnectionManager.ConnectionsListener {

    private static final String TAG = "IRCService";

    public static final int IDLE_NOTIFICATION_ID = 100;
    public static final String ACTION_START_FOREGROUND = "start_foreground";

    private static final String IDLE_NOTIFICATION_CHANNEL = "IdleNotification";

    private ConnectivityChangeReceiver mConnectivityReceiver = new ConnectivityChangeReceiver();

    private boolean mCreatedChannel = false;

    public static void start(Context context) {
        Intent intent = new Intent(context, IRCService.class);
        intent.setAction(ACTION_START_FOREGROUND);
        context.startService(intent);
    }
    public static void stop(Context context) {
        context.stopService(new Intent(context, IRCService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        WarningHelper.setAppContext(getApplicationContext());

        ChatLogStorageManager.getInstance(getApplicationContext());

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !mCreatedChannel) {
                NotificationChannel channel = new NotificationChannel(IDLE_NOTIFICATION_CHANNEL, getString(R.string.notification_channel_idle), android.app.NotificationManager.IMPORTANCE_MIN);
                android.app.NotificationManager mgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mgr.createNotificationChannel(channel);
                mCreatedChannel = true;
            }

            Intent mainIntent = MainActivity.getLaunchIntent(this, null, null);
            int connectionCount = ServerConnectionManager.getInstance(this).getConnections().size();
            NotificationCompat.Builder notification = new NotificationCompat.Builder(this, IDLE_NOTIFICATION_CHANNEL)
                    .setContentTitle(getString(R.string.service_title))
                    .setContentText(getResources().getQuantityString(R.plurals.service_status, connectionCount, connectionCount))
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(PendingIntent.getActivity(this, IDLE_NOTIFICATION_ID, mainIntent, PendingIntent.FLAG_CANCEL_CURRENT));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                notification.setSmallIcon(R.drawable.ic_server_connected);
            else
                notification.setSmallIcon(R.drawable.ic_notification_connected);
            startForeground(IDLE_NOTIFICATION_ID, notification.build());
        }
        return START_STICKY;
    }

    private void onMessage(ServerConnectionInfo connection, String channel, MessageInfo info) {
        NotificationManager.getInstance().processMessage(this, connection, channel, info);
        ChatLogStorageManager.getInstance(this).onMessage(connection);
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        connection.getApiInstance().getMessageStorageApi().subscribeChannelMessages(null, (String channel, MessageInfo info) -> {
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
