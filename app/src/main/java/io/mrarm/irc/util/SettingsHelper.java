package io.mrarm.irc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

import io.mrarm.irc.ServerConfigManager;

public class SettingsHelper {

    private static SettingsHelper mInstance;

    public static String PREF_DEFAULT_NICK = "default_nick";
    public static String PREF_DEFAULT_USER = "default_user";
    public static String PREF_DEFAULT_REALNAME = "default_realname";
    public static String PREF_RECONNECT_ENABLED = "reconnect_enabled";
    public static String PREF_RECONNECT_CONNCHG = "reconnect_connchg";
    public static String PREF_RECONNECT_INTERVAL = "reconnect_interval";

    public static SettingsHelper getInstance(Context context) {
        if (mInstance == null)
            mInstance = new SettingsHelper(context.getApplicationContext());
        return mInstance;
    }

    private SharedPreferences mPreferences;
    private List<ReconnectIntervalPreference.Rule> mCachedIntervalRules;

    public SettingsHelper(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(PREF_RECONNECT_INTERVAL))
                    mCachedIntervalRules = null;
            }
        });
    }

    public String getDefaultNick() {
        return mPreferences.getString(PREF_DEFAULT_NICK, null);
    }

    public String getDefaultUser() {
        return mPreferences.getString(PREF_DEFAULT_USER, null);
    }

    public String getDefaultRealname() {
        return mPreferences.getString(PREF_DEFAULT_REALNAME, null);
    }

    public boolean isReconnectEnabled() {
        return mPreferences.getBoolean(PREF_RECONNECT_ENABLED, true);
    }

    public boolean shouldReconnectOnConnectivityChange() {
        return mPreferences.getBoolean(PREF_RECONNECT_CONNCHG, true);
    }

    public List<ReconnectIntervalPreference.Rule> getReconnectIntervalRules() {
        if (mCachedIntervalRules == null) {
            mCachedIntervalRules = ReconnectIntervalPreference.getDefaultValue();
            try {
                List<ReconnectIntervalPreference.Rule> rules = ServerConfigManager.getGson().fromJson(mPreferences.getString(PREF_RECONNECT_INTERVAL, null), ReconnectIntervalPreference.mListRuleType);
                if (rules != null)
                    mCachedIntervalRules = rules;
            } catch (Exception ignored) {
            }
        }
        return mCachedIntervalRules;
    }

}
