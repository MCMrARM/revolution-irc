package io.mrarm.irc.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mrarm.irc.setting.ListWithCustomSetting;
import io.mrarm.irc.setting.ReconnectIntervalSetting;

public class SettingsHelper implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_DEFAULT_NICKS = "default_nicks";
    public static final String PREF_DEFAULT_USER = "default_user";
    public static final String PREF_DEFAULT_REALNAME = "default_realname";
    public static final String PREF_DEFAULT_QUIT_MESSAGE = "default_quit_message";
    public static final String PREF_DEFAULT_PART_MESSAGE = "default_part_message";
    public static final String PREF_RECONNECT_ENABLED = "reconnect_enabled";
    public static final String PREF_RECONNECT_CONNCHG = "reconnect_connchg";
    public static final String PREF_RECONNECT_WIFI = "reconnect_wifi";
    public static final String PREF_RECONNECT_INTERVAL = "reconnect_interval";
    public static final String PREF_RECONNECT_REJOIN_CHANNELS = "reconnect_rejoin_channels";
    public static final String PREF_PING_ENABLED = "ping_enabled";
    public static final String PREF_PING_WIFI = "ping_wifi";
    public static final String PREF_PING_INTERVAL  = "ping_interval";
    public static final String PREF_DRAWER_PINNED = "drawer_pinned";
    public static final String PREF_DRAWER_ALWAYS_SHOW_SERVER = "drawer_always_show_server";
    public static final String PREF_THEME = "theme";
    public static final String PREF_CHAT_FONT = "chat_font";
    public static final String PREF_CHAT_FONT_SIZE = "chat_font_size";
    public static final String PREF_CHAT_TEXT_AUTOCORRECT = "chat_text_autocorrect";
    public static final String PREF_CHAT_HIDE_JOIN_PART = "chat_hide_join_part";
    public static final String PREF_CHAT_APPBAR_COMPACT_MODE = "chat_appbar_compact_mode";
    public static final String PREF_CHAT_SEND_BOX_HISTORY_SWIPE_MODE = "chat_send_box_history_swipe_mode";
    public static final String PREF_CHAT_SEND_BOX_ALWAYS_MULTILINE = "chat_send_box_always_multiline";
    public static final String PREF_CHAT_SHOW_DCC_SEND = "chat_show_dcc_send";
    public static final String PREF_CHAT_MULTI_SELECT_MODE = "chat_show_multi_select_mode";
    public static final String PREF_MESSAGE_FORMAT = "message_format";
    public static final String PREF_MESSAGE_FORMAT_MENTION = "message_format_mention";
    public static final String PREF_MESSAGE_FORMAT_ACTION = "message_format_action";
    public static final String PREF_MESSAGE_FORMAT_ACTION_MENTION = "message_format_action_mention";
    public static final String PREF_MESSAGE_FORMAT_NOTICE = "message_format_notice";
    public static final String PREF_MESSAGE_FORMAT_EVENT = "message_format_event";
    public static final String PREF_MESSAGE_FORMAT_EVENT_HOSTNAME = "message_format_event_hostname";
    public static final String PREF_MESSAGE_TIME_FORMAT = "message_time_format";
    public static final String PREF_MESSAGE_TIME_FIXED_WIDTH = "message_time_fixed_width";
    public static final String PREF_NICK_AUTOCOMPLETE_SHOW_BUTTON = "nick_autocomplete_show_button";
    public static final String PREF_NICK_AUTOCOMPLETE_DOUBLE_TAP = "nick_autocomplete_double_tap";
    public static final String PREF_NICK_AUTOCOMPLETE_SUGGESTIONS = "nick_autocomplete_suggestions";
    public static final String PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS = "nick_autocomplete_at_suggestions";
    public static final String PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS_REMOVE_AT = "nick_autocomplete_at_suggestions_remove_at";
    public static final String PREF_CHANNEL_AUTOCOMPLETE_SUGGESTIONS = "channel_autocomplete_suggestions";
    public static final String PREF_STORAGE_LIMIT_GLOBAL = "storage_limit_global";
    public static final String PREF_STORAGE_LIMIT_SERVER = "storage_limit_server";

    public static final String COMPACT_MODE_ALWAYS = "always";
    public static final String COMPACT_MODE_NEVER = "never";
    public static final String COMPACT_MODE_AUTO = "auto";

    public static final String SWIPE_DISABLED = "disabled";
    public static final String SWIPE_LEFT_TO_RIGHT = "left_to_right";
    public static final String SWIPE_RIGHT_TO_LEFT = "right_to_left";
    public static final String SWIPE_UP_TO_DOWN = "up_to_down";
    public static final String SWIPE_DOWN_TO_UP = "down_to_up";

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

    public static void deleteSQLiteDatabase(File path) {
        path.delete();
        new File(path.getParent(), path.getName() + "-journal").delete();
        new File(path.getParent(), path.getName() + "-shm").delete();
        new File(path.getParent(), path.getName() + "-wal").delete();
    }

    private Context mContext;
    private SharedPreferences mPreferences;
    private Map<String, List<SharedPreferences.OnSharedPreferenceChangeListener>> mListeners = new HashMap<>();
    private Typeface mCachedFont;

    private static final Map<String, List<SettingChangeCallback>> sListeners = new HashMap<>();

    public SettingsHelper(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public static SharedPreferences getPreferences() {
        return SettingsHelper.getInstance(null).mPreferences;
    }

    public static Context getContext() {
        return SettingsHelper.getInstance(null).mContext;
    }

    public List<File> getCustomFiles() {
        List<File> ret = new ArrayList<>();
        String font = mPreferences.getString(PREF_CHAT_FONT, "default");
        if (ListWithCustomSetting.isPrefCustomValue(font))
            ret.add(ListWithCustomSetting.getCustomFile(mContext, PREF_CHAT_FONT, font));
        return ret;
    }

    public void clear() {
        for (File file : getCustomFiles())
            file.delete();
        mPreferences.edit().clear().commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREF_CHAT_FONT))
            mCachedFont = null;
        if (mListeners.containsKey(key)) {
            for (SharedPreferences.OnSharedPreferenceChangeListener l : mListeners.get(key))
                l.onSharedPreferenceChanged(sharedPreferences, key);
        }
        if (sListeners.containsKey(key)) {
            for (SettingChangeCallback l : sListeners.get(key))
                l.onSettingChanged(key);
        }
    }

    public SharedPreferences.OnSharedPreferenceChangeListener addPreferenceChangeListener(
            String key, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (!mListeners.containsKey(key))
            mListeners.put(key, new ArrayList<>());
        mListeners.get(key).add(listener);
        return listener;
    }

    public void removePreferenceChangeListener(
            String key, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        if (listener != null && mListeners.containsKey(key))
            mListeners.get(key).remove(listener);
    }

    public void setDrawerPinned(boolean pinned) {
        mPreferences.edit()
                .putBoolean(PREF_DRAWER_PINNED, pinned)
                .apply();
    }

    public Typeface getChatFont() {
        if (mCachedFont != null)
            return mCachedFont;
        String font = mPreferences.getString(PREF_CHAT_FONT, "default");
        if (ListWithCustomSetting.isPrefCustomValue(font)) {
            File file = ListWithCustomSetting.getCustomFile(mContext, PREF_CHAT_FONT, font);
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

    public void setTheme(String name) {
        mPreferences.edit()
                .putString(PREF_THEME, name)
                .apply();
    }


    static long getLong(SharedPreferences prefs, String key, long def) {
        try {
            return prefs.getLong(key, def);
        } catch (Exception e) {
            // We most likely got a ClassCastException and this situation happened after restoring
            // from a backup, as in JSON we have no idea of differentiating longs from ints.
            return prefs.contains(key) ? prefs.getInt(key, 0) : def;
        }
    }


    public static ListenerHandle changeEvent() {
        return new ListenerHandle();
    }


    public interface SettingChangeCallback {
        void onSettingChanged(String name);
    }

    public static class ListenerHandle {

        private final Map<String, List<SettingChangeCallback>> mCallbacks = new HashMap<>();

        private ListenerHandle() {
        }

        public ListenerHandle cancel() {
            synchronized (sListeners) {
                for (Map.Entry<String, List<SettingChangeCallback>> e : mCallbacks.entrySet()) {
                    List<SettingChangeCallback> globalList = sListeners.get(e.getKey());
                    if (globalList == null)
                        continue;
                    globalList.removeAll(e.getValue());
                }
            }
            mCallbacks.clear();
            return this;
        }

        public ListenerHandle listen(String property, SettingChangeCallback cb) {
            synchronized (sListeners) {
                List<SettingChangeCallback> globalList = sListeners.get(property);
                if (globalList == null) {
                    globalList = new ArrayList<>();
                    sListeners.put(property, globalList);
                }
                globalList.add(cb);
            }
            List<SettingChangeCallback> localList = sListeners.get(property);
            if (localList == null) {
                localList = new ArrayList<>();
                sListeners.put(property, localList);
            }
            localList.add(cb);
            return this;
        }

        public ListenerHandle listen(String property, Runnable cb) {
            listen(property, (c) -> cb.run());
            return this;
        }

    }

}
