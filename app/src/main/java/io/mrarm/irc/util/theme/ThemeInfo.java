package io.mrarm.irc.util.theme;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThemeInfo {

    public static final String COLOR_PRIMARY = "colorPrimary";
    public static final String COLOR_PRIMARY_DARK = "colorPrimaryDark";
    public static final String COLOR_ACCENT = "colorAccent";

    public static final String PROP_LIGHT_TOOLBAR = "lightToolbar";

    public transient UUID uuid;
    public String name;
    public String base;
    public transient ThemeManager.ThemeResInfo baseThemeInfo;
    public Map<String, Object> properties = new HashMap<>();

    public void copyFrom(ThemeInfo otherTheme) {
        base = otherTheme.base;
        baseThemeInfo = otherTheme.baseThemeInfo;
        properties = new HashMap<>(otherTheme.properties);
    }

    public Integer getInt(String property) {
        Object o = properties.get(property);
        if (!(o instanceof Integer))
            return null;
        return (Integer) o;
    }

    public Boolean getBool(String property) {
        Object o = properties.get(property);
        if (!(o instanceof Boolean))
            return null;
        return (Boolean) o;
    }

}
