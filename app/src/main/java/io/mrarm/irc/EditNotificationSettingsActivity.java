package io.mrarm.irc;

import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ArrayAdapter;
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
        RuleEntry testRule = new RuleEntry();
        testRule.mCollapsed = false;
        mAdapter.add(testRule);
        mAdapter.add(new RuleEntry());
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

        public AddRuleEntryHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener((View v) -> {
                //
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
        UUID mServer = null;
        List<String> mChannels;
        List<String> mNicks;

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

    public static class RuleEntryHolder extends EntryRecyclerViewAdapter.EntryHolder<RuleEntry> {

        private RuleEntry mEntry;
        private Spinner mServerSpinner;
        private ChipsEditText mChannels;
        private ChipsEditText mNicks;
        private View mChannelsCtr;

        public RuleEntryHolder(View itemView) {
            super(itemView);

            mServerSpinner = (Spinner) itemView.findViewById(R.id.server);
            mChannels = (ChipsEditText) itemView.findViewById(R.id.channels);
            mNicks = (ChipsEditText) itemView.findViewById(R.id.nicks);

            mChannelsCtr = itemView.findViewById(R.id.channels_ctr);
            mChannelsCtr.setVisibility(View.GONE);

            itemView.findViewById(R.id.expand).setOnClickListener((View view) -> {
                mEntry.setCollapsed(true);
            });
            mChannels.addChipListener(new ChipsEditText.ChipListener() {
                private void update() {
                    mEntry.mChannels = mChannels.getItems();
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
                    mEntry.mNicks = mNicks.getItems();
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
        public void bind(RuleEntry entry) {
            mEntry = entry;
            List<String> options = new ArrayList<>();
            options.add(mServerSpinner.getContext().getString(R.string.value_any));
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    mServerSpinner.getContext(), android.R.layout.simple_spinner_item, options);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mServerSpinner.setAdapter(spinnerAdapter);
            if (entry.mChannels != null)
                mChannels.setItems(entry.mChannels);
            else
                mChannels.clearItems();
            if (entry.mNicks != null)
                mNicks.setItems(entry.mNicks);
            else
                mNicks.clearItems();
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
            if (entry.mServer != null) {
                ServerConfigData s = ServerConfigManager.getInstance(mDesc.getContext()).findServer(entry.mServer);
                summaryText.append(s != null ? s.name : "null");
                summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_delim));
                if (entry.mChannels != null && entry.mChannels.size() > 0) {
                    if (entry.mChannels.size() > 1) {
                        summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_multi_channels, entry.mChannels.size()));
                    } else {
                        summaryText.append(entry.mChannels.get(0));
                    }
                } else {
                    summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_any_channel));
                }
            } else {
                summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_any_server));
            }
            summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_delim));
            if (entry.mNicks != null && entry.mNicks.size() > 0) {
                if (entry.mNicks.size() > 1) {
                    summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_multi_nicks, entry.mNicks.size()));
                } else {
                    summaryText.append(entry.mNicks.get(0));
                }
            } else {
                summaryText.append(mDesc.getContext().getString(R.string.notification_rule_summary_any_nick));
            }
            mDesc.setText(summaryText.toString());
        }

    }

}
