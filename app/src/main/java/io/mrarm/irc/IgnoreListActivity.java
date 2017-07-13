package io.mrarm.irc;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.UUID;

public class IgnoreListActivity extends AppCompatActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";

    private ServerConfigData mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_list_with_fab);

        UUID uuid = UUID.fromString(getIntent().getStringExtra(ARG_SERVER_UUID));
        mServer = ServerConfigManager.getInstance(this).findServer(uuid);
        if (mServer == null) {
            finish();
            return;
        }
        setTitle(getString(R.string.title_activity_ignore_list_network, mServer.name));

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.items);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        IgnoreListAdapter adapter = new IgnoreListAdapter(this, mServer.ignoreList);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                layoutManager.getOrientation()));
        recyclerView.setAdapter(adapter);
    }

}
