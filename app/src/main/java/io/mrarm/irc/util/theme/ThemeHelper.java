package io.mrarm.irc.util.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.thememonkey.Theme;

public class ThemeHelper implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static ThemeHelper instance;

    public static ThemeHelper getInstance(Context context) {
        if (instance == null)
            instance = new ThemeHelper(context.getApplicationContext());
        return instance;
    }


    private Context context;
    private Theme currentTheme;
    private List<ThemeChangeListener> themeChangeListeners = new ArrayList<>();

    public ThemeHelper(Context context) {
        this.context = context;

        SettingsHelper.getInstance(context).addPreferenceChangeListener(
                SettingsHelper.PREF_COLOR_PRIMARY, this);
        SettingsHelper.getInstance(context).addPreferenceChangeListener(
                SettingsHelper.PREF_COLOR_ACCENT, this);
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
        for (ThemeChangeListener listener : themeChangeListeners)
            listener.onThemeChanged();
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
        if (currentTheme == null) {
            if (hasCustomAccentColor() || hasCustomAccentColor()) {
                File themeFile = ThemeResourceFileBuilder.createThemeZipFile(activity);
                currentTheme = new Theme(activity.getApplicationContext(),
                        themeFile.getAbsolutePath());
            } else {
                return;
            }
        }
        currentTheme.applyToActivity(activity);
    }

    public int getThemeIdToApply(int appThemeId) {
        if (currentTheme == null)
            return appThemeId;
        if (appThemeId == R.style.AppTheme_NoActionBar)
            return ThemeResourceFileBuilder.getNoActionBarThemeId();
        else
            return ThemeResourceFileBuilder.getPrimaryThemeId();
    }


    public interface ThemeChangeListener {

        void onThemeChanged();

    }

}
