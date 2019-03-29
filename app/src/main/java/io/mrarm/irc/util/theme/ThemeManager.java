package io.mrarm.irc.util.theme;

import android.app.Activity;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
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
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.thememonkey.Theme;

public class ThemeManager {

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
    private List<BaseTheme> baseThemeList = new ArrayList<>();
    private Map<UUID, ThemeInfo> customThemes = new HashMap<>();
    private boolean mNeedsApplyIrcColors = true;

    public ThemeManager(Context context) {
        this.context = context;
        themesDir = new File(context.getFilesDir(), "themes");

        fallbackTheme = new BaseTheme("default", R.string.value_default,
                R.style.AppTheme, R.style.AppTheme_NoActionBar, R.style.AppTheme_IRCColors,
                false);
        addBaseTheme(fallbackTheme);
        addBaseTheme(new BaseTheme("default_dark", R.string.theme_default_dark,
                R.style.AppTheme, R.style.AppTheme_NoActionBar, R.style.AppTheme_IRCColors,
                true));
        reloadThemes();

        SettingsHelper.changeEvent().listen(AppSettings.PREF_THEME, this::onThemeSettingChanged);
    }

    public File getThemesDir() {
        return themesDir;
    }

    private void addBaseTheme(BaseTheme theme) {
        baseThemeList.add(theme);
        baseThemes.put(theme.getId(), theme);
    }

    public void reloadThemes() {
        File[] themes = themesDir.listFiles();
        if (themes == null)
            return;
        if (currentCustomTheme != null)
            applyTheme(fallbackTheme);
        customThemes.clear();
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
        onThemeSettingChanged();
    }

    private ThemeInfo loadTheme(File themeFile, UUID uuid) throws IOException {
        ThemeInfo theme;
        try (BufferedReader reader = new BufferedReader(new FileReader(themeFile))) {
            theme = SettingsHelper.getGson().fromJson(reader, ThemeInfo.class);
        }
        if (theme == null)
            throw new IOException("Empty file");
        theme.uuid = uuid;
        theme.baseThemeInfo = getBaseThemeOrFallback(theme.base);
        customThemes.put(uuid, theme);
        return theme;
    }

    public File getThemePath(UUID uuid) {
        return new File(themesDir, FILENAME_PREFIX + uuid + FILENAME_SUFFIX);
    }

