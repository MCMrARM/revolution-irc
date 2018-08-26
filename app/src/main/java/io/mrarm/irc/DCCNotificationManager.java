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

import io.mrarm.chatlib.irc.dcc.DCCClient;
import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;
import io.mrarm.irc.util.FormatUtils;

public class DCCNotificationManager implements DCCServerManager.UploadListener,
        DCCManager.DownloadListener {

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
    private final Map<DCCManager.DownloadInfo, Integer> mDownloadNotificationIds = new HashMap<>();
    private boolean mNotificationUpdateQueued = false;
    private final Runnable mNotificationUpdateRunnable = this::updateNotifications;

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
        List<DCCServer.UploadSession> list = mUploadSessions.remove(uploadEntry.getServer());
        Integer nid = mUploadNotificationIds.remove(uploadEntry.getServer());
        mHandler.post(() -> {
            if (nid != null)
                cancelNotification(nid);
        });
        for (DCCServer.UploadSession session : list)
            onSessionDestroyed(uploadEntry.getServer(), session);
    }

    @Override
    public synchronized void onSessionCreated(DCCServer dccServer,
                                              DCCServer.UploadSession uploadSession) {
        mSessionNotificationIds.put(uploadSession, mNextNotificationId++);
        mUploadSessions.get(dccServer).add(uploadSession);
        mHandler.post(() -> {
            Integer nid = mUploadNotificationIds.get(uploadSession.getServer());
            if (nid != null)
                cancelNotification(nid);
            createUploadNotification(uploadSession);
        });
    }

    @Override
    public synchronized void onSessionDestroyed(DCCServer dccServer,
                                                DCCServer.UploadSession uploadSession) {
        Integer nid = mSessionNotificationIds.remove(uploadSession);
        List<DCCServer.UploadSession> list = mUploadSessions.get(dccServer);
        if (list != null)
            list.remove(uploadSession);
        boolean shouldCreateUploadNot = (list != null && list.size() == 0);
        mHandler.post(() -> {
            if (nid != null)
                cancelNotification(nid);
            if (shouldCreateUploadNot)
                createUploadNotification(DCCManager.getInstance(mContext)
                        .getUploadEntry(dccServer));
        });
    }

    @Override
    public synchronized void onDownloadCreated(DCCManager.DownloadInfo download) {
        mDownloadNotificationIds.put(download, mNextNotificationId++);
        mHandler.post(() -> {
            createDownloadNotification(download);
            postNotificationUpdate();
        });
    }

    @Override
    public synchronized void onDownloadDestroyed(DCCManager.DownloadInfo download) {
        Integer nid = mDownloadNotificationIds.remove(download);
        mDownloadNotificationIds.remove(download);
        if (nid != null)
            mHandler.post(() -> cancelNotification(nid));
    }

    @Override
    public void onDownloadUpdated(DCCManager.DownloadInfo download) {
        mHandler.post(() -> createDownloadNotification(download));
    }

    private synchronized boolean shouldUpdateNotifications() {
        if (DCCManager.getInstance(mContext).hasAnyDownloads())
            return true;
        for (List<DCCServer.UploadSession> sessionList : mUploadSessions.values()) {
            if (sessionList.size() > 0)
                return true;
        }
        return false;
    }

    private synchronized void updateNotifications() {
        mNotificationUpdateQueued = false;
        if (!shouldUpdateNotifications())
            return;
        for (List<DCCServer.UploadSession> sessionList : mUploadSessions.values()) {
            for (DCCServer.UploadSession session : sessionList)
                createUploadNotification(session);
        }
        for (DCCManager.DownloadInfo download : DCCManager.getInstance(mContext).getDownloads()) {
            createDownloadNotification(download);
        }
        postNotificationUpdate();
    }

    private void postNotificationUpdate() {
        if (!mNotificationUpdateQueued) {
            mHandler.postDelayed(mNotificationUpdateRunnable, 500L);
            mNotificationUpdateQueued = true;
        }
    }

    private void cancelNotificationUpdate() {
        if (mNotificationUpdateQueued) {
            mHandler.removeCallbacks(mNotificationUpdateRunnable);
            mNotificationUpdateQueued = false;
        }
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
        if (mNotificationUpdateQueued && !shouldUpdateNotifications())
            cancelNotificationUpdate();
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

    private synchronized void createUploadNotification(DCCServer.UploadSession uploadSession) {
        Integer id = mSessionNotificationIds.get(uploadSession);
        if (id == null)
            return;
        createNotificationChannel();
        DCCServerManager.UploadEntry entry = DCCManager.getInstance(mContext)
                .getUploadEntry(uploadSession.getServer());
        int unit = FormatUtils.getByteFormatUnit(uploadSession.getTotalSize());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                NOTIFICATION_CHANNEL)
                .setContentTitle(entry.getFileName())
                .setProgress(100000, (int) (uploadSession.getAcknowledgedSize() * 100000L /
                        uploadSession.getTotalSize()), false)
                .setContentText(
                        FormatUtils.formatByteSize(uploadSession.getAcknowledgedSize(), unit) +
                        "/" + FormatUtils.formatByteSize(uploadSession.getTotalSize(), unit))
                .setSmallIcon(R.drawable.ic_notification_upload)
                .setOngoing(true);
        if (isNotificationGroupingEnabled())
            builder.setGroup(NOTIFICATION_GROUP_DCC);
        mDisplayedNotificationIds.add(id);
        mNotificationManager.notify(id, builder.build());
        createSummaryNotification();
        postNotificationUpdate();
    }

    private synchronized void createDownloadNotification(DCCManager.DownloadInfo download) {
        Integer id = mDownloadNotificationIds.get(download);
        if (id == null)
            return;
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                NOTIFICATION_CHANNEL)
                .setContentTitle(download.getFileName())
                .setSmallIcon(R.drawable.ic_notification_download)
                .setOngoing(true);
        if (download.isPending()) {
            builder.setContentText(mContext.getString(R.string.dcc_active_waiting_for_approval));
            // TODO: add approve and decline buttons straight to the notification
        } else if (download.getClient() != null) {
            DCCClient client = download.getClient();
            int unit = FormatUtils.getByteFormatUnit(client.getExpectedSize());
            builder
                    .setProgress(100000, (int) (client.getDownloadedSize() * 100000L /
                            client.getExpectedSize()), false)
                    .setContentText(FormatUtils.formatByteSize(client.getDownloadedSize(), unit) +
                            "/" + FormatUtils.formatByteSize(client.getExpectedSize(), unit));
        } else {
            builder.setContentText(mContext.getString(R.string.dcc_active_waiting_for_connection))
                    .setProgress(0, 0, true);
        }
        if (isNotificationGroupingEnabled())
            builder.setGroup(NOTIFICATION_GROUP_DCC);
        mDisplayedNotificationIds.add(id);
        mNotificationManager.notify(id, builder.build());
        createSummaryNotification();
        postNotificationUpdate();
    }

}
