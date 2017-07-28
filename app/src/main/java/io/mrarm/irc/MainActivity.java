package io.mrarm.irc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;

import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.test.TestApiImpl;
import io.mrarm.irc.chat.ChatFragment;
import io.mrarm.irc.dialog.UserSearchDialog;
import io.mrarm.irc.drawer.DrawerHelper;
import io.mrarm.irc.util.NightModeRecreateHelper;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.WarningHelper;
import io.mrarm.irc.view.ChipsEditText;

public class MainActivity extends AppCompatActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";
    public static final String ARG_CHANNEL_NAME = "channel";

    private NightModeRecreateHelper mNightModeHelper = new NightModeRecreateHelper(this);
    private DrawerLayout mDrawerLayout;
    private DrawerHelper mDrawerHelper;
    private Toolbar mToolbar;
    private boolean mBackReturnToServerList;

    private static ServerConnectionInfo mTestConnection;

    public static Intent getLaunchIntent(Context context, ServerConnectionInfo server, String channel) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(ARG_SERVER_UUID, server.getUUID().toString());
        if (channel != null)
            intent.putExtra(ARG_CHANNEL_NAME, channel);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

        mTestConnection = new ServerConnectionInfo(ServerConnectionManager.getInstance(this), UUID.randomUUID(), "Test Connection", api);
        ServerConnectionManager.getInstance(this).addConnection(mTestConnection);
        mTestConnection.setConnected(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WarningHelper.setAppContext(getApplicationContext());
        IRCService.start(this);

        if (SettingsHelper.getInstance(this).isNightModeEnabled())
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //createTestFileConnection();

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

        if (savedInstanceState != null && savedInstanceState.getString(ARG_SERVER_UUID) != null)
            return;

        String serverUUID = getIntent().getStringExtra(ARG_SERVER_UUID);
        if (serverUUID != null) {
            ServerConnectionInfo server = ServerConnectionManager.getInstance(this).getConnection(UUID.fromString(serverUUID));
            openServer(server, getIntent().getStringExtra(ARG_CHANNEL_NAME));
        } else {
            openManageServers();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (getCurrentFragment() instanceof ChatFragment) {
            ChatFragment chat = ((ChatFragment) getCurrentFragment());
            outState.putString(ARG_SERVER_UUID, chat.getConnectionInfo().getUUID().toString());
            outState.putString(ARG_CHANNEL_NAME, chat.getCurrentChannel());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String serverUUID = savedInstanceState.getString(ARG_SERVER_UUID);
        if (serverUUID != null) {
            ServerConnectionInfo server = ServerConnectionManager.getInstance(this).getConnection(UUID.fromString(serverUUID));
            openServer(server, savedInstanceState.getString(ARG_CHANNEL_NAME));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mNightModeHelper.onStart();
    }

    @Override
    protected void onDestroy() {
        mDrawerHelper.unregisterListeners();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WarningHelper.setActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WarningHelper.setActivity(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String serverUUID = intent.getStringExtra(ARG_SERVER_UUID);
        if (serverUUID != null) {
            ServerConnectionInfo server = ServerConnectionManager.getInstance(this).getConnection(UUID.fromString(serverUUID));
            openServer(server, intent.getStringExtra(ARG_CHANNEL_NAME));
        }
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        mToolbar = toolbar;
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public void addActionBarDrawerToggle(Toolbar toolbar) {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        // mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.onDrawerClosed(mDrawerLayout);
    }

    public void openServer(ServerConnectionInfo server, String channel, boolean fromServerList) {
        if (getCurrentFragment() instanceof ChatFragment &&
                ((ChatFragment) getCurrentFragment()).getConnectionInfo() == server) {
            ((ChatFragment) getCurrentFragment()).setCurrentChannel(channel);
            ((ChatFragment) getCurrentFragment()).closeDrawer();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.content_frame, ChatFragment.newInstance(server, channel))
                    .commit();
        }
        mDrawerHelper.setSelectedChannel(server, channel);
        if (fromServerList)
            mBackReturnToServerList = true;
    }

    public void openServer(ServerConnectionInfo server, String channel) {
        openServer(server, channel, false);
    }

    public void openManageServers() {
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.content_frame, ServerListFragment.newInstance())
                .commit();
        mDrawerHelper.setSelectedMenuItem(mDrawerHelper.getManageServersItem());
        mBackReturnToServerList = false;
    }

    public DrawerHelper getDrawerHelper() {
        return mDrawerHelper;
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    @Override
    public void onBackPressed() {
        if (mBackReturnToServerList) {
            openManageServers();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getCurrentFragment() instanceof ChatFragment) {
            getMenuInflater().inflate(R.menu.menu_chat, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean hasChanges = false;
        if (getCurrentFragment() instanceof ChatFragment) {
            ChatFragment fragment = ((ChatFragment) getCurrentFragment());
            ServerConnectionApi api = ((ServerConnectionApi) fragment.getConnectionInfo().getApiInstance());
            boolean connected = fragment.getConnectionInfo().isConnected();
            boolean wasConnected = !menu.findItem(R.id.action_reconnect).isVisible();
            if (connected != wasConnected) {
                if (connected) {
                    menu.findItem(R.id.action_reconnect).setVisible(false);
                    menu.findItem(R.id.action_disconnect).setVisible(true);
                    menu.findItem(R.id.action_disconnect_and_close).setVisible(true);
                } else {
                    menu.findItem(R.id.action_reconnect).setVisible(true);
                    menu.findItem(R.id.action_disconnect).setVisible(false);
                    menu.findItem(R.id.action_disconnect_and_close).setVisible(false);
                }
                hasChanges = true;
            }
            if (fragment.hasSendMessageTextSelection() !=
                    menu.findItem(R.id.action_format).isVisible()) {
                menu.findItem(R.id.action_format).setVisible(fragment.hasSendMessageTextSelection());
                hasChanges = true;
            }
            MenuItem partItem = menu.findItem(R.id.action_part_channel);
            if (fragment.getCurrentChannel() == null) {
                if (partItem.isVisible())
                    hasChanges = true;
                partItem.setVisible(false);
            } else if (fragment.getCurrentChannel().length() > 0 && !api.getServerConnectionData()
                    .getSupportList().getSupportedChannelTypes().contains(fragment.getCurrentChannel().charAt(0))) {
                if (partItem.isVisible() || !partItem.getTitle().equals(getString(R.string.action_close_direct)))
                    hasChanges = true;
                partItem.setVisible(true);
                partItem.setTitle(R.string.action_close_direct);
            } else {
                if (partItem.isVisible() || !partItem.getTitle().equals(getString(R.string.action_part_channel)))
                    hasChanges = true;
                partItem.setVisible(true);
                partItem.setTitle(R.string.action_part_channel);
            }
        }
        return super.onPrepareOptionsMenu(menu) | hasChanges;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_join_channel) {
            View v = LayoutInflater.from(this).inflate(R.layout.dialog_chip_edit_text, null);
            ChipsEditText editText = (ChipsEditText) v.findViewById(R.id.chip_edit_text);
            editText.startItemEdit();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.action_join_channel)
                    .setView(v)
                    .setPositiveButton(R.string.action_ok, (DialogInterface d, int which) -> {
                        editText.clearFocus();
                        ChatApi api = ((ChatFragment) getCurrentFragment()).getConnectionInfo().getApiInstance();
                        api.joinChannels(editText.getItems(), null, null);
                    })
                    .setNeutralButton(R.string.title_activity_channel_list, (DialogInterface d, int which) -> {
                        ServerConnectionInfo info = ((ChatFragment) getCurrentFragment()).getConnectionInfo();
                        Intent intent = new Intent(this, ChannelListActivity.class);
                        intent.putExtra(ChannelListActivity.ARG_SERVER_UUID, info.getUUID().toString());
                        startActivity(intent);
                    })
                    .create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        } else if (id == R.id.action_message_user) {
            UserSearchDialog dialog = new UserSearchDialog(this, ((ChatFragment)
                    getCurrentFragment()).getConnectionInfo());
            dialog.show();
        } else if (id == R.id.action_part_channel) {
            ChatApi api = ((ChatFragment) getCurrentFragment()).getConnectionInfo().getApiInstance();
            String channel = ((ChatFragment) getCurrentFragment()).getCurrentChannel();
            if (channel != null)
                api.leaveChannel(channel, SettingsHelper.getInstance(this).getDefaultPartMessage(), null, null);
        } else if (id == R.id.action_ignore_list) {
            ServerConnectionInfo info = ((ChatFragment) getCurrentFragment()).getConnectionInfo();
            Intent intent = new Intent(this, IgnoreListActivity.class);
            intent.putExtra(IgnoreListActivity.ARG_SERVER_UUID, info.getUUID().toString());
            startActivity(intent);
        } else if (id == R.id.action_disconnect) {
            ((ChatFragment) getCurrentFragment()).getConnectionInfo().disconnect();
        } else if (id == R.id.action_disconnect_and_close) {
            ServerConnectionInfo info = ((ChatFragment) getCurrentFragment()).getConnectionInfo();
            info.disconnect();
            ServerConnectionManager.getInstance(this).removeConnection(info);
            IRCService.start(this);
            openManageServers();
        } else if (id == R.id.action_reconnect) {
            ((ChatFragment) getCurrentFragment()).getConnectionInfo().connect();
        } else if (id == R.id.action_format) {
            ((ChatFragment) getCurrentFragment()).setFormatBarVisible(true);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
