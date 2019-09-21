package io.mrarm.irc.util;

import androidx.databinding.ObservableList;

public class SimpleOnListChangedCallback<T extends ObservableList>
        extends ObservableList.OnListChangedCallback<T> {

    private ChangeCallback mCallback;

    public SimpleOnListChangedCallback(ChangeCallback cb) {
        mCallback = cb;
    }

    @Override
    public void onChanged(T sender) {
        mCallback.onChanged();
    }

    @Override
    public void onItemRangeChanged(T sender, int positionStart, int itemCount) {
        mCallback.onChanged();
    }

    @Override
    public void onItemRangeInserted(T sender, int positionStart, int itemCount) {
        mCallback.onChanged();
    }

    @Override
    public void onItemRangeMoved(T sender, int fromPosition, int toPosition, int itemCount) {
        mCallback.onChanged();
    }

    @Override
    public void onItemRangeRemoved(T sender, int positionStart, int itemCount) {
        mCallback.onChanged();
    }

    public interface ChangeCallback {
        void onChanged();
    }

}
