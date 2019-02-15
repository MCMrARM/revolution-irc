package io.mrarm.irc.config;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.mrarm.irc.setting.ListWithCustomSetting;

public class SettingsHelper implements SharedPreferences.OnSharedPreferenceChangeListener {

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
        String font = mPreferences.getString(ChatSettings.PREF_FONT, "default");
        if (ListWithCustomSetting.isPrefCustomValue(font))
            ret.add(ListWithCustomSetting.getCustomFile(mContext, ChatSettings.PREF_FONT, font));
        return ret;
    }

    public void clear() {
        for (File file : getCustomFiles())
            file.delete();
        mPreferences.edit().clear().commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (sListeners.containsKey(key)) {
            for (SettingChangeCallback l : sListeners.get(key))
                l.onSettingChanged(key);
        }
    }

    public static Object getDefaultValue(String key) {
        Object o = AppSettings.getDefaultValue(key);
        if (o != null)
            return o;
        o = ChatSettings.getDefaultValue(key);
        if (o != null)
            return o;
        o = NickAutocompleteSettings.getDefaultValue(key);
        if (o != null)
            return o;
        return null;
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

        public ListenerHandle listen(String[] properties, SettingChangeCallback cb) {
            for (String property : properties)
                listen(property, cb);
            return this;
        }

        public ListenerHandle listen(String[] properties, Runnable cb) {
            listen(properties, (c) -> cb.run());
            return this;
        }

    }


    private static final HashMap<Class<?>, ClassCallbackInfo> sCallbacks = new HashMap<>();
    private static final WeakHashMap<Object, WeakReference<ListenerHandle>> sRegisteredListeners = new WeakHashMap<>();
    private static final Handler sUiHandler = new Handler(Looper.getMainLooper());

    private static synchronized ClassCallbackInfo resolveCallbacks(Class<?> c) {
        ClassCallbackInfo i = sCallbacks.get(c);
        if (i != null)
            return i;
        i = new ClassCallbackInfo();
        sCallbacks.put(c, i);
        for (Method m : c.getDeclaredMethods()) {
            m.setAccessible(true);
            io.mrarm.irc.config.SettingChangeCallback a =
                    m.getAnnotation(io.mrarm.irc.config.SettingChangeCallback.class);
            if (a != null)
                i.methods.add(new ClassCallbackInfo.MethodInfo(m, a.keys(), false));

            io.mrarm.irc.config.UiSettingChangeCallback a2 =
                    m.getAnnotation(io.mrarm.irc.config.UiSettingChangeCallback.class);
            if (a2 != null)
                i.methods.add(new ClassCallbackInfo.MethodInfo(m, a2.keys(), true));
        }
        return i;
    }

    public static synchronized void registerCallbacks(Object o) {
        if (sRegisteredListeners.containsKey(o))
            return;
        ClassCallbackInfo c = resolveCallbacks(o.getClass());
        ListenerHandle handle = changeEvent();
        for (ClassCallbackInfo.MethodInfo m : c.methods) {
            SettingChangeCallback cb = m.createCallback(o);
            for (String k : m.keys)
                handle.listen(k, cb);
        }
        sRegisteredListeners.put(o, new WeakReference<>(handle));
    }

    public static synchronized void unregisterCallbacks(Object o) {
        WeakReference<ListenerHandle> h = sRegisteredListeners.remove(o);
        if (h != null) {
            ListenerHandle hh = h.get();
            if (hh != null)
                hh.cancel();
        }
    }

    private static class ClassCallbackInfo {

        private static class MethodInfo {
            private Method method;
            private String[] keys;
            private boolean onUi;

            private MethodInfo(Method method, String[] keys, boolean onUi) {
                this.method = method;
                this.keys = keys;
                this.onUi = onUi;
            }

            private SettingChangeCallback createBaseCallback(Object o) {
                if (this.method.getParameterTypes().length > 0) {
                    return (n) -> {
                        try {
                            this.method.invoke(o, n);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };
                } else {
                    return (n) -> {
                        try {
                            this.method.invoke(o);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
            }

            private SettingChangeCallback createCallback(Object o) {
                if (onUi) {
                    SettingChangeCallback base = createBaseCallback(o);
                    return (n) -> {
                        if (Looper.getMainLooper().getThread() == Thread.currentThread())
                            base.onSettingChanged(n);
                        else
                            sUiHandler.post(() -> base.onSettingChanged(n));
                    };
                } else {
                    return createBaseCallback(o);
                }
            }
        }

        private List<MethodInfo> methods = new ArrayList<>();

    }

}
