package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.IRCConnectionRequest;
import io.mrarm.chatlib.test.TestApiImpl;
import io.mrarm.irc.drawer.DrawerHelper;

public class MainActivity extends AppCompatActivity {

    private static final String ARG_SERVER_UUID = "server_uuid";
    private static final String ARG_CHANNEL_NAME = "channel";

    private DrawerLayout mDrawerLayout;
    private DrawerHelper mDrawerHelper;

    private static ServerConnectionInfo mTestConnection;

    public static Intent getLaunchIntent(Context context, ServerConnectionInfo server, String channel) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(ARG_SERVER_UUID, server.getUUID().toString());
        if (channel != null)
            intent.putExtra(ARG_CHANNEL_NAME, channel);
        return intent;
    }

    private void createTestFileConnection() {
        if (mTestConnection != null)
            return;

        TestApiImpl api = new TestApiImpl("test-user");

        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.testdata)));
        try {
            api.readTestChatLog(reader);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        mTestConnection = new ServerConnectionInfo(ServerConnectionManager.getInstance(), UUID.randomUUID(), "Test Connection", api);
        ServerConnectionManager.getInstance().addConnection(mTestConnection);
        mTestConnection.setConnected(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IRCService.start(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createTestFileConnection();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerHelper = new DrawerHelper(this);
        mDrawerHelper.registerListeners();

        mDrawerHelper.setChannelClickListener((ServerConnectionInfo server, String channel) -> {
            mDrawerLayout.closeDrawers();
            Fragment f = getCurrentFragment();
            if (f != null && f instanceof ChatFragment && ((ChatFragment) f).getConnectionInfo() == server)
                ((ChatFragment) f).setCurrentChannel(channel);
            else
                openServer(server, channel);
        });
        mDrawerHelper.getManageServersItem().setOnClickListener((View v) -> {
            mDrawerLayout.closeDrawers();
            openManageServers();
        });

        String serverUUID = getIntent().getStringExtra(ARG_SERVER_UUID);
        if (serverUUID != null) {
            ServerConnectionInfo server = ServerConnectionManager.getInstance().getConnection(UUID.fromString(serverUUID));
            openServer(server, getIntent().getStringExtra(ARG_CHANNEL_NAME));
        } else {
            openManageServers();
        }
    }

    @Override
    protected void onDestroy() {
        mDrawerHelper.unregisterListeners();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WarningDisplayContext.setActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WarningDisplayContext.setActivity(null);
    }

    public void addActionBarDrawerToggle(Toolbar toolbar) {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        // mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.onDrawerClosed(mDrawerLayout);
    }

    public void openServer(ServerConnectionInfo server, String channel) {
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.content_frame, ChatFragment.newInstance(server, channel))
                .commit();
        mDrawerHelper.setSelectedChannel(server, channel);
    }

    public void openManageServers() {
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.content_frame, ServerListFragment.newInstance())
                .commit();
        mDrawerHelper.setSelectedMenuItem(mDrawerHelper.getManageServersItem());
    }

    public DrawerHelper getDrawerHelper() {
        return mDrawerHelper;
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
