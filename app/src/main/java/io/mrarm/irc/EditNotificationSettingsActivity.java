package io.mrarm.irc;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.SettingsItemDecorator;
import io.mrarm.irc.util.SimpleCounter;

public class EditNotificationSettingsActivity extends AppCompatActivity {

    public static final String ARG_USER_RULE_INDEX = "rule_index";
    public static final String ARG_DEFAULT_RULE_INDEX = "default_rule_index";

    SettingsListAdapter mAdapter;
    SimpleCounter mRequestCodeCounter = new SimpleCounter(1);

    NotificationRule mEditingRule;
    boolean mEditingDefaultRule = false;

    RecyclerView mRecyclerView;
    BasicEntry mBasicEntry;
    MatchEntry mMatchEntry;
    SettingsListAdapter.CheckBoxEntry mShowNotificationEntry;
    SettingsListAdapter.RingtoneEntry mSoundEntry;
    SettingsListAdapter.ListEntry mVibrationEntry;
    int[] mVibrationOptions;
    SettingsListAdapter.ListEntry mPriorityEntry;
    SettingsListAdapter.ColorEntry mColorEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(ARG_USER_RULE_INDEX)) {
            mEditingRule = NotificationManager.getUserRules(this).get(getIntent().getIntExtra(ARG_USER_RULE_INDEX, -1));
        } else if (getIntent().hasExtra(ARG_DEFAULT_RULE_INDEX)) {
            NotificationManager.loadUserRuleSettings(this);
            int ruleIndex = getIntent().getIntExtra(ARG_DEFAULT_RULE_INDEX, -1);
            if (ruleIndex >= NotificationManager.sDefaultTopRules.size())
                mEditingRule = NotificationManager.sDefaultBottomRules.get(ruleIndex - NotificationManager.sDefaultTopRules.size());
            else
                mEditingRule = NotificationManager.sDefaultTopRules.get(ruleIndex);
            mEditingDefaultRule = true;
        }

        setContentView(R.layout.activity_edit_notification_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new SettingsItemDecorator(this));

        mAdapter = new SettingsListAdapter(this);
        mAdapter.setRequestCodeCounter(mRequestCodeCounter);

        mBasicEntry = new BasicEntry();
        mMatchEntry = new MatchEntry();
        mShowNotificationEntry = new SettingsListAdapter.CheckBoxEntry(getString(R.string.notification_show), true);
        mSoundEntry = new SettingsListAdapter.RingtoneEntry(mAdapter, getString(R.string.notification_sound), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mVibrationOptions = getResources().getIntArray(R.array.notification_vibration_option_values);
        mVibrationEntry = new SettingsListAdapter.ListEntry(getString(R.string.notification_vibration), getResources().getStringArray(R.array.notification_vibration_options), 0);
        mPriorityEntry = new SettingsListAdapter.ListEntry(getString(R.string.notification_priority), getResources().getStringArray(R.array.notification_priority_options), 1);
        String[] colorNames = getResources().getStringArray(R.array.color_picker_color_names);
        colorNames[0] = getString(R.string.value_none);
        mColorEntry = new SettingsListAdapter.ColorEntry(getString(R.string.notification_color), getResources().getIntArray(R.array.colorPickerColors), colorNames, -1);
        mColorEntry.setHasDefaultOption(true);

        if (mEditingRule != null) {
            mBasicEntry.mName = mEditingRule.getName();

            mMatchEntry.mMatchMode = MatchEntry.MODE_REGEX;
            mMatchEntry.mMatchText = mEditingRule.getRegex();
            if (mMatchEntry.mMatchText != null) {
                if (mMatchEntry.mMatchText.startsWith("(^| |,)\\Q") && mMatchEntry.mMatchText.endsWith("\\E($| |,)")) {
                    String unescaped = unescapeRegex(mMatchEntry.mMatchText.substring(7, mMatchEntry.mMatchText.length() - 7));
                    if (unescaped != null) {
                        mMatchEntry.mMatchText = unescaped;
                        mMatchEntry.mMatchMode = MatchEntry.MODE_CONTAINS_WORD;
                    }
                } else {
                    String unescaped = unescapeRegex(mMatchEntry.mMatchText);
                    if (unescaped != null) {
                        mMatchEntry.mMatchText = unescaped;
                        mMatchEntry.mMatchMode = MatchEntry.MODE_CONTAINS;
                    }
                }
            }

            mMatchEntry.mCaseSensitive = mEditingRule.isRegexCaseInsensitive();
            mShowNotificationEntry.setChecked(!mEditingRule.settings.noNotification);
            if (mEditingRule.settings.soundEnabled)
                mSoundEntry.setValue((mEditingRule.settings.soundUri == null ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri.parse(mEditingRule.settings.soundUri)));
            else
                mSoundEntry.setValue(null);
            int vibrationOption;
            if (mEditingRule.settings.vibrationEnabled)
                vibrationOption = (mEditingRule.settings.vibrationDuration == 0 ? -1 : mEditingRule.settings.vibrationDuration);
            else
                vibrationOption = 0;
            int vibrationOptionIndex = 0;
            for (int i = mVibrationOptions.length - 1; i >= 0; i--) {
                if (mVibrationOptions[i] == vibrationOption) {
                    vibrationOptionIndex = i;
                    break;
                }
            }
            mVibrationEntry.setSelectedOption(vibrationOptionIndex);
            mPriorityEntry.setSelectedOption(mEditingRule.settings.priority + 1);
            if (mEditingRule.settings.lightEnabled)
                mColorEntry.setSelectedColor(mEditingRule.settings.light);
            else
                mColorEntry.setSelectedColorIndex(0);
        }

        if (!mEditingDefaultRule) {
            mAdapter.add(mBasicEntry);
            mAdapter.add(new SettingsListAdapter.HeaderEntry(getString(R.string.notification_header_match)));
            mAdapter.add(mMatchEntry);
            mAdapter.add(new SettingsListAdapter.HeaderEntry(getString(R.string.notification_header_applies_to)));
            if (mEditingRule == null) {
                mAdapter.add(new RuleEntry(NotificationRule.AppliesToEntry.channelEvents()));
            } else {
                for (NotificationRule.AppliesToEntry entry : mEditingRule.getAppliesTo())
                    mAdapter.add(new RuleEntry(entry.clone()));
            }
            mAdapter.add(new AddRuleEntry());
        }
        mAdapter.add(new SettingsListAdapter.HeaderEntry(getString(R.string.notification_header_options)));
        mAdapter.add(mShowNotificationEntry);
        mAdapter.add(mSoundEntry);
        mAdapter.add(mVibrationEntry);
        mAdapter.add(mPriorityEntry);
        mAdapter.add(mColorEntry);
        mRecyclerView.setAdapter(mAdapter);

        onShowNotificationSettingUpdated();
        mShowNotificationEntry.addListener((EntryRecyclerViewAdapter.Entry entry) -> {
            onShowNotificationSettingUpdated();
        });
    }

    private static String unescapeRegex(String regex) {
        if (!regex.startsWith("\\Q") || !regex.endsWith("\\E"))
            return null;

        StringBuilder str = new StringBuilder(regex.length());
        int iof, i = 2;
        while ((iof = regex.indexOf("\\E", i)) != -1 && iof < regex.length() - 2) {
            if (iof + 2 + 5 >= regex.length() || !regex.substring(iof + 2, iof + 2 + 5).equals("\\\\E\\Q"))
                return null;
            str.append(regex.substring(i, iof));
            str.append("\\E");
            i = iof + 2 + 5;
        }
        str.append(regex.substring(i, regex.length() - 2));
        return str.toString();
    }

    private void onShowNotificationSettingUpdated() {
        boolean checked = mShowNotificationEntry.isChecked();
        mSoundEntry.setEnabled(checked);
        mVibrationEntry.setEnabled(checked);
        mPriorityEntry.setEnabled(checked);
        mColorEntry.setEnabled(checked);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_only_done, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done || id == android.R.id.home) {
            if (id == R.id.action_done) {
                mRecyclerView.clearFocus();
                save();
            }

            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void save(NotificationRule rule) {
        // "unbind" all the items - this will in fact save their state instead
        for (int i = mRecyclerView.getChildCount() - 1; i >= 0; i--) {
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
            if (holder instanceof EntryRecyclerViewAdapter.EntryHolder)
                ((EntryRecyclerViewAdapter.EntryHolder) holder).unbind();
        }

        if (!mEditingDefaultRule) {
            rule.setName(mBasicEntry.mName);
            if (mMatchEntry.mMatchMode != MatchEntry.MODE_REGEX)
                rule.setMatchText(mMatchEntry.mMatchText, (mMatchEntry.mMatchMode == MatchEntry.MODE_CONTAINS_WORD), !mMatchEntry.mCaseSensitive);
            else
                rule.setRegex(mMatchEntry.mMatchText, !mMatchEntry.mCaseSensitive);
            List<NotificationRule.AppliesToEntry> appliesTo = new ArrayList<>();
            for (EntryRecyclerViewAdapter.Entry entry : mAdapter.getEntries()) {
                if (entry instanceof RuleEntry)
                    appliesTo.add(((RuleEntry) entry).mEntry);
            }
            rule.setAppliesTo(appliesTo);
        }

        // options
        mEditingRule.settings.noNotification = !mShowNotificationEntry.isChecked();
        rule.settings.lightEnabled = (mColorEntry.getSelectedColorIndex() != 0);
        if (rule.settings.lightEnabled)
            rule.settings.light = (mColorEntry.getSelectedColorIndex() == -1 ? 0 : mColorEntry.getSelectedColor());
        int vibrationDuration = mVibrationOptions[mVibrationEntry.getSelectedOption()];
        rule.settings.vibrationEnabled = (vibrationDuration != 0);
        if (rule.settings.vibrationEnabled)
            rule.settings.vibrationDuration = (vibrationDuration == -1 ? 0 : vibrationDuration);
        rule.settings.priority = mPriorityEntry.getSelectedOption() - 1;
        Uri soundUri = mSoundEntry.getValue();
        rule.settings.soundEnabled = (soundUri != null);
        rule.settings.soundUri = null;
        if (soundUri != RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            rule.settings.soundUri = soundUri.toString();
    }

    public void save() {
        if (mEditingRule != null) {
            save(mEditingRule);
        } else {
            mEditingRule = new NotificationRule();
            save(mEditingRule);
            NotificationManager.getUserRules(this).add(mEditingRule);
        }
        NotificationManager.saveUserRuleSettings(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAdapter.onActivityResult(requestCode, resultCode, data);
    }


    public static class BasicEntry extends EntryRecyclerViewAdapter.Entry {

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(BasicEntryHolder.class, R.layout.notification_settings_basic);

        String mName;

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class BasicEntryHolder extends SettingsListAdapter.SettingsEntryHolder<BasicEntry> {

        EditText mName;

        public BasicEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mName = (EditText) itemView.findViewById(R.id.entry_name);
        }

        @Override
        public void bind(BasicEntry entry) {
            mName.setText(entry.mName);
        }

        @Override
        public void unbind() {
            getEntry().mName = mName.getText().toString();
        }
    }


    public static class MatchEntry extends EntryRecyclerViewAdapter.Entry {

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(MatchEntryHolder.class, R.layout.notification_settings_match_message);

        public static final int MODE_CONTAINS = 0;
        public static final int MODE_CONTAINS_WORD = 1;
        public static final int MODE_REGEX = 2;

        String mMatchText;
        int mMatchMode;
        boolean mCaseSensitive;

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class MatchEntryHolder extends SettingsListAdapter.SettingsEntryHolder<MatchEntry> {

        Spinner mMode;
        EditText mText;
        CheckBox mCaseSensitive;

        public MatchEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);

            mMode = (Spinner) itemView.findViewById(R.id.match_mode);
            ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                    itemView.getContext(), R.array.notification_match_modes,
                    android.R.layout.simple_spinner_item);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mMode.setAdapter(spinnerAdapter);

            mText = (EditText) itemView.findViewById(R.id.match_text);

            mCaseSensitive = (CheckBox) itemView.findViewById(R.id.match_case);
        }

        @Override
        public void bind(MatchEntry entry) {
            mText.setText(entry.mMatchText);
            mMode.setSelection(entry.mMatchMode);
            mCaseSensitive.setChecked(entry.mCaseSensitive);
        }

        @Override
        public void unbind() {
            MatchEntry entry = getEntry();
            entry.mMatchMode = mMode.getSelectedItemPosition();
            entry.mCaseSensitive = mCaseSensitive.isChecked();
            entry.mMatchText = mText.getText().toString();
        }

    }


    public static class AddRuleEntry extends EntryRecyclerViewAdapter.Entry {

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(AddRuleEntryHolder.class, R.layout.notification_settings_add_rule);

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class AddRuleEntryHolder extends SettingsListAdapter.SettingsEntryHolder<AddRuleEntry> {

        public AddRuleEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            itemView.setOnClickListener((View v) -> {
                adapter.add(getAdapterPosition(), new RuleEntry(NotificationRule.AppliesToEntry.channelMessages()));
            });
        }

        @Override
        public void bind(AddRuleEntry entry) {
            // stub
        }

    }


    public static class RuleEntry extends EntryRecyclerViewAdapter.Entry {

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(RuleEntryHolder.class, R.layout.notification_settings_rule);
        private static final int sCollapsedHolder = EntryRecyclerViewAdapter.registerViewHolder(CollapsedRuleEntryHolder.class, R.layout.notification_settings_rule_collapsed);

        boolean mCollapsed = true;
        NotificationRule.AppliesToEntry mEntry;

        public RuleEntry(NotificationRule.AppliesToEntry entry) {
            mEntry = entry;
        }

        public void setCollapsed(boolean collapsed) {
            mCollapsed = collapsed;
            onUpdated();
        }

        @Override
        public int getViewHolder() {
            if (mCollapsed)
                return sCollapsedHolder;
            return sHolder;
        }

    }

    public static class RuleEntryHolder extends SettingsListAdapter.SettingsEntryHolder<RuleEntry> {

        private Spinner mServerSpinner;
        private ChipsEditText mChannels;
        private ChipsEditText mNicks;
        private View mChannelsCtr;
        private CheckBox mChannelMessages;
        private CheckBox mChannelNotices;
        private CheckBox mDirectMessages;
        private CheckBox mDirectNotices;
        private List<UUID> mSpinnerOptionUUIDs;

        public RuleEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);

            mServerSpinner = (Spinner) itemView.findViewById(R.id.server);
            mChannels = (ChipsEditText) itemView.findViewById(R.id.channels);
            mNicks = (ChipsEditText) itemView.findViewById(R.id.nicks);

            mChannelMessages = (CheckBox) itemView.findViewById(R.id.channel_messages);
            mChannelNotices = (CheckBox) itemView.findViewById(R.id.channel_notices);
            mDirectMessages = (CheckBox) itemView.findViewById(R.id.direct_messages);
            mDirectNotices = (CheckBox) itemView.findViewById(R.id.direct_notices);

            mChannelsCtr = itemView.findViewById(R.id.channels_ctr);
            mChannelsCtr.setVisibility(View.GONE);

            itemView.findViewById(R.id.expand).setOnClickListener((View view) -> {
                itemView.clearFocus();
                getEntry().setCollapsed(true);
            });
            itemView.findViewById(R.id.delete).setOnClickListener((View view) -> {
                itemView.clearFocus();
                adapter.remove(getAdapterPosition());
            });
            mServerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mChannelsCtr.setVisibility(position != 0 ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // stub
                }
            });
        }

        private void refreshSpinner() {
            mSpinnerOptionUUIDs = new ArrayList<>();
            List<String> options = new ArrayList<>();
            options.add(mServerSpinner.getContext().getString(R.string.value_any));
            mSpinnerOptionUUIDs.add(null);
            for (ServerConfigData data :
                    ServerConfigManager.getInstance(mServerSpinner.getContext()).getServers()) {
                options.add(data.name);
                mSpinnerOptionUUIDs.add(data.uuid);
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    mServerSpinner.getContext(), android.R.layout.simple_spinner_item, options);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mServerSpinner.setAdapter(spinnerAdapter);
        }

        @Override
        public void bind(RuleEntry entry) {
            refreshSpinner();
            int iof = mSpinnerOptionUUIDs.indexOf(entry.mEntry.server);
            mServerSpinner.setSelection((iof == -1 ? 0 : iof));
            mChannelsCtr.setVisibility(mServerSpinner.getSelectedItemPosition() != 0 ? View.VISIBLE : View.GONE);
            if (entry.mEntry.channels != null)
                mChannels.setItems(entry.mEntry.channels);
            else
                mChannels.clearItems();
            if (entry.mEntry.nicks != null)
                mNicks.setItems(entry.mEntry.nicks);
            else
                mNicks.clearItems();
            mChannelMessages.setChecked(entry.mEntry.matchChannelMessages);
            mChannelNotices.setChecked(entry.mEntry.matchChannelNotices);
            mDirectMessages.setChecked(entry.mEntry.matchDirectMessages);
            mDirectNotices.setChecked(entry.mEntry.matchDirectNotices);
        }

        @Override
        public void unbind() {
            NotificationRule.AppliesToEntry entry = getEntry().mEntry;
            getEntry().mEntry.server = mSpinnerOptionUUIDs.get(mServerSpinner.getSelectedItemPosition());
            if (mChannels.getItemCount() > 0)
                getEntry().mEntry.channels = mChannels.getItems();
            else
                getEntry().mEntry.channels = null;
            if (mNicks.getItemCount() > 0)
                getEntry().mEntry.nicks = mNicks.getItems();
            else
                getEntry().mEntry.nicks = null;
            entry.matchChannelMessages = mChannelMessages.isChecked();
            entry.matchChannelNotices = mChannelNotices.isChecked();
            entry.matchDirectMessages = mDirectMessages.isChecked();
            entry.matchDirectNotices = mDirectNotices.isChecked();
        }
    }

    public static class CollapsedRuleEntryHolder extends SettingsListAdapter.SettingsEntryHolder<RuleEntry> {

        private RuleEntry mEntry;
        private TextView mDesc;

        public CollapsedRuleEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mDesc = (TextView) itemView.findViewById(R.id.desc);
            itemView.setOnClickListener((View view) -> {
                mEntry.setCollapsed(false);
            });
            itemView.findViewById(R.id.expand).setOnClickListener((View view) -> {
                mEntry.setCollapsed(false);
            });
        }

        @Override
        public void bind(RuleEntry entry) {
            mEntry = entry;
            StringBuilder summaryText = new StringBuilder();
            if (entry.mEntry.server != null) {
                ServerConfigData s = ServerConfigManager.getInstance(mDesc.getContext()).findServer(entry.mEntry.server);
                summaryText.append(s != null ? s.name : "null");
                summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_delim));
                if (entry.mEntry.channels != null && entry.mEntry.channels.size() > 0) {
                    if (entry.mEntry.channels.size() > 1) {
                        summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_multi_channels, entry.mEntry.channels.size()));
                    } else {
                        summaryText.append(entry.mEntry.channels.get(0));
                    }
                } else {
                    summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_any_channel));
                }
            } else {
                summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_any_server));
            }
            summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_delim));
            if (entry.mEntry.nicks != null && entry.mEntry.nicks.size() > 0) {
                if (entry.mEntry.nicks.size() > 1) {
                    summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_multi_nicks, entry.mEntry.nicks.size()));
                } else {
                    summaryText.append(entry.mEntry.nicks.get(0));
                }
            } else {
                summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_any_nick));
            }
            mDesc.setText(summaryText.toString());
        }

    }

}
