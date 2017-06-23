package io.mrarm.irc;

import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

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

}
