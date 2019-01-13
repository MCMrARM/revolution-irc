package io.mrarm.irc.util.theme;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThemeInfo {

    public static final String COLOR_PRIMARY = "colorPrimary";
    public static final String COLOR_PRIMARY_DARK = "colorPrimaryDark";
    public static final String COLOR_ACCENT = "colorAccent";

    public transient UUID uuid;
    public String name;
    public String base;
    public transient ThemeManager.ThemeResInfo baseThemeInfo;
    public Map<String, Integer> colors = new HashMap<>();
    public boolean lightToolbar;

    public void copyFrom(ThemeInfo otherTheme) {
        base = otherTheme.base;
        baseThemeInfo = otherTheme.baseThemeInfo;
        colors = new HashMap<>(otherTheme.colors);
        lightToolbar = otherTheme.lightToolbar;
    }

}
