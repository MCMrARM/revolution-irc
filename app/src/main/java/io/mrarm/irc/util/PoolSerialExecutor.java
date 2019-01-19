package io.mrarm.irc.util;

import android.os.AsyncTask;
import androidx.annotation.NonNull;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * A utility class that executes the tasks in order, for use with a ThreadPoolExecutor.
 */
public class PoolSerialExecutor implements Executor {

    private Executor mParentExecutor;
    private final Queue<Runnable> mQueue = new LinkedList<>();
    private boolean mQueuedMainTask = false;

    public PoolSerialExecutor(Executor parentExecutor) {
        mParentExecutor = parentExecutor;
    }

    public PoolSerialExecutor() {
        this(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void execute(@NonNull Runnable runnable) {
        synchronized (this) {
            mQueue.add(runnable);
            if (!mQueuedMainTask) {
                mParentExecutor.execute(mRunTasksRunnable);
                mQueuedMainTask = true;
            }
        }
    }

    private final Runnable mRunTasksRunnable = () -> {
        while (true) {
            Runnable runnable;
            synchronized (this) {
                if (mQueue.size() == 0) {
                    mQueuedMainTask = false;
                    return;
                }
                runnable = mQueue.remove();
            }
            runnable.run();
        }
    };

}
