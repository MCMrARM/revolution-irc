package io.mrarm.irc.setting.fragment.theme;

import io.mrarm.irc.R;
import io.mrarm.irc.setting.SettingsHeader;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.util.theme.ThemeInfo;

public class ChatThemeSettings extends BaseThemeEditorFragment {

    private final ExpandableColorSetting.ExpandGroup colorExpandGroup =
            new ExpandableColorSetting.ExpandGroup();

    @Override
    public SettingsListAdapter createAdapter() {
        SettingsListAdapter a = new SettingsListAdapter(this);
        a.add(new SettingsHeader(getString(R.string.theme_category_chat_irc)));
        addIrcColorSetting(a, R.string.theme_color_irc_black, ThemeInfo.COLOR_IRC_BLACK);
        addIrcColorSetting(a, R.string.theme_color_irc_white, ThemeInfo.COLOR_IRC_WHITE);
        addIrcColorSetting(a, R.string.theme_color_irc_blue, ThemeInfo.COLOR_IRC_BLUE);
        addIrcColorSetting(a, R.string.theme_color_irc_green, ThemeInfo.COLOR_IRC_GREEN);
        addIrcColorSetting(a, R.string.theme_color_irc_light_red, ThemeInfo.COLOR_IRC_LIGHT_RED);
        addIrcColorSetting(a, R.string.theme_color_irc_brown, ThemeInfo.COLOR_IRC_BROWN);
        addIrcColorSetting(a, R.string.theme_color_irc_purple, ThemeInfo.COLOR_IRC_PURPLE);
        addIrcColorSetting(a, R.string.theme_color_irc_orange, ThemeInfo.COLOR_IRC_ORANGE);
        addIrcColorSetting(a, R.string.theme_color_irc_yellow, ThemeInfo.COLOR_IRC_YELLOW);
        addIrcColorSetting(a, R.string.theme_color_irc_light_green, ThemeInfo.COLOR_IRC_LIGHT_GREEN);
        addIrcColorSetting(a, R.string.theme_color_irc_cyan, ThemeInfo.COLOR_IRC_CYAN);
        addIrcColorSetting(a, R.string.theme_color_irc_light_cyan, ThemeInfo.COLOR_IRC_LIGHT_CYAN);
        addIrcColorSetting(a, R.string.theme_color_irc_light_blue, ThemeInfo.COLOR_IRC_LIGHT_BLUE);
        addIrcColorSetting(a, R.string.theme_color_irc_pink, ThemeInfo.COLOR_IRC_PINK);
        addIrcColorSetting(a, R.string.theme_color_irc_gray, ThemeInfo.COLOR_IRC_GRAY);
        addIrcColorSetting(a, R.string.theme_color_irc_light_gray, ThemeInfo.COLOR_IRC_LIGHT_GRAY);
        a.add(new SettingsHeader(getString(R.string.theme_category_chat_message)));
        addIrcColorSetting(a, R.string.theme_color_message_timestamp, ThemeInfo.COLOR_IRC_TIMESTAMP);
        addIrcColorSetting(a, R.string.theme_color_message_status_text, ThemeInfo.COLOR_IRC_STATUS_TEXT);
        addIrcColorSetting(a, R.string.theme_color_message_disconnected, ThemeInfo.COLOR_IRC_DISCONNECTED);
        addIrcColorSetting(a, R.string.theme_color_message_topic, ThemeInfo.COLOR_IRC_TOPIC);
        a.add(new SettingsHeader(getString(R.string.theme_category_chat_member_list)));
        addIrcColorSetting(a, R.string.theme_color_member_owner, ThemeInfo.COLOR_IRC_MEMBER_OWNER);
        addIrcColorSetting(a, R.string.theme_color_member_admin, ThemeInfo.COLOR_IRC_MEMBER_ADMIN);
        addIrcColorSetting(a, R.string.theme_color_member_op, ThemeInfo.COLOR_IRC_MEMBER_OP);
        addIrcColorSetting(a, R.string.theme_color_member_half_op, ThemeInfo.COLOR_IRC_MEMBER_HALF_OP);
        addIrcColorSetting(a, R.string.theme_color_member_voice, ThemeInfo.COLOR_IRC_MEMBER_VOICE);
        addIrcColorSetting(a, R.string.theme_color_member_normal, ThemeInfo.COLOR_IRC_MEMBER_NORMAL);
        return a;
    }

    private void addIrcColorSetting(SettingsListAdapter a, int nameId, String propId) {
        a.add(new ThemeColorSetting(getString(nameId))
                .linkProperty(getContext(), getThemeInfo(), propId)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(getThemeInfo().savedColors));
    }

}
