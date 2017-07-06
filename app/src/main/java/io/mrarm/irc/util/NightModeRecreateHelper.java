package io.mrarm.irc.util;

import android.app.Activity;
import android.content.res.Configuration;

public class NightModeRecreateHelper {

    private Activity mActivity;
    private int mWasEnabled = -1;

    public NightModeRecreateHelper(Activity activity) {
        mActivity = activity;
    }

    public void onStart() {
        int currentState = mActivity.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        if (mWasEnabled == -1)
            mWasEnabled = currentState;
        if (mWasEnabled != currentState) {
            mActivity.recreate();
            mWasEnabled = currentState;
        }
    }

}
