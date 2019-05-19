package io.mrarm.irc;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.dagger.AppComponent;
import io.mrarm.irc.dagger.DaggerAppComponent;

public class IRCApplication extends Application implements Application.ActivityLifecycleCallbacks,
        HasActivityInjector {

    @Inject
    DispatchingAndroidInjector<Activity> mDispatchingActivityInjector;

    private List<Activity> mActivities = new ArrayList<>();
    private List<PreExitCallback> mPreExitCallbacks = new ArrayList<>();
    private List<ExitCallback> mExitCallbacks = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        AppComponent appComponent = DaggerAppComponent.factory().create(this);
        appComponent.inject(this);
        appComponent.createEagerSingletons();
        SettingsHelper.getInstance(this);
        NotificationManager.createDefaultChannels(this);
        registerActivityLifecycleCallbacks(this);
    }

    public void addPreExitCallback(PreExitCallback c) {
        mPreExitCallbacks.add(c);
    }

    public void removePreExitCallback(PreExitCallback c) {
        mPreExitCallbacks.remove(c);
    }

    public void addExitCallback(ExitCallback c) {
        mExitCallbacks.add(c);
    }

    public void removeExitCallback(ExitCallback c) {
        mExitCallbacks.remove(c);
    }

    public boolean requestExit() {
        for (PreExitCallback exitCallback : mPreExitCallbacks) {
            if (!exitCallback.onAppPreExit())
                return false;
        }
        for (ExitCallback exitCallback : mExitCallbacks)
            exitCallback.onAppExiting();
        for (Activity activity : mActivities)
            activity.finish();
        ServerConnectionManager.destroyInstance();
        IRCService.stop(this);
        return true;
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mActivities.add(activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        mActivities.remove(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return mDispatchingActivityInjector;
    }


    public interface PreExitCallback {
        boolean onAppPreExit();
    }


    public interface ExitCallback {
        void onAppExiting();
    }

}
