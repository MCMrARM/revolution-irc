package io.mrarm.irc.setting.fragment;

import android.content.Intent;
import android.view.View;

import io.mrarm.irc.MessageFormatSettingsActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.ClickableSetting;
import io.mrarm.irc.setting.FontSizeSetting;
import io.mrarm.irc.setting.ListSetting;
import io.mrarm.irc.setting.ListWithCustomSetting;
import io.mrarm.irc.setting.SettingsListAdapter;

public class InterfaceSettingsFragment extends SettingsListFragment
        implements NamedSettingsFragment {

    @Override
    public String getName() {
        return getString(R.string.pref_header_interface);
    }

    @Override
    public SettingsListAdapter createAdapter() {
        SettingsListAdapter a = new SettingsListAdapter(this);
        a.setRequestCodeCounter(((SettingsActivity) getActivity()).getRequestCodeCounter());
        a.add(new CheckBoxSetting(getString(R.string.pref_title_dark_theme),
                getString(R.string.pref_summary_dark_theme), false));
        a.add(new ListWithCustomSetting(a, getString(R.string.pref_title_font),
                getResources().getStringArray(R.array.pref_entries_font), 0,
                SettingsHelper.PREF_CHAT_FONT, ListWithCustomSetting.TYPE_FONT));
        a.add(new FontSizeSetting(getString(R.string.pref_title_font_size), -1));
        a.add(new ListSetting(getString(R.string.pref_title_appbar_compact_mode),
                getResources().getStringArray(R.array.pref_entries_appbar_compact_mode), 0));
        a.add(new ClickableSetting(getString(R.string.pref_title_message_format), null)
                .setIntent(new Intent(getActivity(), MessageFormatSettingsActivity.class)));
        a.add(new ClickableSetting(getString(R.string.pref_title_nick_autocomplete), null)
                .setOnClickListener((View v) -> {
                    ((SettingsActivity) getActivity()).setFragment(
                            new AutocompletePreferenceFragment());
                }));
        return a;
    }

}