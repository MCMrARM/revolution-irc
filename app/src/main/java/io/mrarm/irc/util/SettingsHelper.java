package io.mrarm.irc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SettingsHelper {

    public static final String PREF_DEFAULT_NICK = "default_nick";
    public static final String PREF_DEFAULT_USER = "default_user";
    public static final String PREF_DEFAULT_REALNAME = "default_realname";
    public static final String PREF_RECONNECT_ENABLED = "reconnect_enabled";
    public static final String PREF_RECONNECT_CONNCHG = "reconnect_connchg";
    public static final String PREF_RECONNECT_INTERVAL = "reconnect_interval";
    public static final String PREF_AUTOCONNECT_SERVERS = "connect_servers";
    public static final String PREF_DARK_THEME = "dark_theme";
    public static final String PREF_CHAT_FONT = "chat_font";
    public static final String PREF_CHAT_FONT_SIZE = "chat_font_size";
    public static final String PREF_CHAT_APPBAR_COMPACT_MODE = "chat_appbar_compact_mode";

    public static final String COMPACT_MODE_ALWAYS = "always";
    public static final String COMPACT_MODE_NEVER = "never";
    public static final String COMPACT_MODE_AUTO = "auto";

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

    private Context mContext;
    private SharedPreferences mPreferences;
    private List<ReconnectIntervalPreference.Rule> mCachedIntervalRules;
    private Typeface mCachedFont;

    public SettingsHelper(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener((SharedPreferences sharedPreferences, String key) -> {
            if (key.equals(PREF_RECONNECT_INTERVAL))
                mCachedIntervalRules = null;
            if (key.equals(PREF_CHAT_FONT))
                mCachedFont = null;
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

    public boolean isNightModeEnabled() {
        return mPreferences.getBoolean(PREF_DARK_THEME, false);
    }

    public Typeface getChatFont() {
        if (mCachedFont != null)
            return mCachedFont;
        String font = mPreferences.getString(PREF_CHAT_FONT, "default");
        if (ListWithCustomPreference.isCustomValue(font)) {
            File file = ListWithCustomPreference.getCustomFile(mContext, PREF_CHAT_FONT, font);
            try {
                mCachedFont = Typeface.createFromFile(file);
                return mCachedFont;
            } catch (Exception ignored) {
            }
        }
        if (font.equals("monospace"))
            return Typeface.MONOSPACE;
        else if (font.equals("serif"))
            return Typeface.SERIF;
        else
            return Typeface.DEFAULT;
    }

    public int getChatFontSize() {
        return mPreferences.getInt(PREF_CHAT_FONT_SIZE, -1);
    }

    public String getChatAppbarCompactMode() {
        return mPreferences.getString(PREF_CHAT_APPBAR_COMPACT_MODE, COMPACT_MODE_AUTO);
    }

}
