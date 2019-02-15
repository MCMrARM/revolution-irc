package io.mrarm.irc.job;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Log;

import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.config.UiSettingChangeCallback;

public class ServerPingScheduler {

    private static final String TAG = "ServerPingScheduler";

    private static final int INTENT_ID = 300;
    public static final int JOB_ID = 1;

    private static ServerPingScheduler instance;

    public static ServerPingScheduler getInstance(Context ctx) {
        if (instance == null)
            instance = new ServerPingScheduler(ctx.getApplicationContext());
        return instance;
    }


    private Context context;
    private boolean running;
    private boolean enabled = false;
    private long interval = 15 * 60 * 1000; // 15 minutes
    private boolean onlyOnWifi = true;

    public ServerPingScheduler(Context ctx) {
        this.context = ctx;

        SettingsHelper.registerCallbacks(this);
        onSettingChanged();
    }

    @UiSettingChangeCallback(keys = {AppSettings.PREF_PING_ENABLED,
            AppSettings.PREF_PING_WI_FI_ONLY, AppSettings.PREF_PING_INTERVAL})
    private void onSettingChanged() {
        enabled = AppSettings.isPingEnabled();
        onlyOnWifi = AppSettings.isPingWiFiOnly();
        interval = AppSettings.getPingInterval();
        stop();
        startIfEnabled();
    }

    void onJobRan() {
        if (!running) {
            // The job should not be running
            Log.w(TAG, "A job that should have not ran has been started; forcibly stopping");
            forceStop();
        }
    }

    public void startIfEnabled() {
        if (!enabled)
            return;

        if (isUsingNetworkStateAwareApi() || !onlyOnWifi) {
            start();
        } else {
            onWifiStateChanged(ServerConnectionManager.isWifiConnected(context));
        }
    }

    public void onWifiStateChanged(boolean connectedToWifi) {
        if (!isUsingNetworkStateAwareApi() && onlyOnWifi)
            return;
        if (connectedToWifi)
            start();
        else
            stop();
    }

    public boolean isUsingJobService() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

    public boolean isUsingNetworkStateAwareApi() {
        return isUsingJobService();
    }

    @SuppressLint("NewApi")
    private void start() {
        if (running)
            return;
        Log.d(TAG, "Starting the job (with job service = " + isUsingJobService() + ")");
        running = true;
        if (isUsingJobService())
            startUsingJobService();
        else
            startUsingAlarmManager();
    }

    public void stop() {
        if (!running)
            return;
        Log.d(TAG, "Stopping the job (with job service = " + isUsingJobService() + ")");
        forceStop();
    }

    @SuppressLint("NewApi")
    public void forceStop() {
        running = false;
        if (isUsingJobService())
            stopUsingJobService();
        else
            stopUsingAlarmManager();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startUsingJobService() {
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, ServerPingJobService.class))
                .setPeriodic(interval)
                .setRequiredNetworkType(onlyOnWifi ? JobInfo.NETWORK_TYPE_UNMETERED
                        : JobInfo.NETWORK_TYPE_ANY)
                .build();
        JobScheduler scheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        assert scheduler != null;
        scheduler.schedule(jobInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopUsingJobService() {
        JobScheduler scheduler = (JobScheduler)
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        assert scheduler != null;
        scheduler.cancel(JOB_ID);
    }

    private PendingIntent getAlarmManagerIntent() {
        Intent intent = new Intent(context, ServerPingBroadcastReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, INTENT_ID, intent, 0);
        return pi;
    }

    private void startUsingAlarmManager() {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        assert am != null;

        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis(),
                interval, getAlarmManagerIntent());
    }

    private void stopUsingAlarmManager() {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        assert am != null;
        am.cancel(getAlarmManagerIntent());
    }

}
