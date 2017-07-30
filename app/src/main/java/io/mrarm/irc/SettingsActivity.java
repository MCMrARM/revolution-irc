package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.config.NotificationRuleManager;
import io.mrarm.irc.preference.ChipsEditTextPreference;
import io.mrarm.irc.preference.FontSizePickerPreference;
import io.mrarm.irc.preference.ListWithCustomPreference;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.NightModeRecreateHelper;
import io.mrarm.irc.preference.ReconnectIntervalPreference;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.SimpleCounter;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private NightModeRecreateHelper mNightModeHelper = new NightModeRecreateHelper(this);
    private SimpleCounter mRequestCodeCounter = new SimpleCounter(1000);
    private List<ActivityResultCallback> mActivityResultCallbacks = new ArrayList<>();

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            (Preference preference, Object value) -> {
                String stringValue = value.toString();

                if (preference instanceof ListWithCustomPreference &&
                        ListWithCustomPreference.isCustomValue(stringValue)) {
                    preference.setSummary(preference.getContext().getString(
                            R.string.value_custom_specific,
                            ListWithCustomPreference.getCustomValue(stringValue)));
                } else if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else if (preference instanceof FontSizePickerPreference) {
                    Integer intValue = value instanceof Integer ? (Integer) value : Integer.getInteger(stringValue);
                    if (intValue == null || intValue == -1)
                        preference.setSummary(R.string.value_default);
                    else
                        preference.setSummary(intValue + "sp");
                } else if (preference instanceof ReconnectIntervalPreference) {
                    List<ReconnectIntervalPreference.Rule> rules = ReconnectIntervalPreference.parseRules(stringValue);
                    StringBuilder builder = new StringBuilder();
                    boolean first = true;
                    Context context = preference.getContext();
                    String delim = context.getString(R.string.text_comma);
                    for (ReconnectIntervalPreference.Rule rule : rules) {
                        if (first)
                            first = false;
                        else
                            builder.append(delim);
                        builder.append(rule.getReconnectDelayAsString(context));
                        if (rule.repeatCount != -1)
                            builder.append(context.getResources().getQuantityString(R.plurals.reconnect_desc_tries, rule.repeatCount, rule.repeatCount));
                    }
                    preference.setSummary(builder.toString());
                } else {
                    preference.setSummary(stringValue);
                }
                return true;
            };

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference,
                                                     Preference.OnPreferenceChangeListener listener) {
        preference.setOnPreferenceChangeListener(listener);

        listener.onPreferenceChange(preference, PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), ""));
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        bindPreferenceSummaryToValue(preference, sBindPreferenceSummaryToValueListener);
    }

    private static void bindIntPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getInt(preference.getKey(), -1));
    }

    private static void bindChipsEditTextPreferenceSummaryToValue(
            Preference preference, Preference.OnPreferenceChangeListener listener) {
        preference.setOnPreferenceChangeListener(listener);

        listener.onPreferenceChange(preference, ((ChipsEditTextPreference) preference).getValue());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNightModeHelper.onStart();
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
                || InterfacePreferenceFragment.class.getName().equals(fragmentName)
                || AutocompletePreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || CommandPreferenceFragment.class.getName().equals(fragmentName)
                || StoragePreferenceFragment.class.getName().equals(fragmentName);
    }

    public SimpleCounter getRequestCodeCounter() {
        return mRequestCodeCounter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (ActivityResultCallback callback : mActivityResultCallbacks)
            callback.onActivityResult(requestCode, resultCode, data);
    }

    public void addActivityResultCallback(ActivityResultCallback callback) {
        mActivityResultCallbacks.add(callback);
    }

    public void removeActivityResultCallback(ActivityResultCallback callback) {
        mActivityResultCallbacks.remove(callback);
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

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
        private ChipsEditTextPreference mDefaultNickPreference;
        private EditTextPreference mDefaultUserPreference;
        private EditTextPreference mDefaultRealNamePreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_user);

            mDefaultNickPreference = (ChipsEditTextPreference) findPreference("default_nicks");
            mDefaultUserPreference = (EditTextPreference) findPreference("default_user");
            mDefaultRealNamePreference = (EditTextPreference) findPreference("default_realname");

            bindChipsEditTextPreferenceSummaryToValue(mDefaultNickPreference,
                    (Preference preference, Object newValue) -> {
                        List arrayValue = (List) newValue;
                        if (arrayValue != null && arrayValue.size() > 0) {
                            StringBuilder b = new StringBuilder();
                            for (Object o : arrayValue) {
                                if (b.length() > 0)
                                    b.append(getString(R.string.text_comma));
                                b.append(o.toString());
                            }
                            preference.setSummary(b.toString());
                        } else
                            preference.setSummary(R.string.value_not_set);

                        if (mDefaultUserPreference.getText() == null || mDefaultUserPreference.getText().length() == 0)
                            mDefaultUserPreference.setSummary(getPrimaryNick(arrayValue));
                        if (mDefaultRealNamePreference.getText() == null || mDefaultRealNamePreference.getText().length() == 0)
                            mDefaultRealNamePreference.setSummary(getPrimaryNick(arrayValue));
                        return true;
                    });
            bindPreferenceSummaryToValue(mDefaultUserPreference,
                    (Preference preference, Object newValue) -> {
                        String stringValue = newValue.toString();
                        if (stringValue.length() > 0)
                            preference.setSummary(stringValue);
                        else
                            preference.setSummary(getPrimaryNick(mDefaultNickPreference.getValue()));
                        return true;
                    });
            bindPreferenceSummaryToValue(mDefaultRealNamePreference,
                    (Preference preference, Object newValue) -> {
                        String stringValue = newValue.toString();
                        if (stringValue.length() > 0)
                            preference.setSummary(stringValue);
                        else
                            preference.setSummary(getPrimaryNick(mDefaultNickPreference.getValue()));
                        return true;
                    });

            bindPreferenceSummaryToValue(findPreference("default_quit_message"));
            bindPreferenceSummaryToValue(findPreference("default_part_message"));
        }

        private CharSequence getPrimaryNick(List<String> r) {
            if (r == null || r.size() == 0)
                return getString(R.string.value_not_set);
            return r.get(0);
        }
    }

    public static class ReconnectPreferenceFragment extends MyPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_reconnect);

            bindPreferenceSummaryToValue(findPreference("reconnect_interval"));
        }
    }

    public static class InterfacePreferenceFragment extends MyPreferenceFragment {
        private MessageInfo mSampleMessage;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_interface);
            findPreference("dark_theme").setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
                boolean enabled = (Boolean) newValue;
                AppCompatDelegate.setDefaultNightMode(enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                getActivity().recreate();
                return true;
            });
            bindPreferenceSummaryToValue(findPreference("chat_font"));
            bindIntPreferenceSummaryToValue(findPreference("chat_font_size"));
            bindPreferenceSummaryToValue(findPreference("chat_appbar_compact_mode"));

            MessageSenderInfo testSender = new MessageSenderInfo(getString(R.string.message_example_sender), "", "", null, null);
            Date date = MessageFormatSettingsActivity.getSampleMessageTime();
            mSampleMessage = new MessageInfo(testSender, date, getString(R.string.message_example_message), MessageInfo.MessageType.NORMAL);
        }

        @Override
        public void onResume() {
            super.onResume();
            SettingsHelper settingsHelper = SettingsHelper.getInstance(getActivity());

            findPreference("message_format").setSummary(MessageBuilder.getInstance(getActivity()).buildMessage(mSampleMessage));

            StringBuilder builder = new StringBuilder();
            if (settingsHelper.isNickAutocompleteButtonVisible())
                appendString(builder, getString(R.string.pref_title_nick_autocomplete_show_button));
            if (settingsHelper.isNickAutocompleteDoubleTapEnabled())
                appendString(builder, getString(R.string.pref_title_nick_autocomplete_double_tap));
            if (settingsHelper.shouldShowNickAutocompleteSuggestions())
                appendString(builder, getString(R.string.pref_title_nick_autocomplete_suggestions));
            if (settingsHelper.shouldShowNickAutocompleteAtSuggestions())
                appendString(builder, getString(R.string.pref_title_nick_autocomplete_at_suggestions));
            findPreference("nick_autocomplete").setSummary(builder.toString());
        }

        private void appendString(StringBuilder builder, String str) {
            if (builder.length() > 0) {
                builder.append(getString(R.string.text_comma));
                builder.append(str.substring(0, 1).toLowerCase() + str.substring(1));
            } else {
                builder.append(str);
            }
        }
    }

    public static class AutocompletePreferenceFragment extends MyPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_autocomplete);
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
            View view = inflater.inflate(R.layout.simple_list_with_fab, container, false);
            RecyclerView recyclerView = view.findViewById(R.id.items);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mAdapter = new NotificationRulesAdapter(getActivity());
            recyclerView.setAdapter(mAdapter);
            recyclerView.addItemDecoration(mAdapter.createItemDecoration(getActivity()));
            mAdapter.enableDragDrop(recyclerView);

            View addButton = view.findViewById(R.id.add);
            addButton.setOnClickListener((View v) -> {
                startActivity(new Intent(getActivity(), EditNotificationSettingsActivity.class));
            });

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onDestroyView() {
            if (mAdapter != null && mAdapter.hasUnsavedChanges()) {
                NotificationRuleManager.saveUserRuleSettings(getActivity());
            }
            mAdapter = null;
            super.onDestroyView();
        }

    }

    public static class CommandPreferenceFragment extends MyPreferenceFragment {

        private CommandAliasesAdapter mAdapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.simple_list_with_fab, container, false);
            RecyclerView recyclerView = view.findViewById(R.id.items);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(layoutManager);
            mAdapter = new CommandAliasesAdapter(getActivity());
            recyclerView.setAdapter(mAdapter);
            recyclerView.addItemDecoration(mAdapter.createItemDecoration(getActivity()));

            View addButton = view.findViewById(R.id.add);
            addButton.setOnClickListener((View v) -> {
                startActivity(new Intent(getActivity(), EditCommandAliasActivity.class));
            });

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            mAdapter.notifyDataSetChanged();
        }

    }

    public static class StoragePreferenceFragment extends MyPreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.simple_list_with_fab, container, false);
            view.findViewById(R.id.add).setVisibility(View.GONE);

            RecyclerView recyclerView = view.findViewById(R.id.items);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(layoutManager);
            StorageSettingsAdapter adapter = new StorageSettingsAdapter(getActivity());
            recyclerView.setItemAnimator(null);
            recyclerView.setAdapter(adapter);

            return view;
        }

    }

    public void setTintedPreferenceIcon(Preference pref, int icon) {
        Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(icon).mutate());
        DrawableCompat.setTint(drawable, getForegroundColor());
        pref.setIcon(drawable);
    }

    public interface ActivityResultCallback {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

}
