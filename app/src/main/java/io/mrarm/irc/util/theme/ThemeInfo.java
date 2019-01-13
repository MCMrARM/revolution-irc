package io.mrarm.irc.util.theme;

import android.graphics.Color;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
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

    public static final String PROP_LIGHT_TOOLBAR = "lightToolbar";

    public transient UUID uuid;
    public String name;
    public String base;
    public transient ThemeManager.ThemeResInfo baseThemeInfo;
    @JsonAdapter(ColorsAdapter.class)
    public Map<String, Integer> colors = new HashMap<>();
    public Map<String, Object> properties = new HashMap<>();

    public void copyFrom(ThemeInfo otherTheme) {
        base = otherTheme.base;
        baseThemeInfo = otherTheme.baseThemeInfo;
        colors = otherTheme.colors;
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
