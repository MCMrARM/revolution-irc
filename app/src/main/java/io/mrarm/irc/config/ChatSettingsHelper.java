package io.mrarm.irc.config;

import android.graphics.Typeface;

import java.io.File;

import io.mrarm.irc.setting.ListWithCustomSetting;

public class ChatSettingsHelper {

    private static Typeface sCachedFont;

    public static Typeface getFont() {
        if (sCachedFont != null)
            return sCachedFont;
        String font = ChatSettings.getFontString();
        if (ListWithCustomSetting.isPrefCustomValue(font)) {
            File file = ListWithCustomSetting.getCustomFile(SettingsHelper.getContext(),
                    SettingsHelper.PREF_CHAT_FONT, font);
            try {
                sCachedFont = Typeface.createFromFile(file);
                return sCachedFont;
            } catch (Exception ignored) {
            }
        }
        if (font.equals("monospace"))
            return Typeface.MONOSPACE;
        else if (font.equals("serif"))
            return Typeface.SERIF;
        else
            return Typeface.DEFAULT;
    }

    static {
        SettingsHelper.changeEvent().listen(SettingsHelper.PREF_CHAT_FONT, () -> {
            sCachedFont = null;
        });
    }

}
