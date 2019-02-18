package io.mrarm.irc.util;

import android.os.Handler;
import android.os.Looper;

public class UiThreadHelper {

    private static final Handler sUiHandler = new Handler(Looper.getMainLooper());

    public static void runOnUiThread(Runnable r) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread())
            r.run();
        else
            sUiHandler.post(r);
    }

}
