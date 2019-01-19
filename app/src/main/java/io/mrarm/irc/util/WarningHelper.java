package io.mrarm.irc.util;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;

public class WarningHelper {

    public static final int WARNING_NOTIFICATION_ID_RANGE_START = 200;
    public static final int WARNING_NOTIFICATION_ID_RANGE_END = 300;

    private static final String NOTIFICATION_CHANNEL_NAME = "warning";

    private static boolean sNotificationChannelCreated = false;

    private static int sNextNotificationId = WARNING_NOTIFICATION_ID_RANGE_START;

    public static int getNotificationId() {
        int ret = sNextNotificationId++;
        if (ret >= WARNING_NOTIFICATION_ID_RANGE_END) {
            sNextNotificationId = WARNING_NOTIFICATION_ID_RANGE_START;
            ret = sNextNotificationId++;
        }
        return ret;
    }

    private static Context mAppContext;
    private static Activity mActivity;

    private static final List<Warning> mWarnings = new ArrayList<>();

    public static void setAppContext(Context context) {
        mAppContext = context;
    }

    public static void setActivity(Activity activity) {
        Activity oldActivity = mActivity;
        mActivity = activity;
        synchronized (mWarnings) {
            for (Warning warning : mWarnings) {
                if (activity != null) {
                    warning.showDialog(activity);
                    warning.dismissNotification(mAppContext);
                } else {
                    warning.showNotification(mAppContext);
                    if (oldActivity != null)
                        warning.dismissDialog(activity);
                }
            }
        }
    }

    public static Activity getActivity() {
        return mActivity;
    }

    public static void showWarning(Warning warning) {
        synchronized (mWarnings) {
            synchronized (warning) {
                if (warning.mDismissed)
                    return;
            }
            mWarnings.add(warning);
            if (mActivity != null) {
                mActivity.runOnUiThread(() -> {
                    warning.showDialog(mActivity);
                });
            } else {
                warning.showNotification(mAppContext);
            }
        }
    }

    private static void dismissWarning(Warning warning) {
        synchronized (mWarnings) {
            mWarnings.remove(warning);
        }
    }

    public static String getNotificationChannel(Context context) {
        if (!sNotificationChannelCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_NAME,
                    context.getString(R.string.notification_channel_warning),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setGroup(
                    io.mrarm.irc.NotificationManager.getSystemNotificationChannelGroup(context));
            notificationManager.createNotificationChannel(channel);
            sNotificationChannelCreated = true;
        }
        return NOTIFICATION_CHANNEL_NAME;
    }

    public static class Warning {

        private boolean mDismissed = false;
        private int mNotificationId = -1;

        public Warning() {
            //
        }

        public void dismiss() {
            synchronized (this) {
                mDismissed = true;
                dismissWarning(this);
            }
        }

        public void showNotification(Context context) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(mAppContext,
                    getNotificationChannel(context));
            if (mNotificationId == -1)
                mNotificationId = getNotificationId();
            buildNotification(context, notification, mNotificationId);
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(mNotificationId, notification.build());
        }

        protected void buildNotification(Context context, NotificationCompat.Builder notification,
                                         int notificationId) {
            notification.setSmallIcon(R.drawable.ic_warning);
            notification.setPriority(NotificationCompat.PRIORITY_HIGH);
            notification.setContentTitle(context.getString(R.string.notification_action_required));
        }

        public void dismissNotification(Context context) {
            if (mNotificationId == -1)
                return;
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(mNotificationId);
            mNotificationId = -1;
        }

        public void showDialog(Activity activity) {
        }

        public void dismissDialog(Activity activity) {
        }

    }


}
