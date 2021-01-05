package io.mrarm.irc;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCUtils;
import io.mrarm.irc.chat.ChannelInfoAdapter;
import io.mrarm.irc.chat.ChatFragment;
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.ChatSettings;
import io.mrarm.irc.dialog.UserSearchDialog;
import io.mrarm.irc.drawer.DrawerHelper;
import io.mrarm.irc.util.NightModeRecreateHelper;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.WarningHelper;
import io.mrarm.irc.view.ChipsEditText;
import io.mrarm.irc.view.LockableDrawerLayout;

public class MainActivity extends ThemedActivity implements IRCApplication.ExitCallback {

    public static final String ARG_SERVER_UUID = "server_uuid";
    public static final String ARG_CHANNEL_NAME = "channel";
    public static final String ARG_MESSAGE_ID = "message_id";
    public static final String ARG_MANAGE_SERVERS = "manage_servers";

    private static final int REQUEST_CODE_PICK_FILE_DCC = 100;
    private static final int REQUEST_CODE_DCC_FOLDER_PERMISSION = 101;
    private static final int REQUEST_CODE_DCC_STORAGE_PERMISSION = 102;

    private NightModeRecreateHelper mNightModeHelper = new NightModeRecreateHelper(this);
    private LockableDrawerLayout mDrawerLayout;
    private DrawerHelper mDrawerHelper;
    private Toolbar mToolbar;
    private View mFakeToolbar;
    private boolean mBackReturnToServerList;
    private Dialog mCurrentDialog;
    private ChannelInfoAdapter mChannelInfoAdapter;
    private boolean mAppExiting;
    private DCCManager.ActivityDialogHandler mDCCDialogHandler =
            new DCCManager.ActivityDialogHandler(this, REQUEST_CODE_DCC_FOLDER_PERMISSION,
                    REQUEST_CODE_DCC_STORAGE_PERMISSION);

    public static Intent getLaunchIntent(Context context, ServerConnectionInfo server, String channel, String messageId) {
        Intent intent = new Intent(context, MainActivity.class);
        if (server != null)
            intent.putExtra(ARG_SERVER_UUID, server.getUUID().toString());
        if (channel != null)
            intent.putExtra(ARG_CHANNEL_NAME, channel);
        if (messageId != null)
            intent.putExtra(ARG_MESSAGE_ID, messageId);
        return intent;
    }

    public static Intent getLaunchIntent(Context context, ServerConnectionInfo server, String channel) {
        return getLaunchIntent(context, server, channel, null);
    }

