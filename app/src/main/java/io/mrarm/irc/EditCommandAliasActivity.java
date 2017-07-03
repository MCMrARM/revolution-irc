package io.mrarm.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class EditCommandAliasActivity extends AppCompatActivity {

    public static final String ARG_ALIAS_NAME = "alias_name";

    private EditText mName;
    private Spinner mTypeSpinner;
    private EditText mChannel;
    private View mChannelCtr;
    private EditText mText;

    private CommandAliasManager.CommandAlias mEditingAlias;
    private boolean mCreatingNew = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_command_alias);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String aliasName = getIntent().getStringExtra(ARG_ALIAS_NAME);
        if (aliasName != null) {
            for (CommandAliasManager.CommandAlias alias :
                    CommandAliasManager.getInstance(this).getUserAliases()) {
                if (alias.name.equalsIgnoreCase(aliasName)) {
                    mEditingAlias = alias;
                    mCreatingNew = false;
                    break;
                }
            }
        }

        mName = (EditText) findViewById(R.id.name);
        mTypeSpinner = (Spinner) findViewById(R.id.type);
        mChannel = (EditText) findViewById(R.id.channel);
        mChannelCtr = findViewById(R.id.channel_ctr);
        mText = (EditText) findViewById(R.id.text);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.edit_command_alias_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeSpinner.setAdapter(spinnerAdapter);

        mTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mChannelCtr.setVisibility(position == CommandAliasManager.CommandAlias.MODE_MESSAGE
                        ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mTypeSpinner.setSelection(0);

        if (mEditingAlias != null) {
            mName.setText(mEditingAlias.name);
            mTypeSpinner.setSelection(mEditingAlias.mode);
            mChannel.setText(mEditingAlias.channel);
            mText.setText(mEditingAlias.text);
        } else {
            getSupportActionBar().setTitle(R.string.title_activity_add_command_alias);
        }
    }

    public boolean save() {
        if (mEditingAlias == null)
            mEditingAlias = new CommandAliasManager.CommandAlias();
        mEditingAlias.mode = mTypeSpinner.getSelectedItemPosition();
        CommandAliasManager.CommandAlias conflict =
                CommandAliasManager.getInstance(this).findCommandAlias(mEditingAlias.name);
        if (conflict != null && conflict != mEditingAlias) {
            Toast.makeText(this, R.string.edit_command_alias_error_name_collision,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        mEditingAlias.name = mName.getText().toString();
        if (mEditingAlias.mode == CommandAliasManager.CommandAlias.MODE_MESSAGE)
            mEditingAlias.channel = mChannel.getText().toString();
        mEditingAlias.text = mText.getText().toString();

        if (mCreatingNew)
            CommandAliasManager.getInstance(this).getUserAliases().add(mEditingAlias);
        CommandAliasManager.getInstance(this).saveUserSettings();
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
