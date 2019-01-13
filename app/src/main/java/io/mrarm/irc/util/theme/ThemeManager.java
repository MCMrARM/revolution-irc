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

        fallbackTheme = new BaseTheme("default", R.string.value_default,
                R.style.AppTheme, R.style.AppTheme_NoActionBar);
        baseThemes.put(fallbackTheme.getId(), fallbackTheme);
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
                    themeFile.delete();
                }
            }
        }
    }

    private ThemeInfo loadTheme(File themeFile, UUID uuid) throws IOException {
        ThemeInfo theme;
        try (BufferedReader reader = new BufferedReader(new FileReader(themeFile))) {
            theme = SettingsHelper.getGson().fromJson(reader, ThemeInfo.class);
        }
        if (theme == null)
            throw new IOException("Empty file");
        theme.uuid = uuid;
        theme.baseThemeInfo = baseThemes.get(theme.base);
        if (theme.baseThemeInfo == null)
            theme.baseThemeInfo = fallbackTheme;
        customThemes.put(uuid, theme);
        return theme;
    }

    public void saveTheme(ThemeInfo theme) throws IOException {
        if (theme.uuid == null) {
            theme.uuid = UUID.randomUUID();
            customThemes.put(theme.uuid, theme);
        }
        themesDir.mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(
                new File(themesDir, FILENAME_PREFIX + theme.uuid + FILENAME_SUFFIX)))) {
            SettingsHelper.getGson().toJson(theme, writer);
        }
    }

    public void deleteTheme(ThemeInfo theme) {
        customThemes.remove(theme.uuid);
        new File(themesDir, FILENAME_PREFIX + theme.uuid + FILENAME_SUFFIX).delete();
        if (currentCustomTheme == theme)
            setTheme(fallbackTheme);
    }

    public Collection<BaseTheme> getBaseThemes() {
        return baseThemes.values();
    }

    public Collection<ThemeInfo> getCustomThemes() {
        return customThemes.values();
    }

    public ThemeInfo getCustomTheme(UUID uuid) {
        return customThemes.get(uuid);
    }

    public BaseTheme getFallbackTheme() {
        return fallbackTheme;
    }

    public ThemeResInfo getCurrentTheme() {
        return currentTheme;
    }

    public ThemeInfo getCurrentCustomTheme() {
        return currentCustomTheme;
    }

    public void addThemeChangeListener(ThemeChangeListener listener) {
        themeChangeListeners.add(listener);
    }

    public void removeThemeChangeListener(ThemeChangeListener listener) {
        themeChangeListeners.remove(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String theme = SettingsHelper.getInstance(context).getTheme();
        if (theme != null && theme.startsWith(PREF_THEME_CUSTOM_PREFIX)) {
            try {
                UUID uuid = UUID.fromString(
                        theme.substring(PREF_THEME_CUSTOM_PREFIX.length()));
                applyTheme(customThemes.get(uuid));
            } catch (IllegalArgumentException ignored) {
            }
        } else {
            applyTheme(baseThemes.get(theme));
        }
        if (currentTheme == null && currentCustomTheme == null)
            currentTheme = fallbackTheme;

        for (ThemeChangeListener listener : themeChangeListeners)
            listener.onThemeChanged();
    }

    private void applyTheme(BaseTheme theme) {
        currentTheme = theme;
        currentCustomTheme = null;
        currentCustomThemePatcher = null;
        if (theme == null)
            currentTheme = fallbackTheme;
    }

    private void applyTheme(ThemeInfo theme) {
        currentTheme = null;
        currentCustomTheme = theme;
        currentCustomThemePatcher = null;
        if (theme == null)
            currentTheme = fallbackTheme;
    }

    public void setTheme(BaseTheme theme) {
        SettingsHelper.getInstance(context).setTheme(theme.getId());
    }

    public void setTheme(ThemeInfo theme) {
        SettingsHelper.getInstance(context).setTheme(PREF_THEME_CUSTOM_PREFIX + theme.uuid);
    }

    public void invalidateCurrentCustomTheme() {
        if (currentCustomTheme == null)
            return;
        currentTheme = null;
        currentCustomThemePatcher = null;
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

        private String id;
        private int nameResId;

        public BaseTheme(String id, int nameResId, int themeResId, int themeNoActionBarResId) {
            super(themeResId, themeNoActionBarResId);
            this.id = id;
            this.nameResId = nameResId;
        }

        public String getId() {
            return id;
        }

        public int getNameResId() {
            return nameResId;
        }

    }

}
