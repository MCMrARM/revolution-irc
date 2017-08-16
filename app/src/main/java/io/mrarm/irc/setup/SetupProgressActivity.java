package io.mrarm.irc.setup;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.mrarm.irc.R;

public class SetupProgressActivity extends SetupBigHeaderActivity {

    private TextView mDescription;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSetupContentView(R.layout.activity_setup_progress_content);
        mDescription = findViewById(R.id.desc);
        mProgressBar = findViewById(R.id.progress);
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    public void setDescriptionText(int resId) {
        mDescription.setText(resId);
    }

}
