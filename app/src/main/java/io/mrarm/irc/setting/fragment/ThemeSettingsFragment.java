package io.mrarm.irc.setting.fragment;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.MaterialColorSetting;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;

public class ThemeSettingsFragment extends SettingsListFragment implements NamedSettingsFragment {
    @Override
    public String getName() {
        return getString(R.string.pref_header_theme);
    }

    @Override
    public SettingsListAdapter createAdapter() {
        SettingsListAdapter a = new SettingsListAdapter(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        a.setRequestCodeCounter(((SettingsActivity) getActivity()).getRequestCodeCounter());
        a.add(new CheckBoxSetting(getString(R.string.theme_dark_theme), null, false)
                .linkPreference(prefs, SettingsHelper.PREF_DARK_THEME)
                .addListener((EntryRecyclerViewAdapter.Entry entry) -> {
                    AppCompatDelegate.setDefaultNightMode(((CheckBoxSetting) entry).isChecked()
                            ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                    getActivity().recreate();
                }));
        a.add(new SingleLineMaterialColorSetting(getString(R.string.theme_primary_color))
                .linkPreference(prefs, SettingsHelper.PREF_COLOR_PRIMARY)
                .addListener((EntryRecyclerViewAdapter.Entry entry) -> getActivity().recreate() ));
        a.add(new SingleLineMaterialColorSetting(getString(R.string.theme_accent_color))
                .linkPreference(prefs, SettingsHelper.PREF_COLOR_ACCENT)
                .addListener((EntryRecyclerViewAdapter.Entry entry) -> getActivity().recreate() ));
        return a;
    }

    public static final class SingleLineMaterialColorSetting extends MaterialColorSetting {

        private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
                R.layout.settings_list_entry_color_single_line);

        public SingleLineMaterialColorSetting(String name) {
            super(name);
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

}
