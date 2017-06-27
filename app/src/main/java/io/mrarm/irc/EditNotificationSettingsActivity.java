package io.mrarm.irc;

import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleCounter;

public class EditNotificationSettingsActivity extends AppCompatActivity {

    SettingsListAdapter mAdapter;
    SimpleCounter mRequestCodeCounter = new SimpleCounter(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_notification_settings);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new SettingsListAdapter(this);
        mAdapter.setRequestCodeCounter(mRequestCodeCounter);
        mAdapter.add(new BasicEntry());
        mAdapter.add(new SettingsListAdapter.HeaderEntry(getString(R.string.notification_header_match)));
        mAdapter.add(new MatchEntry());
        mAdapter.add(new SettingsListAdapter.HeaderEntry(getString(R.string.notification_header_applies_to)));
        mAdapter.add(new RuleEntry(NotificationRule.AppliesToEntry.channelEvents()));
        mAdapter.add(new AddRuleEntry());
        mAdapter.add(new SettingsListAdapter.HeaderEntry(getString(R.string.notification_header_options)));
        mAdapter.add(new SettingsListAdapter.RingtoneEntry(mAdapter, getString(R.string.notification_sound), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)));
        mAdapter.add(new SettingsListAdapter.ListEntry(getString(R.string.notification_vibration), getResources().getStringArray(R.array.notification_vibration_options), 0));
        mAdapter.add(new SettingsListAdapter.ListEntry(getString(R.string.notification_priority), getResources().getStringArray(R.array.notification_priority_options), 1));
        String[] colorNames = getResources().getStringArray(R.array.color_picker_color_names);
        colorNames[0] = getString(R.string.value_none);
        mAdapter.add(new SettingsListAdapter.ColorEntry(getString(R.string.notification_color), getResources().getIntArray(R.array.colorPickerColors), colorNames, 2));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAdapter.onActivityResult(requestCode, resultCode, data);
    }


    public static class BasicEntry extends EntryRecyclerViewAdapter.Entry {

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(BasicEntryHolder.class, R.layout.notification_settings_basic);

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class BasicEntryHolder extends EntryRecyclerViewAdapter.EntryHolder<BasicEntry> {

        public BasicEntryHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bind(BasicEntry entry) {
            //
        }

    }


    public static class MatchEntry extends EntryRecyclerViewAdapter.Entry {

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(MatchEntryHolder.class, R.layout.notification_settings_match_message);

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class MatchEntryHolder extends EntryRecyclerViewAdapter.EntryHolder<MatchEntry> {

        Spinner mMode;

        public MatchEntryHolder(View itemView) {
            super(itemView);

            mMode = (Spinner) itemView.findViewById(R.id.match_mode);
            ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                    itemView.getContext(), R.array.notification_match_modes,
                    android.R.layout.simple_spinner_item);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mMode.setAdapter(spinnerAdapter);
        }

        @Override
        public void bind(MatchEntry entry) {
            //
        }

    }


    public static class AddRuleEntry extends EntryRecyclerViewAdapter.Entry {

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(AddRuleEntryHolder.class, R.layout.notification_settings_add_rule);

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class AddRuleEntryHolder extends EntryRecyclerViewAdapter.EntryHolder<AddRuleEntry> {

        public AddRuleEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView);
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

    public static class RuleEntryHolder extends EntryRecyclerViewAdapter.EntryHolder<RuleEntry>
            implements CompoundButton.OnCheckedChangeListener {

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
            super(itemView);

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
                    getEntry().mEntry.server = mSpinnerOptionUUIDs.get(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // stub
                }
            });
            mChannels.addChipListener(new ChipsEditText.ChipListener() {
                private void update() {
                    if (mChannels.getItemCount() > 0)
                        getEntry().mEntry.channels = mChannels.getItems();
                    else
                        getEntry().mEntry.channels = null;
                }
                @Override
                public void onChipAdded(String text, int index) {
                    update();
                }
                @Override
                public void onChipRemoved(int index) {
                    update();
                }
            });
            mNicks.addChipListener(new ChipsEditText.ChipListener() {
                private void update() {
                    if (mNicks.getItemCount() > 0)
                        getEntry().mEntry.nicks = mNicks.getItems();
                    else
                        getEntry().mEntry.nicks = null;
                }
                @Override
                public void onChipAdded(String text, int index) {
                    update();
                }
                @Override
                public void onChipRemoved(int index) {
                    update();
                }
            });
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            getEntry().mEntry.matchChannelMessages = mChannelMessages.isChecked();
            getEntry().mEntry.matchChannelNotices = mChannelNotices.isChecked();
            getEntry().mEntry.matchDirectMessages = mDirectMessages.isChecked();
            getEntry().mEntry.matchDirectNotices = mDirectNotices.isChecked();
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
            mChannelMessages.setOnCheckedChangeListener(null);
            mChannelNotices.setOnCheckedChangeListener(null);
            mDirectMessages.setOnCheckedChangeListener(null);
            mDirectNotices.setOnCheckedChangeListener(null);
            mChannelMessages.setChecked(entry.mEntry.matchChannelMessages);
            mChannelNotices.setChecked(entry.mEntry.matchChannelNotices);
            mDirectMessages.setChecked(entry.mEntry.matchDirectMessages);
            mDirectNotices.setChecked(entry.mEntry.matchDirectNotices);
            mChannelMessages.setOnCheckedChangeListener(this);
            mChannelNotices.setOnCheckedChangeListener(this);
            mDirectMessages.setOnCheckedChangeListener(this);
            mDirectNotices.setOnCheckedChangeListener(this);
        }

    }

    public static class CollapsedRuleEntryHolder extends EntryRecyclerViewAdapter.EntryHolder<RuleEntry> {

        private RuleEntry mEntry;
        private TextView mDesc;

        public CollapsedRuleEntryHolder(View itemView) {
            super(itemView);
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
