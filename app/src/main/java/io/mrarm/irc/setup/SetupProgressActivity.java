package io.mrarm.irc.setup;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.mrarm.irc.IRCApplication;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseExitLock();
    }

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    public void setDescriptionText(int resId) {
        mDescription.setText(resId);
    }


    public void acquireExitLock() {
        ((IRCApplication) getApplication()).addPreExitCallback(sExitLock);
    }

    public void releaseExitLock() {
        ((IRCApplication) getApplication()).removePreExitCallback(sExitLock);
    }


    private static final IRCApplication.PreExitCallback sExitLock = () -> false;

}
