package io.mrarm.irc.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import io.mrarm.irc.R;

public class BackupPasswordActivity extends SetupBigHeaderActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSetupContentView(R.layout.activity_backup_password_content);

        EditText userPassword = (EditText) findViewById(R.id.pass);
        findViewById(R.id.next).setOnClickListener((View v) -> {
            Intent intent = new Intent(BackupPasswordActivity.this, BackupProgressActivity.class);
            if (userPassword.getText().length() > 0)
                intent.putExtra(BackupProgressActivity.ARG_USER_PASSWORD, userPassword.getText().toString());
            startNextActivity(intent);
        });
    }

}
