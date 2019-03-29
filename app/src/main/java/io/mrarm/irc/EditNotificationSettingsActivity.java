package io.mrarm.irc;

import android.app.NotificationChannel;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.config.NotificationRuleManager;
import io.mrarm.irc.config.NotificationRule;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.ClickableSetting;
import io.mrarm.irc.setting.ColorListSetting;
import io.mrarm.irc.setting.ListSetting;
import io.mrarm.irc.setting.RingtoneSetting;
import io.mrarm.irc.setting.SettingsHeader;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleCounter;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.view.ChipsEditText;

public class EditNotificationSettingsActivity extends ThemedActivity {

    public static final String ARG_USER_RULE_INDEX = "rule_index";
    public static final String ARG_DEFAULT_RULE_INDEX = "default_rule_index";

    SettingsListAdapter mAdapter;
    SimpleCounter mRequestCodeCounter = new SimpleCounter(1);
    int mAndroidNotSettingsReqCode = mRequestCodeCounter.next();

    NotificationRule mEditingRule;
    boolean mEditingDefaultRule = false;

    RecyclerView mRecyclerView;
    BasicEntry mBasicEntry;
    MatchEntry mMatchEntry;
    CheckBoxSetting mShowNotificationEntry;
    CheckBoxSetting mUseMentionFormattingEntry;
    RingtoneSetting mSoundEntry;
    ListSetting mVibrationEntry;
    int[] mVibrationOptions;
    ListSetting mPriorityEntry;
    ColorListSetting mColorEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(ARG_USER_RULE_INDEX)) {
            mEditingRule = NotificationRuleManager.getUserRules(this).get(getIntent().getIntExtra(ARG_USER_RULE_INDEX, -1));
        } else if (getIntent().hasExtra(ARG_DEFAULT_RULE_INDEX)) {
            NotificationRuleManager.loadUserRuleSettings(this);
            int ruleIndex = getIntent().getIntExtra(ARG_DEFAULT_RULE_INDEX, -1);
            if (ruleIndex >= NotificationRuleManager.getDefaultTopRules().size())
                mEditingRule = NotificationRuleManager.getDefaultBottomRules().get(ruleIndex - NotificationRuleManager.getDefaultTopRules().size());
            else
                mEditingRule = NotificationRuleManager.getDefaultTopRules().get(ruleIndex);
            mEditingDefaultRule = true;
        }

        setContentView(R.layout.activity_edit_notification_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new SettingsListAdapter(this);
        mAdapter.setRequestCodeCounter(mRequestCodeCounter);
        mRecyclerView.addItemDecoration(mAdapter.createItemDecoration());

        mBasicEntry = new BasicEntry();
        mMatchEntry = new MatchEntry();
        mUseMentionFormattingEntry = new CheckBoxSetting(getString(R.string.notification_mention), true);
        mShowNotificationEntry = new CheckBoxSetting(getString(R.string.notification_show), true);
        mSoundEntry = new RingtoneSetting(mAdapter, getString(R.string.notification_sound), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mVibrationOptions = getResources().getIntArray(R.array.notification_vibration_option_values);
        mVibrationEntry = new ListSetting(getString(R.string.notification_vibration), getResources().getStringArray(R.array.notification_vibration_options), 0);
        mPriorityEntry = new ListSetting(getString(R.string.notification_priority), getResources().getStringArray(R.array.notification_priority_options), 1);
        String[] colorNames = getResources().getStringArray(R.array.color_picker_color_names);
        colorNames[0] = getString(R.string.value_none);
        mColorEntry = new ColorListSetting(getString(R.string.notification_color), getResources().getIntArray(R.array.colorPickerColors), colorNames, -1);
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

            mMatchEntry.mCaseSensitive = !mEditingRule.isRegexCaseInsensitive();
            mUseMentionFormattingEntry.setChecked(mEditingRule.settings.mentionFormatting);
            mShowNotificationEntry.setChecked(!mEditingRule.settings.noNotification);

            loadOptions();
        }

        if (!mEditingDefaultRule) {
            mAdapter.add(mBasicEntry);
            mAdapter.add(new SettingsHeader(getString(R.string.notification_header_match)));
            mAdapter.add(mMatchEntry);
            mAdapter.add(new SettingsHeader(getString(R.string.notification_header_applies_to)));
            if (mEditingRule == null) {
                mAdapter.add(new RuleEntry(NotificationRule.AppliesToEntry.channelEvents()));
            } else {
                for (NotificationRule.AppliesToEntry entry : mEditingRule.getAppliesTo())
                    mAdapter.add(new RuleEntry(entry.clone()));
            }
            mAdapter.add(new AddRuleEntry());
        }
        mAdapter.add(new SettingsHeader(getString(R.string.notification_header_options)));
        mAdapter.add(mUseMentionFormattingEntry);
        mAdapter.add(mShowNotificationEntry);
        mAdapter.add(mSoundEntry);
        mAdapter.add(mVibrationEntry);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            mAdapter.add(mPriorityEntry);
        mAdapter.add(mColorEntry);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mEditingRule != null) {
            mAdapter.add(new ClickableSetting(getString(
                    R.string.notification_android_settings_link), null)
                    .setOnClickListener((View v) -> {
                        if (mEditingRule.settings.notificationChannelId == null)
                            ChannelNotificationManager.createChannel(this, mEditingRule);

                        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        intent.putExtra(Settings.EXTRA_CHANNEL_ID,
                                mEditingRule.settings.notificationChannelId);
                        startActivityForResult(intent, mAndroidNotSettingsReqCode);
                    }));
        }
        mRecyclerView.setAdapter(mAdapter);

        onShowNotificationSettingUpdated();
        mShowNotificationEntry.addListener((EntryRecyclerViewAdapter.Entry entry) -> {
            onShowNotificationSettingUpdated();
        });
    }

    @SuppressWarnings("DoubleNegation")
    private boolean hasNotificationRuleChanges() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                mEditingRule == null || mEditingRule.settings.notificationChannelId == null)
            return false;
        android.app.NotificationManager mgr = (android.app.NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel =
                mgr.getNotificationChannel(mEditingRule.settings.notificationChannelId);

        Uri soundUri = mSoundEntry.getValue();
        if (channel.getSound() != soundUri &&
                (channel.getSound() == null || !channel.getSound().equals(soundUri)))
            return true;

        int vibrationDuration = mVibrationOptions[mVibrationEntry.getSelectedOption()];
        if (channel.shouldVibrate() != (vibrationDuration != 0))
            return true;
        if (channel.shouldVibrate()) {
            int channelVibrationDuration = channel.getVibrationPattern() == null ||
                    channel.getVibrationPattern().length != 2
                    ? -1 : (int) channel.getVibrationPattern()[1];
            if (channelVibrationDuration != vibrationDuration)
                return true;
        }

        if (channel.shouldShowLights() != (mColorEntry.getSelectedColorIndex() != 0))
            return true;
        //noinspection RedundantIfStatement
        if (channel.shouldShowLights() && channel.getLightColor() !=
                (mColorEntry.getSelectedColorIndex() == -1 ? 0 : mColorEntry.getSelectedColor()))
            return true;

        return false;
    }

    private void loadOptions() {
        loadNotificationRuleSettings();

        if (mEditingRule.settings.soundEnabled)
            mSoundEntry.setValue((mEditingRule.settings.soundUri == null
                    ? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    : Uri.parse(mEditingRule.settings.soundUri)));
        else
            mSoundEntry.setValue(null);
        int vibrationOption;
        if (mEditingRule.settings.vibrationEnabled)
            vibrationOption = (mEditingRule.settings.vibrationDuration == 0
                    ? -1 : mEditingRule.settings.vibrationDuration);
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

    private void loadNotificationRuleSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                mEditingRule == null || mEditingRule.settings == null ||
                mEditingRule.settings.notificationChannelId == null)
            return;
        android.app.NotificationManager mgr = (android.app.NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel =
                mgr.getNotificationChannel(mEditingRule.settings.notificationChannelId);

        mEditingRule.settings.soundEnabled = channel.getSound() != null;
        mEditingRule.settings.soundUri = null;
        if (channel.getSound() != null && !channel.getSound().equals(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)))
            mEditingRule.settings.soundUri = channel.getSound().toString();

        mEditingRule.settings.vibrationEnabled = channel.shouldVibrate();
        if (channel.shouldVibrate())
            mEditingRule.settings.vibrationDuration = channel.getVibrationPattern() == null ||
                    channel.getVibrationPattern().length != 2
                    ? 0 : (int) channel.getVibrationPattern()[1];

        mEditingRule.settings.lightEnabled = channel.shouldShowLights();
        if (channel.shouldShowLights())
            mEditingRule.settings.light = channel.getLightColor();
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
                if (!save())
                    return true;
            }

            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean save(NotificationRule rule) {
        // "unbind" all the items - this will in fact save their state instead
        for (int i = mRecyclerView.getChildCount() - 1; i >= 0; i--) {
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(mRecyclerView.getChildAt(i));
            if (holder instanceof EntryRecyclerViewAdapter.EntryHolder)
                ((EntryRecyclerViewAdapter.EntryHolder) holder).unbind();
        }

        if (!mEditingDefaultRule) {
            // validate
            for (NotificationRule other : NotificationRuleManager.getUserRules(this)) {
                if (other != rule && other.getName().equals(mBasicEntry.mName)) {
                    mBasicEntry.setHasDuplicateError();
                    mRecyclerView.scrollToPosition(mAdapter.getEntries().indexOf(mBasicEntry));
                    return false;
                }
            }
            if (mMatchEntry.mMatchMode == MatchEntry.MODE_REGEX && !MatchEntry.validateRegex(
                    mMatchEntry.mMatchText, mMatchEntry.mCaseSensitive)) {
                mRecyclerView.scrollToPosition(mAdapter.getEntries().indexOf(mMatchEntry));
                return false;
            }

            if (mMatchEntry.mMatchText == null)
                mMatchEntry.mMatchText = "";

            if (mMatchEntry.mMatchMode != MatchEntry.MODE_REGEX)
                rule.setMatchText(mMatchEntry.mMatchText, (mMatchEntry.mMatchMode == MatchEntry.MODE_CONTAINS_WORD), !mMatchEntry.mCaseSensitive);
            else
                rule.setRegex(mMatchEntry.mMatchText, !mMatchEntry.mCaseSensitive);
            rule.setName(mBasicEntry.mName);
            List<NotificationRule.AppliesToEntry> appliesTo = new ArrayList<>();
            for (EntryRecyclerViewAdapter.Entry entry : mAdapter.getEntries()) {
                if (entry instanceof RuleEntry)
                    appliesTo.add(((RuleEntry) entry).mEntry);
            }
            rule.setAppliesTo(appliesTo);
        }

        if (hasNotificationRuleChanges() && mEditingRule.settings.notificationChannelId != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationManager mgr = (android.app.NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            mgr.deleteNotificationChannel(mEditingRule.settings.notificationChannelId);
            mEditingRule.settings.notificationChannelId = null;
        }

        // options
        mEditingRule.settings.mentionFormatting = mUseMentionFormattingEntry.isChecked();
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
        if (soundUri != null && soundUri != RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            rule.settings.soundUri = soundUri.toString();

        ChannelNotificationManager.createChannel(this, mEditingRule);
        return true;
    }

    public boolean save() {
        if (mEditingRule != null) {
            if (!save(mEditingRule))
                return false;
        } else {
            mEditingRule = new NotificationRule();
            if (!save(mEditingRule)) {
                mEditingRule = null;
                return false;
            }
            NotificationRuleManager.getUserRules(this).add(mEditingRule);
        }

        NotificationRuleManager.saveUserRuleSettings(this);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == mAndroidNotSettingsReqCode) {
            if (hasNotificationRuleChanges())
                loadOptions();
            return;
        }
        mAdapter.onActivityResult(requestCode, resultCode, data);
    }


    public static class BasicEntry extends EntryRecyclerViewAdapter.Entry {

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(BasicEntryHolder.class, R.layout.notification_settings_basic);

        String mName;
        boolean mNameDuplicateError;

        void setHasDuplicateError() {
            mNameDuplicateError = true;
            onUpdated();
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class BasicEntryHolder extends SettingsListAdapter.SettingsEntryHolder<BasicEntry>
            implements SimpleTextWatcher.OnTextChangedListener {

        EditText mName;
        TextInputLayout mNameCtr;
        final SimpleTextWatcher mNameTextWatcher;

        public BasicEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mName = itemView.findViewById(R.id.entry_name);
            mNameCtr = itemView.findViewById(R.id.entry_name_ctr);
            mNameTextWatcher = new SimpleTextWatcher(this);
        }

        @Override
        public void bind(BasicEntry entry) {
            mName.removeTextChangedListener(mNameTextWatcher);
            mName.setText(entry.mName);
            mName.addTextChangedListener(mNameTextWatcher);
            if (entry.mNameDuplicateError)
                mNameCtr.setError(mNameCtr.getResources().getString(R.string.notification_rule_name_collision));
            else
                mNameCtr.setErrorEnabled(false);
        }

        @Override
        public void unbind() {
            getEntry().mName = mName.getText().toString();
        }

        @Override
        public void afterTextChanged(Editable s) {
            getEntry().mNameDuplicateError = false;
            mNameCtr.setErrorEnabled(false);
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

        public static boolean validateRegex(String regex, boolean caseSensitive) {
            Matcher matcher = CommandAliasManager.mMatchVariablesRegex.matcher(regex);
            StringBuffer buf = new StringBuffer();
            while (matcher.find())
                matcher.appendReplacement(buf, Matcher.quoteReplacement(Pattern.quote("replacement")));
            matcher.appendTail(buf);
            try {
                Pattern.compile(buf.toString(), caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                return false;
            }
            return true;
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class MatchEntryHolder extends SettingsListAdapter.SettingsEntryHolder<MatchEntry>
            implements SimpleTextWatcher.OnTextChangedListener {

        Spinner mMode;
        EditText mText;
        TextInputLayout mTextCtr;
        CheckBox mCaseSensitive;

        public MatchEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);

            mMode = itemView.findViewById(R.id.match_mode);
            ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(itemView.getContext(),
                    R.layout.simple_spinner_item, android.R.id.text1,
                    itemView.getResources().getStringArray(R.array.notification_match_modes));
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mMode.setAdapter(spinnerAdapter);

            mText = itemView.findViewById(R.id.match_text);
            mTextCtr = itemView.findViewById(R.id.match_text_ctr);

            mCaseSensitive = itemView.findViewById(R.id.match_case);

            mText.addTextChangedListener(new SimpleTextWatcher(this));
            mMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    afterTextChanged(mText.getText());
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
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

        @Override
        public void afterTextChanged(Editable s) {
            if (mMode.getSelectedItemPosition() != MatchEntry.MODE_REGEX ||
                    MatchEntry.validateRegex(s.toString(), mCaseSensitive.isChecked())) {
                mTextCtr.setErrorEnabled(false);
            } else {
                mTextCtr.setError(mTextCtr.getResources().getString(R.string.notification_rule_regex_invalid));
            }
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

            mServerSpinner = itemView.findViewById(R.id.server);
            mChannels = itemView.findViewById(R.id.channels);
            mNicks = itemView.findViewById(R.id.nicks);

            mChannelMessages = itemView.findViewById(R.id.channel_messages);
            mChannelNotices = itemView.findViewById(R.id.channel_notices);
            mDirectMessages = itemView.findViewById(R.id.direct_messages);
            mDirectNotices = itemView.findViewById(R.id.direct_notices);

            mChannelsCtr = itemView.findViewById(R.id.channels_ctr);
            mChannelsCtr.setVisibility(View.GONE);

            Button deleteButton = itemView.findViewById(R.id.delete);
            Drawable d = AppCompatResources.getDrawable(itemView.getContext(), R.drawable.ic_delete).mutate();
            DrawableCompat.setTint(d, deleteButton.getTextColors().getDefaultColor());
            deleteButton.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);

            itemView.findViewById(R.id.expand).setOnClickListener((View view) -> {
                itemView.clearFocus();
                getEntry().setCollapsed(true);
            });
            deleteButton.setOnClickListener((View view) -> {
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
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(mServerSpinner.getContext(),
                    R.layout.simple_spinner_item, android.R.id.text1, options);
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
            if (!mChannels.isEmpty())
                getEntry().mEntry.channels = Arrays.asList(mChannels.getItems());
            else
                getEntry().mEntry.channels = null;
            if (!mNicks.isEmpty())
                getEntry().mEntry.nicks = Arrays.asList(mNicks.getItems());
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
            mDesc = itemView.findViewById(R.id.desc);
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
                summaryText.append(mDesc.getContext().getString(R.string.text_comma));
                if (entry.mEntry.channels != null && entry.mEntry.channels.size() > 0) {
                    int count = entry.mEntry.channels.size();
                    if (count > 1) {
                        summaryText.append(mDesc.getResources().getQuantityString(R.plurals.notification_rule_summary_multi_channels, count, count));
                    } else {
                        summaryText.append(entry.mEntry.channels.get(0));
                    }
                } else {
                    summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_any_channel));
                }
            } else {
                summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_any_server));
            }
            summaryText.append(mDesc.getContext().getString(R.string.text_comma));
            if (entry.mEntry.nicks != null && entry.mEntry.nicks.size() > 0) {
                int count = entry.mEntry.nicks.size();
                if (count > 1) {
                    summaryText.append(mDesc.getResources().getQuantityString(R.plurals.notification_rule_summary_multi_nicks, count, count));
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