    public static Intent getLaunchIntentForManageServers(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(ARG_MANAGE_SERVERS, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ServerConnectionManager.getInstance(this);
        WarningHelper.setAppContext(getApplicationContext());

        mAppExiting = false;
        ((IRCApplication) getApplication()).addExitCallback(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFakeToolbar = findViewById(R.id.fake_toolbar);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        mDrawerHelper = new DrawerHelper(this);
        mDrawerHelper.registerListeners();

        mDrawerHelper.setChannelClickListener((ServerConnectionInfo server, String channel) -> {
            mDrawerLayout.closeDrawers();
            Fragment f = getCurrentFragment();
            if (f != null && f instanceof ChatFragment && ((ChatFragment) f).getConnectionInfo() == server)
                ((ChatFragment) f).setCurrentChannel(channel, null);
            else
                openServer(server, channel);
        });
        mDrawerHelper.getManageServersItem().setOnClickListener((View v) -> {
            mDrawerLayout.closeDrawers();
            openManageServers();
        });

        if (AppSettings.isDrawerPinned())
            mDrawerLayout.setLocked(true);

        mChannelInfoAdapter = new ChannelInfoAdapter();
        RecyclerView membersRecyclerView = findViewById(R.id.members_list);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersRecyclerView.setAdapter(mChannelInfoAdapter);
        setChannelInfoDrawerVisible(false);

        if (savedInstanceState != null && savedInstanceState.getString(ARG_SERVER_UUID) != null)
            return;

        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        String serverUUID = intent.getStringExtra(ARG_SERVER_UUID);
        ServerConnectionInfo server = null;
        if (serverUUID != null)
            server = ServerConnectionManager.getInstance(this).getConnection(
                    UUID.fromString(serverUUID));
        if (server != null) {
            ChatFragment fragment = openServer(server, intent.getStringExtra(ARG_CHANNEL_NAME),
                    intent.getStringExtra(ARG_MESSAGE_ID), false);
            if (Intent.ACTION_SEND.equals(intent.getAction()) &&
                    "text/plain".equals(intent.getType())) {
                setFragmentShareText(fragment, intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        } else if (intent.getBooleanExtra(ARG_MANAGE_SERVERS, false) ||
                getCurrentFragment() == null) {
            openManageServers();
        } else {
            mBackReturnToServerList = false;
        }
    }

    private void setFragmentShareText(ChatFragment fragment, String text) {
        if (fragment.getSendMessageHelper() != null) {
            fragment.getSendMessageHelper().setMessageText(text);
        } else {
            Bundle bundle = fragment.getArguments();
            bundle.putString(ChatFragment.ARG_SEND_MESSAGE_TEXT, text);
            fragment.setArguments(bundle);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(this,
                new int[] { R.attr.actionBarSize });
        ViewGroup.LayoutParams params = mFakeToolbar.getLayoutParams();
        params.height = ta.getDimensionPixelSize(R.attr.actionBarSize, 0);
        mFakeToolbar.setLayoutParams(params);
        ta.recycle();
        if (mToolbar != null) {
            ViewGroup group = (ViewGroup) mToolbar.getParent();
            int i = group.indexOfChild(mToolbar);
            group.removeViewAt(i);
            Toolbar replacement = new Toolbar(group.getContext());
            replacement.setPopupTheme(mToolbar.getPopupTheme());
            AppBarLayout.LayoutParams toolbarParams = new AppBarLayout.LayoutParams(
                    AppBarLayout.LayoutParams.MATCH_PARENT, params.height);
            replacement.setLayoutParams(toolbarParams);
            for (int j = 0; j < mToolbar.getChildCount(); j++) {
                View v = mToolbar.getChildAt(j);
                if (v instanceof TabLayout) {
                    mToolbar.removeViewAt(j);
                    replacement.addView(v);
                    j--;
                }
            }
            group.addView(replacement, i);
            setSupportActionBar(replacement);
            addActionBarDrawerToggle(replacement);
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
        ((IRCApplication) getApplication()).removeExitCallback(this);
        mDrawerHelper.unregisterListeners();
        dismissFragmentDialog();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WarningHelper.setActivity(this);
        mDCCDialogHandler.onResume();
        if (getCurrentFragment() instanceof ChatFragment && !ServerConnectionManager
                .getInstance(this).hasConnection(((ChatFragment) getCurrentFragment())
                        .getConnectionInfo().getUUID())) {
            openManageServers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        WarningHelper.setActivity(null);
        mDCCDialogHandler.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
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
        LockableDrawerLayout.ActionBarDrawerToggle toggle = new LockableDrawerLayout.ActionBarDrawerToggle(
                toolbar, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        // mDrawerLayout.addDrawerListener(toggle);
    }

    public ChatFragment openServer(ServerConnectionInfo server, String channel, String messageId,
                                   boolean fromServerList) {
        dismissFragmentDialog();
        setChannelInfoDrawerVisible(false);
        ChatFragment fragment;
        if (getCurrentFragment() instanceof ChatFragment &&
                ((ChatFragment) getCurrentFragment()).getConnectionInfo() == server) {
            fragment = (ChatFragment) getCurrentFragment();
            fragment.setCurrentChannel(channel, messageId);
            setChannelInfoDrawerVisible(false);
        } else {
            fragment = ChatFragment.newInstance(server, channel, messageId);
            getSupportFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }
        mDrawerHelper.setSelectedChannel(server, channel);
        if (fromServerList)
            mBackReturnToServerList = true;
        return fragment;
    }

    public ChatFragment openServer(ServerConnectionInfo server, String channel) {
        return openServer(server, channel, null, false);
    }

    public void openManageServers() {
        dismissFragmentDialog();
        setChannelInfoDrawerVisible(false);
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

    public Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content_frame);
    }

    public void setFragmentDialog(Dialog dialog) {
        if (mCurrentDialog != null) {
            mCurrentDialog.setOnDismissListener(null);
            mCurrentDialog.dismiss();
        }
        mCurrentDialog = dialog;
        mCurrentDialog.setOnDismissListener((DialogInterface di) -> {
            if (mCurrentDialog == dialog)
                mCurrentDialog = null;
        });
    }

    public void dismissFragmentDialog() {
        if (mCurrentDialog != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(mCurrentDialog.getWindow().getDecorView()
                    .getApplicationWindowToken(), 0);

            mCurrentDialog.setOnDismissListener(null);
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }
    }

    public void setCurrentChannelInfo(ServerConnectionInfo server, String topic, String topicSetBy,
                                      Date topicSetOn, List<NickWithPrefix> members) {
        if (mChannelInfoAdapter == null)
            return;
        mChannelInfoAdapter.setData(server, topic, topicSetBy, topicSetOn, members);
        setChannelInfoDrawerVisible(topic != null || (members != null && members.size() > 0));
    }

    public void setChannelInfoDrawerVisible(boolean visible) {
        if (visible) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);
            mDrawerLayout.closeDrawer(GravityCompat.END);
        }
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
        } else if (getCurrentFragment() instanceof ServerListFragment) {
            getMenuInflater().inflate(R.menu.menu_server_list, menu);
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
                    menu.findItem(R.id.action_close).setVisible(false);
                    menu.findItem(R.id.action_disconnect).setVisible(true);
                    menu.findItem(R.id.action_disconnect_and_close).setVisible(true);
                } else {
                    menu.findItem(R.id.action_reconnect).setVisible(true);
                    menu.findItem(R.id.action_close).setVisible(true);
                    menu.findItem(R.id.action_disconnect).setVisible(false);
                    menu.findItem(R.id.action_disconnect_and_close).setVisible(false);
                }
                hasChanges = true;
            }
            menu.findItem(R.id.action_members).setVisible(
                    mDrawerLayout.getDrawerLockMode(GravityCompat.END) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            if (fragment.getSendMessageHelper().hasSendMessageTextSelection() !=
                    menu.findItem(R.id.action_format).isVisible()) {
                menu.findItem(R.id.action_format).setVisible(fragment.getSendMessageHelper()
                        .hasSendMessageTextSelection());
                hasChanges = true;
            }
            MenuItem partItem = menu.findItem(R.id.action_part_channel);
            boolean inDirectChat = false;
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
                inDirectChat = true;
            } else {
                if (partItem.isVisible() || !partItem.getTitle().equals(getString(R.string.action_part_channel)))
                    hasChanges = true;
                partItem.setVisible(true);
                partItem.setTitle(R.string.action_part_channel);
            }
            boolean wasDccSendVisible = menu.findItem(R.id.action_dcc_send).isVisible();
            boolean dccSendVisible = ChatSettings.isDccSendVisible() && connected && inDirectChat;
            if (dccSendVisible != wasDccSendVisible) {
                menu.findItem(R.id.action_dcc_send).setVisible(dccSendVisible);
                hasChanges = true;
            }
        }
        return super.onPrepareOptionsMenu(menu) | hasChanges;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_join_channel) {
            View v = LayoutInflater.from(this).inflate(R.layout.dialog_chip_edit_text, null);
            ChipsEditText editText = v.findViewById(R.id.chip_edit_text);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.action_join_channel)
                    .setView(v)
                    .setPositiveButton(R.string.action_ok, (DialogInterface d, int which) -> {
                        editText.clearFocus();
                        String[] channels = editText.getItems();
                        if (channels.length == 0)
                            return;
                        ChatFragment currentChat = (ChatFragment) getCurrentFragment();
                        ChatApi api = currentChat.getConnectionInfo().getApiInstance();
                        currentChat.setAutoOpenChannel(channels[0]);
                        api.joinChannels(Arrays.asList(channels), null, null);
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
            setFragmentDialog(dialog);
        } else if (id == R.id.action_message_user) {
            UserSearchDialog dialog = new UserSearchDialog(this, ((ChatFragment)
                    getCurrentFragment()).getConnectionInfo());
            dialog.show();
            setFragmentDialog(dialog);
        } else if (id == R.id.action_part_channel) {
            ChatApi api = ((ChatFragment) getCurrentFragment()).getConnectionInfo().getApiInstance();
            String channel = ((ChatFragment) getCurrentFragment()).getCurrentChannel();
            if (channel != null)
                api.leaveChannel(channel, AppSettings.getDefaultPartMessage(), null, null);
        } else if (id == R.id.action_dcc_send) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE_DCC);
        } else if (id == R.id.action_members) {
            mDrawerLayout.openDrawer(GravityCompat.END);
        } else if (id == R.id.action_ignore_list) {
            ServerConnectionInfo info = ((ChatFragment) getCurrentFragment()).getConnectionInfo();
            Intent intent = new Intent(this, IgnoreListActivity.class);
            intent.putExtra(IgnoreListActivity.ARG_SERVER_UUID, info.getUUID().toString());
            startActivity(intent);
        } else if (id == R.id.action_disconnect) {
            ((ChatFragment) getCurrentFragment()).getConnectionInfo().disconnect();
        } else if (id == R.id.action_disconnect_and_close || id == R.id.action_close) {
            ServerConnectionInfo info = ((ChatFragment) getCurrentFragment()).getConnectionInfo();
            info.disconnect();
            ServerConnectionManager.getInstance(this).removeConnection(info);
            openManageServers();
        } else if (id == R.id.action_reconnect) {
            ((ChatFragment) getCurrentFragment()).getConnectionInfo().connect();
        } else if (id == R.id.action_format) {
            ((ChatFragment) getCurrentFragment()).getSendMessageHelper().setFormatBarVisible(true);
        } else if (id == R.id.action_dcc_transfers) {
            startActivity(new Intent(this, DCCActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_exit) {
            ((IRCApplication) getApplication()).requestExit();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_FILE_DCC && data != null && data.getData() != null) {
            Uri uri = data.getData();
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            String name = DCCUtils.escapeFilename(cursor.getString(nameIndex));
            long size = cursor.isNull(sizeIndex) ? -1 : cursor.getLong(sizeIndex);

            String channel = ((ChatFragment) getCurrentFragment()).getCurrentChannel();
            try {
                ParcelFileDescriptor desc = getContentResolver().openFileDescriptor(uri, "r");
                if (desc == null)
                    throw new IOException();
                if (size == -1)
                    size = desc.getStatSize();

                DCCServer.FileChannelFactory fileFactory = () -> new FileInputStream(
                        desc.getFileDescriptor()).getChannel().position(0);
                DCCManager.getInstance(this).startUpload(((ChatFragment) getCurrentFragment())
                        .getConnectionInfo(), channel, fileFactory, name, size);
            } catch (IOException e) {
                Toast.makeText(this, R.string.error_file_open, Toast.LENGTH_SHORT).show();
                return;
            }
            return;
        }
        mDCCDialogHandler.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mDCCDialogHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onAppExiting() {
        mAppExiting = true;
        if (getCurrentFragment() instanceof ServerListFragment)
            ((ServerListFragment) getCurrentFragment()).getAdapter().unregisterListeners();
        getDrawerHelper().unregisterListeners();
    }

    public boolean isAppExiting() {
        return mAppExiting;
    }
}
