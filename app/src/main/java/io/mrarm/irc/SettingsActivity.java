package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            (Preference preference, Object value) -> {
                String stringValue = value.toString();

                if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else {
                    preference.setSummary(stringValue);
                }
                return true;
            };

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || UserPreferenceFragment.class.getName().equals(fragmentName)
                || ReconnectPreferenceFragment.class.getName().equals(fragmentName)
                || AppearancePreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || CommandPreferenceFragment.class.getName().equals(fragmentName);
    }

    private static class MyPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }

    public static class UserPreferenceFragment extends MyPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_user);

            bindPreferenceSummaryToValue(findPreference("default_nick"));
            bindPreferenceSummaryToValue(findPreference("default_user"));
            bindPreferenceSummaryToValue(findPreference("default_realname"));
        }
    }

    public static class ReconnectPreferenceFragment extends MyPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_reconnect);
        }
    }

    public static class AppearancePreferenceFragment extends MyPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appearance);
        }
    }

    public static class NotificationPreferenceFragment extends MyPreferenceFragment {

        private NotificationRulesAdapter mAdapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.settings_simple_list_with_fab, container, false);
            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rules);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mAdapter = new NotificationRulesAdapter(getActivity());
            recyclerView.setAdapter(mAdapter);
            mAdapter.enableDragDrop(recyclerView);

            View addButton = view.findViewById(R.id.add);
            addButton.setOnClickListener((View v) -> {
                startActivity(new Intent(getActivity(), EditNotificationSettingsActivity.class));
            });

            return view;
        }

        @Override
        public void onDestroyView() {
            if (mAdapter != null && mAdapter.hasUnsavedChanges()) {
                NotificationManager.saveUserRuleSettings(getActivity());
            }
            mAdapter = null;
            super.onDestroyView();
        }

    }

    public static class CommandPreferenceFragment extends MyPreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.settings_simple_list_with_fab, container, false);
            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rules);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(layoutManager);
            CommandAliasesAdapter adapter = new CommandAliasesAdapter(getActivity());
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(adapter.createItemDecoration(getActivity()));

            View addButton = view.findViewById(R.id.add);
            addButton.setOnClickListener((View v) -> {
                startActivity(new Intent(getActivity(), EditCommandAliasActivity.class));
            });

            return view;
        }

    }

}
