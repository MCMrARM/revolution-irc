package io.mrarm.irc.util.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.thememonkey.Theme;

public class ThemeManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String FILENAME_PREFIX = "theme-";
    private static final String FILENAME_SUFFIX = ".json";

    private static final String PREF_THEME_CUSTOM_PREFIX = "custom:";

    private static ThemeManager instance;

    public static ThemeManager getInstance(Context context) {
        if (instance == null)
            instance = new ThemeManager(context.getApplicationContext());
        return instance;
    }


    private Context context;
    private File themesDir;
    private ThemeResInfo currentTheme;
    private ThemeInfo currentCustomTheme;
    private Theme currentCustomThemePatcher;
    private List<ThemeChangeListener> themeChangeListeners = new ArrayList<>();
    private BaseTheme fallbackTheme;
    private Map<String, BaseTheme> baseThemes = new HashMap<>();
    private Map<UUID, ThemeInfo> customThemes = new HashMap<>();

    public ThemeManager(Context context) {
        this.context = context;
        themesDir = new File(context.getFilesDir(), "themes");

        fallbackTheme = new BaseTheme(R.string.value_default,
                R.style.AppTheme, R.style.AppTheme_NoActionBar);
        baseThemes.put("default", fallbackTheme);
        loadThemes();

        SettingsHelper.getInstance(context).addPreferenceChangeListener(
                SettingsHelper.PREF_THEME, this);
        onSharedPreferenceChanged(null, SettingsHelper.PREF_THEME);
    }

    private void loadThemes() {
        File[] themes = themesDir.listFiles();
        if (themes == null)
            return;
        for (File themeFile : themes) {
            String fileName = themeFile.getName();
            if (fileName.startsWith(FILENAME_PREFIX) && fileName.endsWith(FILENAME_SUFFIX)) {
                try {
                    UUID uuid = UUID.fromString(fileName.substring(FILENAME_PREFIX.length(),
                            fileName.length() - FILENAME_SUFFIX.length()));
                    loadTheme(themeFile, uuid);
                } catch (IOException | IllegalArgumentException e) {
                    Log.w("ThemeManager", "Failed to load theme: " + fileName);
                }
            }
        }
    }

    private ThemeInfo loadTheme(File themeFile, UUID uuid) throws IOException {
        ThemeInfo theme = SettingsHelper.getGson().fromJson(
                new BufferedReader(new FileReader(themeFile)), ThemeInfo.class);
        theme.uuid = uuid;
        theme.baseThemeInfo = baseThemes.get(theme.base);
        if (theme.baseThemeInfo == null)
            theme.baseThemeInfo = fallbackTheme;
        customThemes.put(uuid, theme);
        return theme;
    }

    public void saveTheme(ThemeInfo theme) throws IOException {
        if (theme.uuid == null)
            theme.uuid = UUID.randomUUID();
        SettingsHelper.getGson().toJson(theme, new BufferedWriter(new FileWriter(
                new File(themesDir, FILENAME_PREFIX + theme.uuid + FILENAME_SUFFIX))));
    }

    public Map<String, BaseTheme> getBaseThemes() {
        return baseThemes;
    }

    public Collection<ThemeInfo> getCustomThemes() {
        return customThemes.values();
    }

    public void addThemeChangeListener(ThemeChangeListener listener) {
        themeChangeListeners.add(listener);
    }

    public void removeThemeChangeListener(ThemeChangeListener listener) {
        themeChangeListeners.remove(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        currentTheme = null;
        currentCustomTheme = null;
        currentCustomThemePatcher = null;

        String theme = SettingsHelper.getInstance(context).getTheme();
        if (theme != null && theme.startsWith(PREF_THEME_CUSTOM_PREFIX)) {
            try {
                UUID uuid = UUID.fromString(
                        theme.substring(PREF_THEME_CUSTOM_PREFIX.length()));
                currentCustomTheme = customThemes.get(uuid);
            } catch (IllegalArgumentException ignored) {
            }
        } else {
            currentTheme = baseThemes.get(theme);
        }
        if (currentTheme == null && currentCustomTheme == null)
            currentTheme = fallbackTheme;

        for (ThemeChangeListener listener : themeChangeListeners)
            listener.onThemeChanged();
    }

    public void applyThemeToActivity(Activity activity) {
        if (currentCustomThemePatcher == null && currentCustomTheme != null) {
            ThemeResourceFileBuilder.CustomTheme theme = ThemeResourceFileBuilder
                    .createTheme(context, currentCustomTheme);
            currentTheme = theme;
            File themeFile = ThemeResourceFileBuilder.createThemeZipFile(context,
                    theme.getResTable());
            currentCustomThemePatcher = new Theme(context, themeFile.getAbsolutePath());
        }
        if (currentCustomThemePatcher != null)
            currentCustomThemePatcher.applyToActivity(activity);
    }

    public int getThemeIdToApply(int appThemeId) {
        if (currentTheme == null)
            return appThemeId;
        if (appThemeId == R.style.AppTheme_NoActionBar)
            return currentTheme.getThemeNoActionBarResId();
        else
            return currentTheme.getThemeResId();
    }


    public interface ThemeChangeListener {

        void onThemeChanged();

    }


    public static class ThemeResInfo {

        private int themeResId;
        private int themeNoActionBarResId;

        public ThemeResInfo(int themeResId, int themeNoActionBarResId) {
            this.themeResId = themeResId;
            this.themeNoActionBarResId = themeNoActionBarResId;
        }

        public int getThemeResId() {
            return themeResId;
        }

        public int getThemeNoActionBarResId() {
            return themeNoActionBarResId;
        }

    }

    public static class BaseTheme extends ThemeResInfo {

        private int nameResId;

        public BaseTheme(int nameResId, int themeResId, int themeNoActionBarResId) {
            super(themeResId, themeNoActionBarResId);
            this.nameResId = nameResId;
        }

        public int getNameResId() {
            return nameResId;
        }

    }

}
