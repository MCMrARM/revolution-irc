package io.mrarm.irc.util.theme;

import android.graphics.Color;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ThemeInfo {

    public static final String COLOR_PRIMARY = "colorPrimary";
    public static final String COLOR_PRIMARY_DARK = "colorPrimaryDark";
    public static final String COLOR_ACCENT = "colorAccent";
    public static final String COLOR_BACKGROUND = "colorBackground";
    public static final String COLOR_BACKGROUND_FLOATING = "colorBackgroundFloating";
    public static final String COLOR_TEXT_PRIMARY = "textColorPrimary";
    public static final String COLOR_TEXT_SECONDARY = "textColorSecondary";
    public static final String COLOR_ICON = "app_colorIcon";
    public static final String COLOR_ICON_OPAQUE = "app_colorIconOpaque";
    public static final String COLOR_ACTION_BAR_TEXT_PRIMARY = "app_actionBarTextColorPrimary";
    public static final String COLOR_ACTION_BAR_TEXT_SECONDARY = "app_actionBarTextColorSecondary";
    public static final String COLOR_IRC_BLACK = "irc_colorBlack";
    public static final String COLOR_IRC_WHITE = "irc_colorWhite";
    public static final String COLOR_IRC_BLUE = "irc_colorBlue";
    public static final String COLOR_IRC_GREEN = "irc_colorGreen";
    public static final String COLOR_IRC_LIGHT_RED = "irc_colorLightRed";
    public static final String COLOR_IRC_BROWN = "irc_colorBrown";
    public static final String COLOR_IRC_PURPLE = "irc_colorPurple";
    public static final String COLOR_IRC_ORANGE = "irc_colorOrange";
    public static final String COLOR_IRC_YELLOW = "irc_colorYellow";
    public static final String COLOR_IRC_LIGHT_GREEN = "irc_colorLightGreen";
    public static final String COLOR_IRC_CYAN = "irc_colorCyan";
    public static final String COLOR_IRC_LIGHT_CYAN = "irc_colorLightCyan";
    public static final String COLOR_IRC_LIGHT_BLUE = "irc_colorLightBlue";
    public static final String COLOR_IRC_PINK = "irc_colorPink";
    public static final String COLOR_IRC_GRAY = "irc_colorGray";
    public static final String COLOR_IRC_LIGHT_GRAY = "irc_colorLightGray";
    public static final String COLOR_IRC_TIMESTAMP = "irc_colorTimestamp";
    public static final String COLOR_IRC_STATUS_TEXT = "irc_colorStatusText";
    public static final String COLOR_IRC_DISCONNECTED = "irc_colorDisconnected";
    public static final String COLOR_IRC_TOPIC = "irc_colorTopic";
    public static final String COLOR_IRC_MEMBER_OWNER = "irc_colorMemberOwner";
    public static final String COLOR_IRC_MEMBER_ADMIN = "irc_colorMemberAdmin";
    public static final String COLOR_IRC_MEMBER_OP = "irc_colorMemberOp";
    public static final String COLOR_IRC_MEMBER_HALF_OP = "irc_colorMemberHalfOp";
    public static final String COLOR_IRC_MEMBER_VOICE = "irc_colorMemberVoice";
    public static final String COLOR_IRC_MEMBER_NORMAL = "irc_colorMemberNormal";

    public static final String PROP_LIGHT_STATUS_BAR = "windowLightStatusBar";

    public transient UUID uuid;
    public String name;
    public String base;
    public transient ThemeManager.ThemeResInfo baseThemeInfo;
    @JsonAdapter(ColorsAdapter.class)
    public Map<String, Integer> colors = new HashMap<>();
    public Map<String, Object> properties = new HashMap<>();
    public List<Integer> savedColors = new ArrayList<>();

    public void copyFrom(ThemeInfo otherTheme) {
        base = otherTheme.base;
        baseThemeInfo = otherTheme.baseThemeInfo;
        colors = new HashMap<>(otherTheme.colors);
        properties = new HashMap<>(otherTheme.properties);
    }

    public Boolean getBool(String property) {
        Object o = properties.get(property);
        if (!(o instanceof Boolean))
            return null;
        return (Boolean) o;
    }


    public class ColorsAdapter extends TypeAdapter<Map<String, Integer>> {

        @Override
        public void write(JsonWriter out, Map<String, Integer> data) throws IOException {
            out.beginObject();
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                out.name(e.getKey());
                out.value(String.format("#%08X", e.getValue()));
            }
            out.endObject();
        }

        @Override
        public Map<String, Integer> read(JsonReader in) throws IOException {
            Map<String, Integer> ret = new HashMap<>();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                int value = Color.parseColor(in.nextString());
                ret.put(name, value);
            }
            in.endObject();
            return ret;
        }
    }

}
