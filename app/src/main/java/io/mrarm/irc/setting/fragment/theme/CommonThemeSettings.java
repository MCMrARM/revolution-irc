package io.mrarm.irc.setting.fragment.theme;

import android.os.Bundle;
import androidx.annotation.Nullable;

import java.util.Collection;

import io.mrarm.irc.R;
import io.mrarm.irc.setting.ListSetting;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.theme.ThemeInfo;
import io.mrarm.irc.util.theme.ThemeManager;
import io.mrarm.irc.util.theme.live.LiveThemeManager;

public class CommonThemeSettings extends BaseThemeEditorFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public SettingsListAdapter createAdapter() {
        ThemeInfo themeInfo = getThemeInfo();
        ThemeManager themeManager = ThemeManager.getInstance(getContext());
        SettingsListAdapter a = new SettingsListAdapter(this);
        Collection<ThemeManager.BaseTheme> baseThemes = themeManager.getBaseThemes();
        String[] baseThemeNames = new String[baseThemes.size()];
        String[] baseThemeIds = new String[baseThemes.size()];
        int i = 0;
        for (ThemeManager.BaseTheme baseTheme : baseThemes) {
            baseThemeNames[i] = getString(baseTheme.getNameResId());
            baseThemeIds[i] = baseTheme.getId();
            ++i;
        }
        String baseThemeId = themeInfo.base == null ? themeManager.getFallbackTheme().getId() : themeInfo.base;
        ListSetting baseSetting = new ThemeListSetting(getString(R.string.theme_base), baseThemeNames, baseThemeIds, baseThemeId);
        baseSetting.addListener((EntryRecyclerViewAdapter.Entry entry) -> {
            themeInfo.base = ((ListSetting) entry).getSelectedOptionValue();
            themeInfo.baseThemeInfo = themeManager.getBaseThemeOrFallback(themeInfo.base);
            onNonLivePropertyChanged();
        });
        a.add(baseSetting);

        SettingsListAdapter.SettingChangedListener restartListener =
                (EntryRecyclerViewAdapter.Entry entry) -> onNonLivePropertyChanged();
        ExpandableColorSetting.ExpandGroup colorExpandGroup = new ExpandableColorSetting.ExpandGroup();
        LiveThemeManager liveThemeManager = getLiveThemeManager();
        a.add(new ThemeColorSetting(getString(R.string.theme_color_primary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_PRIMARY)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_primary_dark))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_PRIMARY_DARK)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_accent))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ACCENT)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_action_bar_text_primary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ACTION_BAR_TEXT_PRIMARY)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_action_bar_text_secondary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ACTION_BAR_TEXT_SECONDARY)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeBoolSetting(getString(R.string.theme_color_status_bar_light))
                .linkProperty(getContext(), themeInfo, ThemeInfo.PROP_LIGHT_STATUS_BAR)
                .addListener(restartListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_background))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_BACKGROUND)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_background_floating))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_BACKGROUND_FLOATING)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_text_primary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_TEXT_PRIMARY)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_text_secondary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_TEXT_SECONDARY)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_icon))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ICON)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_icon_opaque))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ICON_OPAQUE)
                .linkLiveApplyManager(liveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setSavedColors(themeInfo.savedColors));
        return a;
    }

    private void onNonLivePropertyChanged() {
        ThemeManager.getInstance(getContext()).invalidateCurrentCustomTheme();
        getActivity().recreate();
    }


}
