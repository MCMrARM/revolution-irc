package io.mrarm.irc.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;

import java.io.File;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.thememonkey.Theme;

public class ThemeHelper {

    private static Theme currentTheme = null;

    public static int getPrimaryColor(Context ctx) {
        int def = StyledAttributesHelper.getColor(ctx, R.attr.colorPrimary, 0);
        return SettingsHelper.getInstance(ctx).getColor(SettingsHelper.PREF_COLOR_PRIMARY, def);
    }

    public static boolean hasCustomPrimaryColor(Context ctx) {
        return SettingsHelper.getInstance(ctx).hasColor(SettingsHelper.PREF_COLOR_PRIMARY);
    }

    public static int getPrimaryDarkColor(Context ctx) {
        if (!hasCustomPrimaryColor(ctx))
            return StyledAttributesHelper.getColor(ctx, R.attr.colorPrimaryDark, 0);
        int color = getPrimaryColor(ctx);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    public static boolean hasCustomAccentColor(Context ctx) {
        return SettingsHelper.getInstance(ctx).hasColor(SettingsHelper.PREF_COLOR_ACCENT);
    }

    public static int getAccentColor(Context ctx) {
        int def = StyledAttributesHelper.getColor(ctx, R.attr.colorAccent, 0);
        return SettingsHelper.getInstance(ctx).getColor(SettingsHelper.PREF_COLOR_ACCENT, def);
    }


    public static void applyThemeToActivity(Activity activity, int appThemeId) {
        if (currentTheme == null) {
            if (hasCustomAccentColor(activity) || hasCustomAccentColor(activity)) {
                File themeFile = ThemeResourceFileBuilder.createThemeZipFile(activity);
                currentTheme = new Theme(activity.getApplicationContext(),
                        themeFile.getAbsolutePath());
            } else {
                return;
            }
        }
        currentTheme.applyToActivity(activity);
        if (appThemeId == R.style.AppTheme_NoActionBar)
            activity.setTheme(ThemeResourceFileBuilder.getNoActionBarThemeId());
        else
            activity.setTheme(ThemeResourceFileBuilder.getPrimaryThemeId());
    }

}
