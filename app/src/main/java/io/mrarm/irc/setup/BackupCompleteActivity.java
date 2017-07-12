package io.mrarm.irc.setup;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import io.mrarm.irc.R;

public class BackupCompleteActivity extends SetupBigHeaderActivity {

    public static final String ARG_DESC_TEXT = "desc_text";
    public static final String ARG_RESTORE_MODE = "restore_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setCustomContentView(R.layout.activity_backup_complete);
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra(ARG_RESTORE_MODE, false))
            setTitle(R.string.title_activity_restore);

        int desc = getIntent().getIntExtra(ARG_DESC_TEXT, -1);
        if (desc != -1) {
            ((TextView) findViewById(R.id.desc)).setText(desc);
        }
        findViewById(R.id.finish).setOnClickListener((View v) -> {
            setSetupFinished();
        });
    }

    @Override
    public void onBackPressed() {
        setSetupFinished();
        super.onBackPressed();
    }

}
