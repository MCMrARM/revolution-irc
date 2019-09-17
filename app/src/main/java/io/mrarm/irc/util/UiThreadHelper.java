package io.mrarm.irc.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutionException;

import io.mrarm.chatlib.util.SettableFuture;

public class UiThreadHelper {

    private static final Handler sUiHandler = new Handler(Looper.getMainLooper());

    public static void runOnUiThread(Runnable r) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread())
            r.run();
        else
            sUiHandler.post(r);
    }

    public static void runOnUiThreadSync(Runnable r) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            r.run();
        } else {
            SettableFuture<Void> f = new SettableFuture<>();
            sUiHandler.post(() -> {
                try {
                    r.run();
                    f.set(null);
                } catch (Exception e) {
                    f.setExecutionException(e);
                }
            });
            try {
                f.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException)
                    throw (RuntimeException) e.getCause();
                throw new RuntimeException(e.getCause());
            }
        }
    }

    public static <T> T runOnUiThreadSync(SyncRunFn<T> r) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            r.run();
        } else {
            SettableFuture<T> f = new SettableFuture<>();
            sUiHandler.post(() -> {
                try {
                    f.set(r.run());
                } catch (Exception e) {
                    f.setExecutionException(e);
                }
            });
            try {
                return f.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException)
                    throw (RuntimeException) e.getCause();
                throw new RuntimeException(e.getCause());
            }
        }
    }

    public interface SyncRunFn<T> {
        T run();
    }

}
