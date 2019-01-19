package io.mrarm.irc.setup;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import io.mrarm.irc.R;

public class BackupPasswordActivity extends SetupBigHeaderActivity {

    public static final String ARG_RESTORE_MODE = "restore_mode";
    public static final String ARG_WAS_INVALID = "was_invalid";

    public static final String RET_PASSWORD = "password";

    public static final int RESULT_CODE_PASSWORD = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSetupContentView(R.layout.activity_backup_password_content);

        TextInputLayout layout = findViewById(R.id.pass_ctr);

        boolean restoreMode = getIntent().getBooleanExtra(ARG_RESTORE_MODE, false);
        if (restoreMode) {
            setTitle(R.string.title_activity_backup_password_restore);
            ((TextView) findViewById(R.id.text)).setText(R.string.backup_restore_password_text);
            if (getIntent().getBooleanExtra(ARG_WAS_INVALID, false))
                layout.setError(getString(R.string.backup_restore_invalid_password));
        }

        EditText userPassword = findViewById(R.id.pass);

        findViewById(R.id.next).setOnClickListener((View v) -> {
            if (restoreMode) {
                if (userPassword.getText().length() == 0) {
                    layout.setError(getString(R.string.backup_restore_password_not_empty));
                    return;
                }
                Intent ret = new Intent();
                ret.putExtra(RET_PASSWORD, userPassword.getText().toString());
                setResult(RESULT_CODE_PASSWORD, ret);
                finish();
                setSlideAnimation(false);
                return;
            }
            Intent intent = new Intent(BackupPasswordActivity.this, BackupProgressActivity.class);
            if (userPassword.getText().length() > 0)
                intent.putExtra(BackupProgressActivity.ARG_USER_PASSWORD, userPassword.getText().toString());
            startNextActivity(intent);
        });
    }

}