    public void saveTheme(ThemeInfo theme) throws IOException {
        if (theme.uuid == null) {
            theme.uuid = UUID.randomUUID();
            customThemes.put(theme.uuid, theme);
        }
        themesDir.mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getThemePath(theme.uuid)))) {
            SettingsHelper.getGson().toJson(theme, writer);
        }
    }

    public void exportTheme(ThemeInfo theme, BufferedWriter writer) {
        SettingsHelper.getGson().toJson(theme, writer);
    }

    public void importTheme(BufferedReader reader) throws IOException {
        ThemeInfo theme = SettingsHelper.getGson().fromJson(reader, ThemeInfo.class);
        if (theme == null)
            throw new IOException("Empty file");
        theme.baseThemeInfo = getBaseThemeOrFallback(theme.base);
        saveTheme(theme);
    }

    public void deleteTheme(ThemeInfo theme) {
        customThemes.remove(theme.uuid);
        new File(themesDir, FILENAME_PREFIX + theme.uuid + FILENAME_SUFFIX).delete();
        if (currentCustomTheme == theme)
            setTheme(fallbackTheme);
    }

    public Collection<BaseTheme> getBaseThemes() {
        return baseThemeList;
    }

    public BaseTheme getBaseThemeOrFallback(String name) {
        BaseTheme ret = baseThemes.get(name);
        if (ret == null)
            return fallbackTheme;
        return ret;
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

    private void onThemeSettingChanged() {
        String theme = AppSettings.getTheme();
        if (theme != null && theme.startsWith(PREF_THEME_CUSTOM_PREFIX)) {
            try {
                UUID uuid = UUID.fromString(
                        theme.substring(PREF_THEME_CUSTOM_PREFIX.length()));
                applyTheme(customThemes.get(uuid));
            } catch (IllegalArgumentException ignored) {
                applyTheme(fallbackTheme);
            }
        } else {
            applyTheme(baseThemes.get(theme));
        }

        for (ThemeChangeListener listener : themeChangeListeners)
            listener.onThemeChanged();
    }

    private void applyTheme(BaseTheme theme) {
        currentTheme = theme;
        currentCustomTheme = null;
        currentCustomThemePatcher = null;
        if (theme == null)
            currentTheme = fallbackTheme;
        mNeedsApplyIrcColors = true;
    }

    private void applyTheme(ThemeInfo theme) {
        currentTheme = null;
        currentCustomTheme = theme;
        currentCustomThemePatcher = null;
        if (theme == null)
            currentTheme = fallbackTheme;
        mNeedsApplyIrcColors = true;
    }

    public void setTheme(BaseTheme theme) {
        AppSettings.setTheme(theme.getId());
    }

    public void setTheme(ThemeInfo theme) {
        AppSettings.setTheme(PREF_THEME_CUSTOM_PREFIX + theme.uuid);
    }

    public void invalidateCurrentCustomTheme() {
        if (currentCustomTheme == null)
            return;
        currentTheme = null;
        currentCustomThemePatcher = null;
        mNeedsApplyIrcColors = true;
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
        ThemeResInfo currentBaseTheme = currentTheme;
        if (currentCustomTheme != null)
            currentBaseTheme = currentCustomTheme.baseThemeInfo;
        boolean isThemeDark = currentBaseTheme instanceof BaseTheme &&
                ((BaseTheme) currentBaseTheme).isDark;
        if (currentCustomThemePatcher == null && isThemeDark) {
            currentCustomThemePatcher = new Theme(activity.getAssets());
        }
        if (mNeedsApplyIrcColors) {
            Configuration c = new Configuration();
            c.setToDefaults();
            c.uiMode = Configuration.UI_MODE_TYPE_NORMAL;
            if (currentBaseTheme instanceof BaseTheme && ((BaseTheme) currentBaseTheme).isDark)
                c.uiMode |= Configuration.UI_MODE_NIGHT_YES;
            Resources r = new Resources(currentCustomThemePatcher != null ?
                    currentCustomThemePatcher.getAssetManager() : context.getAssets(),
                    new DisplayMetrics(), c);
            Resources.Theme t = r.newTheme();
            ThemeResInfo resInfo = currentTheme != null ? currentTheme : fallbackTheme;
            t.applyStyle(resInfo.getThemeResId(), true);
            IRCColorUtils.loadColors(t, resInfo.getIRCColorsResId());
            mNeedsApplyIrcColors = false;
        }
        if (currentCustomThemePatcher != null) {
            currentCustomThemePatcher.applyToActivity(activity);
        }
        if (isThemeDark)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    public int getThemeIdToApply(int appThemeId) {
        if (currentTheme == null)
            return appThemeId;
        if (appThemeId == R.style.AppTheme_NoActionBar)
            return currentTheme.getThemeNoActionBarResId();
        else if (appThemeId == R.style.AppTheme)
            return currentTheme.getThemeResId();
        else
            return appThemeId;
    }


    public interface ThemeChangeListener {

        void onThemeChanged();

    }


    public static class ThemeResInfo {

        private int themeResId;
        private int themeNoActionBarResId;
        private int ircColorsResId;

        public ThemeResInfo(int themeResId, int themeNoActionBarResId, int ircColorsResId) {
            this.themeResId = themeResId;
            this.themeNoActionBarResId = themeNoActionBarResId;
            this.ircColorsResId = ircColorsResId;
        }

        public int getThemeResId() {
            return themeResId;
        }

        public int getThemeNoActionBarResId() {
            return themeNoActionBarResId;
        }

        public int getIRCColorsResId() {
            return ircColorsResId;
        }

    }

    public static class BaseTheme extends ThemeResInfo {

        private String id;
        private int nameResId;
        private boolean isDark;

        public BaseTheme(String id, int nameResId, int themeResId, int themeNoActionBarResId,
                         int ircColorsResId, boolean isDark) {
            super(themeResId, themeNoActionBarResId, ircColorsResId);
            this.id = id;
            this.nameResId = nameResId;
            this.isDark = isDark;
        }

        public boolean isDark() {
            return isDark;
        }

        public String getId() {
            return id;
        }

        public int getNameResId() {
            return nameResId;
        }

    }

}
