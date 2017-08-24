package io.mrarm.irc;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;

public class EditIgnoreEntryActivity extends ThemedActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";
    public static final String ARG_ENTRY_INDEX = "edit_index";

    private ServerConfigData mServer;
    private ServerConfigData.IgnoreEntry mEntry;
    private EditText mNick;
    private EditText mUser;
    private EditText mHost;
    private EditText mComment;
    private CheckBox mChannelMessages;
    private CheckBox mChannelNotices;
    private CheckBox mDirectMessages;
    private CheckBox mDirectNotices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_ignore_entry);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        UUID uuid = UUID.fromString(getIntent().getStringExtra(ARG_SERVER_UUID));
        mServer = ServerConfigManager.getInstance(this).findServer(uuid);
        if (getIntent().hasExtra(ARG_ENTRY_INDEX))
            mEntry = mServer.ignoreList.get(getIntent().getIntExtra(ARG_ENTRY_INDEX, -1));

        mNick = findViewById(R.id.nick);
        mUser = findViewById(R.id.user);
        mHost = findViewById(R.id.host);
        mComment = findViewById(R.id.comment);

        mChannelMessages = findViewById(R.id.channel_messages);
        mChannelNotices = findViewById(R.id.channel_notices);
        mDirectMessages = findViewById(R.id.direct_messages);
        mDirectNotices = findViewById(R.id.direct_notices);

        if (mEntry != null) {
            mNick.setText(mEntry.nick);
            mUser.setText(mEntry.user);
            mHost.setText(mEntry.host);
            mComment.setText(mEntry.comment);
            mChannelMessages.setChecked(mEntry.matchChannelMessages);
            mChannelNotices.setChecked(mEntry.matchChannelNotices);
            mDirectMessages.setChecked(mEntry.matchDirectMessages);
            mDirectNotices.setChecked(mEntry.matchDirectNotices);
        } else {
            setTitle(R.string.title_activity_add_ignore_entry);
        }
    }

    public boolean save() {
        if (mEntry == null) {
            mEntry = new ServerConfigData.IgnoreEntry();
            if (mServer.ignoreList == null)
                mServer.ignoreList = new ArrayList<>();
            mServer.ignoreList.add(mEntry);
        }
        mEntry.nick = mNick.getText().length() > 0 ? mNick.getText().toString() : null;
        mEntry.user = mUser.getText().length() > 0 ? mUser.getText().toString() : null;
        mEntry.host = mHost.getText().length() > 0 ? mHost.getText().toString() : null;
        mEntry.comment = mComment.getText().length() > 0 ? mComment.getText().toString() : null;
        mEntry.matchChannelMessages = mChannelMessages.isChecked();
        mEntry.matchChannelNotices = mChannelNotices.isChecked();
        mEntry.matchDirectMessages = mDirectMessages.isChecked();
        mEntry.matchDirectNotices = mDirectNotices.isChecked();
        mEntry.updateRegexes();
        try {
            ServerConfigManager.getInstance(this).saveServer(mServer);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_only_done, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done || id == android.R.id.home) {
            if (id == R.id.action_done) {
                if (!save())
                    return true;
            }

            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
