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
    }

}
