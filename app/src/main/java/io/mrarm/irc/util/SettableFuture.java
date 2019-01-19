package io.mrarm.irc.util;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SettableFuture<V> implements Future<V> {

    private V mValue;
    private boolean mValueSet = false;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        synchronized (this) {
            return mValueSet;
        }
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        synchronized (this) {
            while (!mValueSet) {
                wait();
            }
            return mValue;
        }
    }

    @Override
    public V get(long timeout, @NonNull TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        synchronized (this) {
            while (!mValueSet) {
                wait(unit.toMillis(timeout), (int) (unit.toNanos(timeout) % 1000000L));
            }
            return mValue;
        }
    }

    public void set(V value) {
        synchronized (this) {
            mValue = value;
            mValueSet = true;
            notifyAll();
        }
    }

}
