package io.mrarm.irc.setting.fragment;

import io.mrarm.irc.R;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.SettingsListAdapter;

public class AutocompletePreferenceFragment extends SettingsListFragment
        implements NamedSettingsFragment {

    @Override
    public String getName() {
        return getString(R.string.pref_title_nick_autocomplete);
    }

    @Override
    public SettingsListAdapter createAdapter() {
        SettingsListAdapter a = new SettingsListAdapter(this);
        a.add(new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_show_button),
                getString(R.string.pref_summary_nick_autocomplete_show_button), false));
        a.add(new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_double_tap),
                getString(R.string.pref_summary_nick_autocomplete_double_tap), true));
        a.add(new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_suggestions),
                getString(R.string.pref_summary_nick_autocomplete_suggestions), false));
        CheckBoxSetting atSuggestions = new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_at_suggestions),
                getString(R.string.pref_summary_nick_autocomplete_at_suggestions), true);
        a.add(atSuggestions);
        a.add(new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_at_suggestions_remove_at),
                getString(R.string.pref_summary_nick_autocomplete_at_suggestions_remove_at), true)
                .requires(atSuggestions));
        a.add(new CheckBoxSetting(getString(R.string.pref_title_channel_autocomplete_suggestions),
                getString(R.string.pref_summary_channel_autocomplete_suggestions), true));
        return a;
    }

}
