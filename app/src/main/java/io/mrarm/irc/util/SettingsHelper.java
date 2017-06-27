package io.mrarm.irc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SettingsHelper {

    public static String PREF_DEFAULT_NICK = "default_nick";
    public static String PREF_DEFAULT_USER = "default_user";
    public static String PREF_DEFAULT_REALNAME = "default_realname";
    public static String PREF_RECONNECT_ENABLED = "reconnect_enabled";
    public static String PREF_RECONNECT_CONNCHG = "reconnect_connchg";
    public static String PREF_RECONNECT_INTERVAL = "reconnect_interval";
    public static String PREF_AUTOCONNECT_SERVERS = "connect_servers";

    private static SettingsHelper mInstance;

    private static Gson mGson = new Gson();

    public static SettingsHelper getInstance(Context context) {
        if (mInstance == null)
            mInstance = new SettingsHelper(context.getApplicationContext());
        return mInstance;
    }

    public static Gson getGson() {
        return mGson;
    }

    private SharedPreferences mPreferences;
    private List<ReconnectIntervalPreference.Rule> mCachedIntervalRules;

    public SettingsHelper(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener((SharedPreferences sharedPreferences, String key) -> {
            if (key.equals(PREF_RECONNECT_INTERVAL))
                mCachedIntervalRules = null;
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
                List<ReconnectIntervalPreference.Rule> rules = getGson().fromJson(mPreferences.getString(PREF_RECONNECT_INTERVAL, null), ReconnectIntervalPreference.sListRuleType);
                if (rules != null)
                    mCachedIntervalRules = rules;
            } catch (Exception ignored) {
            }
        }
        return mCachedIntervalRules;
    }

    public List<UUID> getAutoConnectServerList() {
        Set<String> set = mPreferences.getStringSet(PREF_AUTOCONNECT_SERVERS, null);
        if (set == null)
            return null;
        List<UUID> ret = new ArrayList<>();
        for (String s : set)
            ret.add(UUID.fromString(s));
        return ret;
    }

    public void setAutoConnectServerList(List<UUID> list) {
        Set<String> set = new HashSet<>();
        for (UUID uuid : list)
            set.add(uuid.toString());
        mPreferences.edit()
                .putStringSet(PREF_AUTOCONNECT_SERVERS, set)
                .apply();
    }

}
