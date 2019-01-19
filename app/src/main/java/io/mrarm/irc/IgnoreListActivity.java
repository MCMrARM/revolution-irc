package io.mrarm.irc;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import java.util.UUID;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;

public class IgnoreListActivity extends ThemedActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";

    private ServerConfigData mServer;
    private IgnoreListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_list_with_fab);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        UUID uuid = UUID.fromString(getIntent().getStringExtra(ARG_SERVER_UUID));
        mServer = ServerConfigManager.getInstance(this).findServer(uuid);
        if (mServer == null) {
            finish();
            return;
        }
        setTitle(getString(R.string.title_activity_ignore_list_network, mServer.name));

        RecyclerView recyclerView = findViewById(R.id.items);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new IgnoreListAdapter(this, mServer);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                layoutManager.getOrientation()));
        recyclerView.setAdapter(mAdapter);

        findViewById(R.id.add).setOnClickListener((View v) -> {
            Intent intent = new Intent(IgnoreListActivity.this, EditIgnoreEntryActivity.class);
            intent.putExtra(EditIgnoreEntryActivity.ARG_SERVER_UUID, mServer.uuid.toString());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
