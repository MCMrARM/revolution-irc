package io.mrarm.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class EditNotificationSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_notification_settings);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SettingsListAdapter adapter = new SettingsListAdapter();
        adapter.add(new SettingsListAdapter.SimpleEntry(getString(R.string.notification_sound), getString(R.string.value_default)));
        adapter.add(new SettingsListAdapter.SimpleEntry(getString(R.string.notification_vibration), getString(R.string.value_default)));
        adapter.add(new SettingsListAdapter.SimpleEntry(getString(R.string.notification_priority), getString(R.string.value_default)));
        adapter.add(new SettingsListAdapter.SimpleEntry(getString(R.string.notification_color), getString(R.string.value_default)));
        recyclerView.setAdapter(adapter);
    }

}
