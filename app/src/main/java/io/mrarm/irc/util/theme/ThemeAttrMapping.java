package io.mrarm.irc.util.theme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mrarm.irc.R;

public class ThemeAttrMapping {

    private static final Map<String, List<Integer>> colorToAttrs = new HashMap<>();

    public static Collection<String> getColorProperties() {
        return colorToAttrs.keySet();
    }

    private static void mapColorToAttr(String colorName, int attr) {
        List<Integer> l = colorToAttrs.get(colorName);
        if (l == null) {
            l = new ArrayList<>();
            colorToAttrs.put(colorName, l);
        }
        l.add(attr);
    }

    public static List<Integer> getColorAttrs(String color) {
        return colorToAttrs.get(color);
    }

    static {
        mapColorToAttr(ThemeInfo.COLOR_PRIMARY, R.attr.colorPrimary);
        mapColorToAttr(ThemeInfo.COLOR_PRIMARY_DARK, R.attr.colorPrimaryDark);
        mapColorToAttr(ThemeInfo.COLOR_ACCENT, R.attr.colorAccent);
        mapColorToAttr(ThemeInfo.COLOR_BACKGROUND, android.R.attr.colorBackground);
        mapColorToAttr(ThemeInfo.COLOR_BACKGROUND, android.R.attr.windowBackground);
        mapColorToAttr(ThemeInfo.COLOR_BACKGROUND_FLOATING, R.attr.colorBackgroundFloating);
        mapColorToAttr(ThemeInfo.COLOR_TEXT_PRIMARY, android.R.attr.textColorPrimary);
        mapColorToAttr(ThemeInfo.COLOR_TEXT_SECONDARY, android.R.attr.textColorSecondary);
        mapColorToAttr(ThemeInfo.COLOR_TEXT_SECONDARY, android.R.attr.textColorTertiary);
        mapColorToAttr(ThemeInfo.COLOR_ICON, R.attr.iconColor);
        mapColorToAttr(ThemeInfo.COLOR_ICON_OPAQUE, R.attr.iconColorOpaque);
    }

}
