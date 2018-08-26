package io.mrarm.irc;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;

public class DCCNotificationManager implements DCCServerManager.UploadListener {

    private static final String NOTIFICATION_CHANNEL = "DCCTransfers";
    public static final String NOTIFICATION_GROUP_DCC = "dcc";

    public static final int DCC_SUMMARY_NOTIFICATION_ID = 10000000;
    public static final int DCC_NOTIFICATION_ID_START = DCC_SUMMARY_NOTIFICATION_ID + 1;

    private Context mContext;
    private NotificationManager mNotificationManager;
    private int mNextNotificationId = DCC_NOTIFICATION_ID_START;
    private final Map<DCCServer, List<DCCServer.UploadSession>> mUploadSessions = new HashMap<>();
    private final Map<DCCServer, Integer> mUploadNotificationIds = new HashMap<>();
    private final Map<DCCServer.UploadSession, Integer> mSessionNotificationIds = new HashMap<>();
    private final Set<Integer> mDisplayedNotificationIds = new HashSet<>();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public DCCNotificationManager(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public synchronized void onUploadCreated(DCCServerManager.UploadEntry uploadEntry) {
        mUploadSessions.put(uploadEntry.getServer(), new ArrayList<>());
        mUploadNotificationIds.put(uploadEntry.getServer(), mNextNotificationId++);
        mHandler.post(() -> createUploadNotification(uploadEntry));
    }

    @Override
    public synchronized void onUploadDestroyed(DCCServerManager.UploadEntry uploadEntry) {
        mUploadSessions.remove(uploadEntry.getServer());
        Integer nid = mUploadNotificationIds.remove(uploadEntry.getServer());
        if (nid != null)
            mHandler.post(() -> cancelNotification(nid));
    }

    @Override
    public synchronized void onSessionCreated(DCCServer dccServer,
                                              DCCServer.UploadSession uploadSession) {
        mUploadSessions.get(dccServer).add(uploadSession);
        mHandler.post(() -> {
            Integer nid = mUploadNotificationIds.get(uploadSession.getServer());
            if (nid != null)
                cancelNotification(nid);
        });
    }

    @Override
    public synchronized void onSessionDestroyed(DCCServer dccServer,
                                                DCCServer.UploadSession uploadSession) {
        List<DCCServer.UploadSession> list = mUploadSessions.get(dccServer);
        list.remove(uploadSession);
        if (list.size() == 0)
            mHandler.post(() -> createUploadNotification(DCCManager.getInstance(mContext)
                    .getUploadEntry(dccServer)));
    }

    private boolean isNotificationGroupingEnabled() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                mContext.getString(R.string.notification_channel_dcc),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setShowBadge(false);
        mNotificationManager.createNotificationChannel(channel);
    }

    private void createSummaryNotification() {
        if (!isNotificationGroupingEnabled())
            return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                NOTIFICATION_CHANNEL)
                .setContentTitle(mContext.getString(R.string.dcc_summary_notification_title))
                .setContentText(mContext.getString(R.string.dcc_summary_notification_text,
                        mDisplayedNotificationIds.size()))
                .setSmallIcon(R.drawable.ic_notification_connected)
                .setGroup(NOTIFICATION_GROUP_DCC)
                .setGroupSummary(true);
        mNotificationManager.notify(DCC_SUMMARY_NOTIFICATION_ID, builder.build());
    }

    private synchronized void cancelNotification(Integer notificationId) {
        if (mDisplayedNotificationIds.remove(notificationId)) {
            if (mDisplayedNotificationIds.size() == 0)
                mNotificationManager.cancel(DCC_SUMMARY_NOTIFICATION_ID);
            mNotificationManager.cancel(notificationId);
        }
    }

    private synchronized void createUploadNotification(DCCServerManager.UploadEntry uploadEntry) {
        Integer id = mUploadNotificationIds.get(uploadEntry.getServer());
        if (id == null)
            return;
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                NOTIFICATION_CHANNEL)
                .setContentTitle(uploadEntry.getFileName())
                .setContentText(mContext.getString(R.string.dcc_active_waiting_for_connection))
                .setSmallIcon(R.drawable.ic_notification_upload)
                .setOngoing(true);
        if (isNotificationGroupingEnabled())
            builder.setGroup(NOTIFICATION_GROUP_DCC);
        mDisplayedNotificationIds.add(id);
        mNotificationManager.notify(id, builder.build());
        createSummaryNotification();
    }

}
