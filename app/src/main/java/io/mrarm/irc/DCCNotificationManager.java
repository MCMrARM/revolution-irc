package io.mrarm.irc;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mrarm.chatlib.irc.dcc.DCCClient;
import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;
import io.mrarm.irc.util.FormatUtils;

public class DCCNotificationManager implements DCCServerManager.UploadListener,
        DCCManager.DownloadListener {

    private static final String NOTIFICATION_CHANNEL = "DCCTransfers";
    public static final String NOTIFICATION_GROUP_DCC = "dcc";

    public static final int DCC_SUMMARY_NOTIFICATION_ID = 102;
    public static final int DCC_NOTIFICATION_ID_START = 30000000;
    public static final int DCC_SECOND_INTENT_ID_START = 40000000;

    private Context mContext;
    private NotificationManager mNotificationManager;
    private int mNextNotificationId = DCC_NOTIFICATION_ID_START;
    private final Map<DCCServer, List<DCCServer.UploadSession>> mUploadSessions = new HashMap<>();
    private final Map<DCCServer, Integer> mUploadNotificationIds = new HashMap<>();
    private final Map<DCCServer.UploadSession, Integer> mSessionNotificationIds = new HashMap<>();
    private final Map<DCCManager.DownloadInfo, Integer> mDownloadNotificationIds = new HashMap<>();
    private final Map<Integer, Object> mDisplayedNotificationIds = new HashMap<>();
    private boolean mNotificationUpdateQueued = false;
    private final Runnable mNotificationUpdateRunnable = this::updateNotifications;
    private PendingIntent mOpenTransfersIntent;

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
        if (list != null) {
            for (DCCServer.UploadSession session : list)
                onSessionDestroyed(uploadEntry.getServer(), session);
        }
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
            if (shouldCreateUploadNot) {
                DCCServerManager.UploadEntry entry = DCCManager.getInstance(mContext)
                        .getUploadEntry(dccServer);
                if (entry != null)
                    createUploadNotification(entry);
            }
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

    private boolean shouldUpdateNotifications() {
        if (DCCManager.getInstance(mContext).hasAnyActiveDownloads())
            return true;
        synchronized (this) {
            for (List<DCCServer.UploadSession> sessionList : mUploadSessions.values()) {
                if (sessionList.size() > 0)
                    return true;
            }
            return false;
        }
    }

    private void updateNotifications() {
        mNotificationUpdateQueued = false;
        if (!shouldUpdateNotifications())
            return;
        synchronized (this) {
            for (List<DCCServer.UploadSession> sessionList : mUploadSessions.values()) {
                for (DCCServer.UploadSession session : sessionList)
                    createUploadNotification(session);
            }
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

    private PendingIntent getOpenTransfersIntent() {
        if (mOpenTransfersIntent == null) {
            Intent intent = new Intent(mContext, DCCActivity.class);
            mOpenTransfersIntent = PendingIntent.getActivity(mContext, DCC_SUMMARY_NOTIFICATION_ID,
                    intent, 0);
        }
        return mOpenTransfersIntent;
    }

    private NotificationCompat.Action createCancelAction(int notId) {
        int cancelIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                R.drawable.ic_close : R.drawable.ic_notification_close;
        return new NotificationCompat.Action.Builder(cancelIcon,
                mContext.getString(R.string.action_cancel),
                PendingIntent.getBroadcast(mContext, notId,
                        ActionReceiver.getCancelIntent(mContext, notId),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
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
                .setContentText(mContext.getResources().getQuantityString(
                        R.plurals.dcc_summary_notification_text,
                        mDisplayedNotificationIds.size(), mDisplayedNotificationIds.size()))
                .setContentIntent(getOpenTransfersIntent())
                .setSmallIcon(R.drawable.ic_notification_connected)
                .setGroup(NOTIFICATION_GROUP_DCC)
                .setGroupSummary(true)
                .setOnlyAlertOnce(true);
        mNotificationManager.notify(DCC_SUMMARY_NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification(Integer notificationId) {
        if (mDisplayedNotificationIds.remove(notificationId) != null) {
            if (mDisplayedNotificationIds.size() == 0)
                mNotificationManager.cancel(DCC_SUMMARY_NOTIFICATION_ID);
            mNotificationManager.cancel(notificationId);
        }
        if (mNotificationUpdateQueued && !shouldUpdateNotifications())
            cancelNotificationUpdate();
    }

    private void createUploadNotification(DCCServerManager.UploadEntry uploadEntry) {
        Integer id;
        synchronized (this) {
            id = mUploadNotificationIds.get(uploadEntry.getServer());
        }
        if (id == null)
            return;
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                NOTIFICATION_CHANNEL)
                .setContentTitle(uploadEntry.getFileName())
                .setContentText(mContext.getString(R.string.dcc_active_waiting_for_connection))
                .setContentIntent(getOpenTransfersIntent())
                .setSmallIcon(R.drawable.ic_notification_upload)
                .setOngoing(true)
                .addAction(createCancelAction(id));
        if (isNotificationGroupingEnabled())
            builder.setGroup(NOTIFICATION_GROUP_DCC);
        mDisplayedNotificationIds.put(id, uploadEntry);
        mNotificationManager.notify(id, builder.build());
        createSummaryNotification();
    }

    private void createUploadNotification(DCCServer.UploadSession uploadSession) {
        Integer id;
        synchronized (this) {
            id = mSessionNotificationIds.get(uploadSession);
        }
        if (id == null)
            return;
        DCCServerManager.UploadEntry entry = DCCManager.getInstance(mContext)
                .getUploadEntry(uploadSession.getServer());
        if (entry == null)
            return;
        createNotificationChannel();
        int unit = FormatUtils.getByteFormatUnit(uploadSession.getTotalSize());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                NOTIFICATION_CHANNEL)
                .setContentTitle(entry.getFileName())
                .setProgress(100000, (int) (uploadSession.getAcknowledgedSize() * 100000L /
                        uploadSession.getTotalSize()), false)
                .setContentText(
                        FormatUtils.formatByteSize(uploadSession.getAcknowledgedSize(), unit) +

                        "/" + FormatUtils.formatByteSize(uploadSession.getTotalSize(), unit))
                .setContentIntent(getOpenTransfersIntent())
                .setSmallIcon(R.drawable.ic_notification_upload)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(createCancelAction(id));
        if (isNotificationGroupingEnabled())
            builder.setGroup(NOTIFICATION_GROUP_DCC);
        mDisplayedNotificationIds.put(id, uploadSession);
        mNotificationManager.notify(id, builder.build());
        createSummaryNotification();
        postNotificationUpdate();
    }

    private void createDownloadNotification(DCCManager.DownloadInfo download) {
        Integer id;
        synchronized (this) {
            id = mDownloadNotificationIds.get(download);
        }
        if (id == null)
            return;
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext,
                NOTIFICATION_CHANNEL)
                .setContentTitle(download.getUnescapedFileName())
                .setSmallIcon(R.drawable.ic_notification_download)
                .setContentIntent(getOpenTransfersIntent())
                .setOnlyAlertOnce(true)
                .setOngoing(true);
        if (download.isPending()) {
            builder.setContentText(mContext.getString(R.string.dcc_approve_notification_body,
                    download.getSender(), download.getServerName()));
            int acceptIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                    R.drawable.ic_done_white : R.drawable.ic_notification_done;
            builder.addAction(acceptIcon, mContext.getString(R.string.action_accept),
                    PendingIntent.getBroadcast(mContext, id,
                            ActionReceiver.getApproveIntent(mContext, id, true),
                            PendingIntent.FLAG_CANCEL_CURRENT));
            int cancelIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                    R.drawable.ic_close : R.drawable.ic_notification_close;
            builder.addAction(cancelIcon, mContext.getString(R.string.action_reject),
                    PendingIntent.getBroadcast(mContext,
                            DCC_SECOND_INTENT_ID_START - DCC_NOTIFICATION_ID_START + id,
                            ActionReceiver.getApproveIntent(mContext, id, false),
                            PendingIntent.FLAG_CANCEL_CURRENT));
        } else if (download.getClient() != null) {
            DCCClient client = download.getClient();
            int unit = FormatUtils.getByteFormatUnit(client.getExpectedSize());
            builder
                    .setProgress(100000, (int) (client.getDownloadedSize() * 100000L /
                            client.getExpectedSize()), false)
                    .setContentText(FormatUtils.formatByteSize(client.getDownloadedSize(), unit) +
                            "/" + FormatUtils.formatByteSize(client.getExpectedSize(), unit))
                    .addAction(createCancelAction(id));
        } else {
            builder.setContentText(mContext.getString(R.string.dcc_active_waiting_for_connection))
                    .setProgress(0, 0, true)
                    .addAction(createCancelAction(id));
        }
        if (isNotificationGroupingEnabled())
            builder.setGroup(NOTIFICATION_GROUP_DCC);
        mDisplayedNotificationIds.put(id, download);
        mNotificationManager.notify(id, builder.build());
        createSummaryNotification();
        if (!download.isPending())
            postNotificationUpdate();
    }

    public static class ActionReceiver extends BroadcastReceiver {

        private static final String ARG_NOT_ID = "not_id";
        private static final String ARG_TYPE = "type";

        private static final String TYPE_CANCEL = "cancel";
        private static final String TYPE_APPROVE = "approve";
        private static final String TYPE_REJECT = "reject";

        public static Intent getCancelIntent(Context context, int notificationId) {
            Intent intent = new Intent(context, ActionReceiver.class);
            intent.putExtra(ARG_NOT_ID, notificationId);
            intent.putExtra(ARG_TYPE, TYPE_CANCEL);
            return intent;
        }

        public static Intent getApproveIntent(Context context, int notificationId,
                                              boolean approve) {
            Intent intent = new Intent(context, ActionReceiver.class);
            intent.putExtra(ARG_NOT_ID, notificationId);
            intent.putExtra(ARG_TYPE, approve ? TYPE_APPROVE : TYPE_REJECT);
            return intent;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(ARG_TYPE);
            if (type == null)
                return;

            DCCManager dccManager = DCCManager.getInstance(context);
            DCCNotificationManager manager = dccManager.getNotificationManager();
            int notId = intent.getIntExtra(ARG_NOT_ID, -1);
            Object notData = manager.mDisplayedNotificationIds.get(notId);
            if (notData == null)
                return;

            if (type.equals(TYPE_APPROVE) && notData instanceof DCCManager.DownloadInfo) {
                if (dccManager.needsAskSystemDownloadsPermission())
                    context.startActivity(new Intent(context, DCCActivity.class));
                else
                    ((DCCManager.DownloadInfo) notData).approve();
            } else if (type.equals(TYPE_REJECT) && notData instanceof DCCManager.DownloadInfo) {
                ((DCCManager.DownloadInfo) notData).reject();
            } else if (type.equals(TYPE_CANCEL)) {
                if (notData instanceof DCCManager.DownloadInfo)
                    ((DCCManager.DownloadInfo) notData).cancel();
                else if (notData instanceof DCCServer.UploadSession)
                    dccManager.getServer().cancelUpload(dccManager.getUploadEntry(
                            ((DCCServer.UploadSession) notData).getServer()));
                else if (notData instanceof DCCServerManager.UploadEntry)
                    dccManager.getServer().cancelUpload((DCCServerManager.UploadEntry) notData);
            }
        }

    }

}
