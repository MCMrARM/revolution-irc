package io.mrarm.irc.setting.fragment.theme;

import io.mrarm.irc.ThemeEditorActivity;
import io.mrarm.irc.setting.fragment.SettingsListFragment;
import io.mrarm.irc.util.theme.ThemeInfo;
import io.mrarm.irc.util.theme.live.LiveThemeManager;

public abstract class BaseThemeEditorFragment extends SettingsListFragment {

    protected ThemeInfo getThemeInfo() {
        return ((ThemeEditorActivity) getActivity()).getThemeInfo();
    }

    protected LiveThemeManager getLiveThemeManager() {
        return ((ThemeEditorActivity) getActivity()).getLiveThemeManager();
    }

}
