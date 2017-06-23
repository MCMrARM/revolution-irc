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

        private static final int sHolder = EntryRecyclerViewAdapter.registerViewHolder(AddRuleHolder.class, R.layout.notification_settings_add_rule);

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class AddRuleHolder extends EntryRecyclerViewAdapter.EntryHolder<AddRuleEntry> {

        public AddRuleHolder(View itemView) {
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

}
