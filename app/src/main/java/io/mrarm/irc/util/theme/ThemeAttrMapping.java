package io.mrarm.irc.util.theme;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mrarm.irc.R;

public class ThemeAttrMapping {

    private static final Map<String, List<Integer>> colorToAttrs = new HashMap<>();
    private static final Map<String, Integer> ircColorToAttrs = new HashMap<>();

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

    public static Integer getIrcColorAttr(String color) {
        return ircColorToAttrs.get(color);
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
        mapColorToAttr(ThemeInfo.COLOR_ACTION_BAR_TEXT_PRIMARY, R.attr.actionBarTextColorPrimary);
        mapColorToAttr(ThemeInfo.COLOR_ACTION_BAR_TEXT_SECONDARY, R.attr.actionBarTextColorSecondary);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mapColorToAttr(ThemeInfo.COLOR_BACKGROUND_FLOATING, android.R.attr.colorBackgroundFloating);

        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_BLACK, R.attr.colorBlack);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_WHITE, R.attr.colorWhite);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_BLUE, R.attr.colorBlue);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_GREEN, R.attr.colorGreen);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_LIGHT_RED, R.attr.colorLightRed);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_BROWN, R.attr.colorBrown);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_PURPLE, R.attr.colorPurple);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_ORANGE, R.attr.colorOrange);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_YELLOW, R.attr.colorYellow);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_LIGHT_GREEN, R.attr.colorLightGreen);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_CYAN, R.attr.colorCyan);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_LIGHT_CYAN, R.attr.colorLightCyan);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_LIGHT_BLUE, R.attr.colorLightBlue);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_PINK, R.attr.colorPink);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_GRAY, R.attr.colorGray);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_LIGHT_GRAY, R.attr.colorLightGray);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_TIMESTAMP, R.attr.colorTimestamp);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_STATUS_TEXT, R.attr.colorStatusText);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_DISCONNECTED, R.attr.colorDisconnected);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_TOPIC, R.attr.colorTopic);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_MEMBER_OWNER, R.attr.colorMemberOwner);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_MEMBER_ADMIN, R.attr.colorMemberAdmin);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_MEMBER_OP, R.attr.colorMemberOp);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_MEMBER_HALF_OP, R.attr.colorMemberHalfOp);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_MEMBER_VOICE, R.attr.colorMemberVoice);
        ircColorToAttrs.put(ThemeInfo.COLOR_IRC_MEMBER_NORMAL, R.attr.colorMemberNormal);
    }

}
