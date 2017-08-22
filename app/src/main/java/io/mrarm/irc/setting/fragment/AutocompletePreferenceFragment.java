package io.mrarm.irc.setting.fragment;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        a.add(new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_show_button),
                getString(R.string.pref_summary_nick_autocomplete_show_button), false)
                .linkPreference(prefs, SettingsHelper.PREF_NICK_AUTOCOMPLETE_SHOW_BUTTON));
        a.add(new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_double_tap),
                getString(R.string.pref_summary_nick_autocomplete_double_tap), true)
                .linkPreference(prefs, SettingsHelper.PREF_NICK_AUTOCOMPLETE_DOUBLE_TAP));
        a.add(new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_suggestions),
                getString(R.string.pref_summary_nick_autocomplete_suggestions), false)
                .linkPreference(prefs, SettingsHelper.PREF_NICK_AUTOCOMPLETE_SUGGESTIONS));
        CheckBoxSetting atSuggestions = new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_at_suggestions),
                getString(R.string.pref_summary_nick_autocomplete_at_suggestions), true);
        atSuggestions.linkPreference(prefs, SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS);
        a.add(atSuggestions);
        a.add(new CheckBoxSetting(
                getString(R.string.pref_title_nick_autocomplete_at_suggestions_remove_at),
                getString(R.string.pref_summary_nick_autocomplete_at_suggestions_remove_at), true)
                .linkPreference(prefs, SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS_REMOVE_AT)
                .requires(atSuggestions));
        a.add(new CheckBoxSetting(getString(R.string.pref_title_channel_autocomplete_suggestions),
                getString(R.string.pref_summary_channel_autocomplete_suggestions), true)
                .linkPreference(prefs, SettingsHelper.PREF_CHANNEL_AUTOCOMPLETE_SUGGESTIONS));
        return a;
    }

}
