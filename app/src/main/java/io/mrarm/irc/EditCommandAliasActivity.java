package io.mrarm.irc;

import android.content.res.ColorStateList;
import android.os.Bundle;
import androidx.core.view.ViewCompat;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.util.StyledAttributesHelper;

public class EditCommandAliasActivity extends ThemedActivity {

    public static final String ARG_ALIAS_NAME = "alias_name";
    public static final String ARG_ALIAS_SYNTAX = "alias_syntax";

    private EditText mName;
    private Spinner mTypeSpinner;
    private EditText mSyntax;
    private EditText mChannel;
    private View mChannelCtr;
    private EditText mText;

    private CommandAliasManager.CommandAlias mEditingAlias;
    private boolean mCreatingNew = true;
    private boolean mReadOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_command_alias);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String aliasName = getIntent().getStringExtra(ARG_ALIAS_NAME);
        String aliasSyntax = getIntent().getStringExtra(ARG_ALIAS_SYNTAX);
        if (aliasName != null && aliasSyntax != null) {
            for (CommandAliasManager.CommandAlias a : CommandAliasManager.getInstance(this).getUserAliases()) {
                if (a.name.equalsIgnoreCase(aliasName) && a.syntax.equals(aliasSyntax)) {
                    mEditingAlias = a;
                    mCreatingNew = false;
                    break;
                }
            }
            if (mEditingAlias == null) {
                for (CommandAliasManager.CommandAlias a : CommandAliasManager.getDefaultAliases()) {
                    if (a.name.equalsIgnoreCase(aliasName) && a.syntax.equals(aliasSyntax)) {
                        mEditingAlias = a;
                        mReadOnly = true;
                        mCreatingNew = false;
                        break;
                    }
                }
            }
        }

        mName = findViewById(R.id.name);
        mTypeSpinner = findViewById(R.id.type);
        mSyntax = findViewById(R.id.syntax);
        mChannel = findViewById(R.id.channel);
        mChannelCtr = findViewById(R.id.channel_ctr);
        mText = findViewById(R.id.text);

        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(this,
                R.layout.simple_spinner_item, android.R.id.text1,
                getResources().getStringArray(R.array.edit_command_alias_types));
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
            mSyntax.setText(mEditingAlias.syntax);
            mChannel.setText(mEditingAlias.channel);
            mText.setText(mEditingAlias.text);

            if (mReadOnly) {
                setEditTextDisabled(mName);
                mTypeSpinner.setEnabled(false);
                setEditTextDisabled(mSyntax);
                setEditTextDisabled(mChannel);
                setEditTextDisabled(mText);

                getSupportActionBar().setTitle(R.string.title_activity_view_command_alias);
            }
        } else {
            getSupportActionBar().setTitle(R.string.title_activity_add_command_alias);
        }
    }

    private static void setEditTextDisabled(EditText editText) {
        editText.setInputType(InputType.TYPE_NULL);
        editText.setTextIsSelectable(true);
        editText.setKeyListener(null);

        editText.setBackgroundResource(R.drawable.edit_text_readonly);
        int color = StyledAttributesHelper.getColor(editText.getContext(), android.R.attr.textColorSecondary, 0);
        ViewCompat.setBackgroundTintList(editText, ColorStateList.valueOf(color));
        ((ViewGroup) editText.getParent()).setAddStatesFromChildren(false);
    }

    public boolean save() {
        if (mReadOnly)
            return false;
        if (mEditingAlias == null)
            mEditingAlias = new CommandAliasManager.CommandAlias();
        mEditingAlias.mode = mTypeSpinner.getSelectedItemPosition();
        String name = mName.getText().toString();
        String syntax = mSyntax.getText().toString();
        CommandAliasManager.CommandAlias conflict = CommandAliasManager.getInstance(this)
                .findCommandAlias(name, syntax);
        if (conflict != null && conflict != mEditingAlias) {
            Toast.makeText(this, R.string.edit_command_alias_error_name_collision,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        mEditingAlias.name = name;
        mEditingAlias.syntax = syntax;
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
        if (mReadOnly)
            return true;
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
