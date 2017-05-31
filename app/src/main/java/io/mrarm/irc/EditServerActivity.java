package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import io.mrarm.irc.util.ExpandIconStateHelper;
import io.mrarm.irc.util.StaticLabelTextInputLayout;

public class EditServerActivity extends AppCompatActivity {

    private static String TAG = "EditServerActivity";

    public static String ARG_SERVER_UUID = "server_uuid";

    private ServerConfigData mEditServer;
    private EditText mServerName;
    private EditText mServerAddress;
    private EditText mServerPort;
    private CheckBox mServerSSL;
    private EditText mServerPass;
    private StaticLabelTextInputLayout mServerPassCtr;
    private View mServerPassReset;
    private EditText mServerNick;
    private EditText mServerUser;
    private EditText mServerRealname;
    private EditText mServerChannels;

    private View mServerUserExpandIcon;
    private View mServerUserExpandContent;

    public static Intent getLaunchIntent(Context context, ServerConfigData data) {
        Intent intent = new Intent(context, EditServerActivity.class);
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, data.uuid.toString());
        intent.putExtras(args);
        return intent;
    }

    private TextWatcher mResetPasswordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                mServerPassReset.setVisibility(View.GONE);
                mServerPassCtr.setPasswordVisibilityToggleEnabled(true);
            } else {
                mServerPassReset.setVisibility(View.VISIBLE);
                mServerPassCtr.setPasswordVisibilityToggleEnabled(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uuidString = getIntent().getStringExtra(ARG_SERVER_UUID);
        if (uuidString != null)
            mEditServer = ServerConfigManager.getInstance(this).findServer(UUID.fromString(uuidString));

        setContentView(R.layout.activity_edit_server);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mServerName = (EditText) findViewById(R.id.server_name);
        mServerAddress = (EditText) findViewById(R.id.server_address_name);
        mServerPort = (EditText) findViewById(R.id.server_address_port);
        mServerSSL = (CheckBox) findViewById(R.id.server_ssl_checkbox);
        mServerPass = (EditText) findViewById(R.id.server_password);
        mServerPassCtr = (StaticLabelTextInputLayout) findViewById(R.id.server_password_ctr);
        mServerPassReset = findViewById(R.id.server_password_reset);
        mServerNick = (EditText) findViewById(R.id.server_nick);
        mServerUser = (EditText) findViewById(R.id.server_user);
        mServerRealname = (EditText) findViewById(R.id.server_realname);
        mServerChannels = (EditText) findViewById(R.id.server_channels);

        mServerUserExpandIcon = findViewById(R.id.server_user_expand);
        mServerUserExpandContent = findViewById(R.id.server_user_expand_content);
        mServerUserExpandIcon.setOnClickListener((View view) -> {
            mServerUserExpandContent.setVisibility(mServerUserExpandContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            ExpandIconStateHelper.animateSetExpanded(mServerUserExpandIcon, mServerUserExpandContent.getVisibility() == View.VISIBLE);
        });

        mServerPassReset.setOnClickListener((View view) -> {
            mServerPassCtr.setForceShowHint(false);
            mServerPassReset.setVisibility(View.GONE);
            mServerPassCtr.setPasswordVisibilityToggleEnabled(true);
            mServerPass.removeTextChangedListener(mResetPasswordWatcher);
        });

        if (mEditServer != null) {
            mServerName.setText(mEditServer.name);
            mServerAddress.setText(mEditServer.address);
            mServerPort.setText(String.valueOf(mEditServer.port));
            mServerSSL.setChecked(mEditServer.ssl);

            if (mEditServer.pass != null) {
                mServerPassReset.setVisibility(View.VISIBLE);
                mServerPassCtr.setForceShowHint(true);
                mServerPass.setHint(R.string.server_password_unchanged);
                mServerPass.addTextChangedListener(mResetPasswordWatcher);
                mServerPassCtr.setPasswordVisibilityToggleEnabled(false);
            }

            if (mEditServer.autojoinChannels != null)
                mServerChannels.setText(TextUtils.join(" ", mEditServer.autojoinChannels));

            mServerNick.setText(mEditServer.nick);
            if (mEditServer.user != null || mEditServer.realname != null) {
                mServerUser.setText(mEditServer.user);
                mServerRealname.setText(mEditServer.realname);
                mServerUserExpandContent.setVisibility(View.VISIBLE);
                ExpandIconStateHelper.setExpanded(mServerUserExpandIcon, true);
            }
        } else {
            getSupportActionBar().setTitle(R.string.add_server);
            findViewById(R.id.server_ssl_certs).setVisibility(View.GONE);
        }
    }

    private void save() {
        if (mEditServer == null) {
            mEditServer = new ServerConfigData();
            mEditServer.uuid = UUID.randomUUID();
        }
        mEditServer.name = mServerName.getText().toString();
        mEditServer.address = mServerAddress.getText().toString();
        mEditServer.port = Integer.parseInt(mServerPort.getText().toString());
        mEditServer.ssl = mServerSSL.isChecked();
        mEditServer.nick = mServerNick.getText().length() > 0 ? mServerNick.getText().toString() : null;
        mEditServer.user = mServerUser.getText().length() > 0 ? mServerUser.getText().toString() : null;
        mEditServer.realname = mServerRealname.getText().length() > 0 ? mServerRealname.getText().toString() : null;
        if (mServerPass.getVisibility() == View.VISIBLE)
            mEditServer.pass = mServerPass.getText().toString();
        mEditServer.autojoinChannels = Arrays.asList(mServerChannels.getText().toString().split(" "));
        try {
            ServerConfigManager.getInstance(this).saveServer(mEditServer);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save server info");
            e.printStackTrace();

            Toast.makeText(this, R.string.server_save_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done || id == android.R.id.home) {
            if (id == R.id.action_done) {
                save();
            }

            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
