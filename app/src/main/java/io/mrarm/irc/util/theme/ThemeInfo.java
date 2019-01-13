package io.mrarm.irc.util.theme;

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
    public Map<String, Integer> colors;
    public boolean lightToolbar;

}
