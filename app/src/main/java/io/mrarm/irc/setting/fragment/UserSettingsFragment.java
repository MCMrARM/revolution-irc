package io.mrarm.irc.setting.fragment;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import io.mrarm.irc.R;
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.setting.ChipsEditTextSetting;
import io.mrarm.irc.setting.EditTextSetting;
import io.mrarm.irc.setting.SettingsListAdapter;

public class UserSettingsFragment extends SettingsListFragment implements NamedSettingsFragment {

    @Override
    public String getName() {
        return getString(R.string.pref_header_user);
    }

    @Override
    public SettingsListAdapter createAdapter() {
        SettingsListAdapter a = new SettingsListAdapter(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        a.add(new ChipsEditTextSetting(getString(R.string.pref_title_default_nick), null,
                getString(R.string.value_not_set))
                .linkSetting(prefs, AppSettings.PREF_DEFAULT_NICKS));
        a.add(new EditTextSetting(getString(R.string.pref_title_default_user), null,
                getString(R.string.value_not_set))
                .linkSetting(prefs, AppSettings.PREF_DEFAULT_USER));
        a.add(new EditTextSetting(getString(R.string.pref_title_default_realname), null,
                getString(R.string.value_not_set))
                .linkSetting(prefs, AppSettings.PREF_DEFAULT_REALNAME));
        a.add(new EditTextSetting(getString(R.string.pref_title_default_quit_message),
                null, getString(R.string.pref_value_default_quit_message))
                .linkSetting(prefs, AppSettings.PREF_DEFAULT_QUIT_MESSAGE));
        a.add(new EditTextSetting(getString(R.string.pref_title_default_part_message),
                null, getString(R.string.pref_value_default_part_message))
                .linkSetting(prefs, AppSettings.PREF_DEFAULT_PART_MESSAGE));
        return a;
    }

}