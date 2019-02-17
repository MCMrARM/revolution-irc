package io.mrarm.irc.newui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import io.mrarm.irc.DCCActivity;
import io.mrarm.irc.IRCApplication;
import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;
import io.mrarm.irc.ThemedActivity;

public class MainActivity extends ThemedActivity {

    private SlideableFragmentContainer mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newui_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        mContainer = findViewById(R.id.container);
        mContainer.setFragmentManager(getSupportFragmentManager());
        mContainer.push(new MainFragment());
    }

    public SlideableFragmentContainer getContainer() {
        return mContainer;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_server_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dcc_transfers) {
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
}
