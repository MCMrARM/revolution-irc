package io.mrarm.irc;

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

    private EditText mServerName;
    private EditText mServerAddress;
    private EditText mServerPort;
    private CheckBox mServerSSL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_server);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mServerName = (EditText) findViewById(R.id.server_name);
        mServerAddress = (EditText) findViewById(R.id.server_address_name);
        mServerPort = (EditText) findViewById(R.id.server_address_port);
        mServerSSL = (CheckBox) findViewById(R.id.server_ssl_checkbox);

        findViewById(R.id.server_ssl_certs).setVisibility(View.GONE);
    }

    private void save() {
        ServerConfigData data = new ServerConfigData();
        data.uuid = UUID.randomUUID();
        data.name = mServerName.getText().toString();
        data.address = mServerAddress.getText().toString();
        data.port = Integer.parseInt(mServerPort.getText().toString());
        data.ssl = mServerSSL.isChecked();
        try {
            ServerConfigManager.getInstance(this).saveServer(data);
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
