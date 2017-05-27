package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class EditServerActivity extends AppCompatActivity {

    private static String TAG = "EditServerActivity";

    public static String ARG_SERVER_UUID = "server_uuid";

    private ServerConfigData mEditServer;
    private EditText mServerName;
    private EditText mServerAddress;
    private EditText mServerPort;
    private CheckBox mServerSSL;

    public static Intent getLaunchIntent(Context context, ServerConfigData data) {
        Intent intent = new Intent(context, EditServerActivity.class);
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, data.uuid.toString());
        intent.putExtras(args);
        return intent;
    }

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

        if (mEditServer != null) {
            mServerName.setText(mEditServer.name);
            mServerAddress.setText(mEditServer.address);
            mServerPort.setText(String.valueOf(mEditServer.port));
            mServerSSL.setChecked(mEditServer.ssl);
        } else {
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
