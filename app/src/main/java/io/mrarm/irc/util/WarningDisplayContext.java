package io.mrarm.irc.util;

import android.app.Activity;

public class WarningDisplayContext {

    private static Activity mActivity;

    public static void setActivity(Activity activity) {
        mActivity = activity;
    }

    public static Activity getActivity() {
        return mActivity;
    }

}
