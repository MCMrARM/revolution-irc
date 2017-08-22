package io.mrarm.irc.setting.fragment;

import java.util.ArrayList;

import io.mrarm.irc.R;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.ReconnectIntervalSetting;
import io.mrarm.irc.setting.SettingsListAdapter;

public class ReconnectSettingsFragment extends SettingsListFragment
        implements NamedSettingsFragment {

    @Override
    public String getName() {
        return getString(R.string.pref_header_reconnect);
    }

    @Override
    public SettingsListAdapter createAdapter() {
        SettingsListAdapter a = new SettingsListAdapter(this);
        CheckBoxSetting mainSetting = new CheckBoxSetting(
                getString(R.string.pref_title_auto_reconnect),
                getString(R.string.pref_description_auto_reconnect), true);
        a.add(mainSetting);
        a.add(new CheckBoxSetting(getString(R.string.pref_title_reconnect_connchg),
                getString(R.string.pref_description_reconnect_connchg), true)
                .requires(mainSetting));
        a.add(new CheckBoxSetting(getString(R.string.pref_title_reconnect_wifi),
                getString(R.string.pref_description_reconnect_wifi), true)
                .requires(mainSetting));
        a.add(new ReconnectIntervalSetting(getString(R.string.pref_title_reconnect_pattern),
                new ArrayList<>()).requires(mainSetting));
        return a;
    }

}