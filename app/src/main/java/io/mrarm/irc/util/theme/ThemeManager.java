package io.mrarm.irc.util.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.thememonkey.Theme;

public class ThemeManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static ThemeManager instance;

    public static ThemeManager getInstance(Context context) {
        if (instance == null)
            instance = new ThemeManager(context.getApplicationContext());
        return instance;
    }


    private Context context;
    private Theme currentThemePatcher;
    private ThemeResInfo currentTheme;
    private List<ThemeChangeListener> themeChangeListeners = new ArrayList<>();
    private ThemeResInfo fallbackTheme;

    public ThemeManager(Context context) {
        this.context = context;

        fallbackTheme = new ThemeResInfo(R.style.AppTheme, R.style.AppTheme_NoActionBar);

        SettingsHelper.getInstance(context).addPreferenceChangeListener(
                SettingsHelper.PREF_COLOR_PRIMARY, this);
        SettingsHelper.getInstance(context).addPreferenceChangeListener(
                SettingsHelper.PREF_COLOR_ACCENT, this);

        createTheme();
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
        createTheme();
        for (ThemeChangeListener listener : themeChangeListeners)
            listener.onThemeChanged();
    }

    private void createTheme() {
        ThemeInfo themeInfo = new ThemeInfo();
        themeInfo.colors = new HashMap<>();
        themeInfo.colors.put(ThemeInfo.COLOR_PRIMARY, getPrimaryColor());
        themeInfo.colors.put(ThemeInfo.COLOR_PRIMARY_DARK, getPrimaryDarkColor());
        themeInfo.colors.put(ThemeInfo.COLOR_ACCENT, getAccentColor());
        themeInfo.lightToolbar = shouldUseLightToolbar();
        ThemeResourceFileBuilder.CustomTheme theme = ThemeResourceFileBuilder
                .createTheme(context, themeInfo, fallbackTheme);
        currentTheme = theme;
        File themeFile = ThemeResourceFileBuilder.createThemeZipFile(context, theme.getResTable());
        currentThemePatcher = new Theme(context, themeFile.getAbsolutePath());
    }

    public int getPrimaryColor() {
        int def = StyledAttributesHelper.getColor(context, R.attr.colorPrimary, 0);
        return SettingsHelper.getInstance(context).getColor(SettingsHelper.PREF_COLOR_PRIMARY, def);
    }

    public boolean hasCustomPrimaryColor() {
        return SettingsHelper.getInstance(context).hasColor(SettingsHelper.PREF_COLOR_PRIMARY);
    }

    public int getPrimaryDarkColor() {
        if (!hasCustomPrimaryColor())
            return StyledAttributesHelper.getColor(context, R.attr.colorPrimaryDark, 0);
        int color = getPrimaryColor();
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    public boolean hasCustomAccentColor() {
        return SettingsHelper.getInstance(context).hasColor(SettingsHelper.PREF_COLOR_ACCENT);
    }

    public int getAccentColor() {
        int def = StyledAttributesHelper.getColor(context, R.attr.colorAccent, 0);
        return SettingsHelper.getInstance(context).getColor(SettingsHelper.PREF_COLOR_ACCENT, def);
    }

    public boolean shouldUseLightToolbar() {
        int c = getPrimaryColor();
        return ((c >> 16) & 0xFF) > 140 && ((c >> 8) & 0xFF) > 140 && (c & 0xFF) > 140;
    }


    public void applyThemeToActivity(Activity activity) {
        if (currentThemePatcher != null)
            currentThemePatcher.applyToActivity(activity);
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

}
