package io.mrarm.irc.util;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.animation.AnimationUtils;

import io.mrarm.irc.R;

public class RecyclerViewScrollerRunnable implements Runnable {

    private RecyclerView mRecyclerView;
    private OnScrolledListener mScrolledListener;
    private int mScrollDir = 0;
    private int mAutoscrollAmount;
    private long mPrevTime;

    public RecyclerViewScrollerRunnable(RecyclerView recyclerView, OnScrolledListener listener) {
        mRecyclerView = recyclerView;
        mScrolledListener = listener;
        mAutoscrollAmount = recyclerView.getContext().getResources()
                .getDimensionPixelSize(R.dimen.touch_press_select_autoscroll_amount);
    }

    public void setScrollDir(int dir) {
        if (mScrollDir == dir)
            return;
        if (dir != 0 && mScrollDir == 0)
            ViewCompat.postOnAnimation(mRecyclerView, this);
        mScrollDir = dir;
        mPrevTime = AnimationUtils.currentAnimationTimeMillis();
    }

    @Override
    public void run() {
        if (mScrollDir == 0)
            return;

        long now = AnimationUtils.currentAnimationTimeMillis();
        float delta = (now - mPrevTime) * 0.001f;
        mPrevTime = now;

        mRecyclerView.scrollBy(0, (int) (delta * mAutoscrollAmount * mScrollDir));
        if (mScrolledListener != null)
            mScrolledListener.onScrolled(mScrollDir);
        ViewCompat.postOnAnimation(mRecyclerView, this);
    }


    public interface OnScrolledListener {
        void onScrolled(int scrollDir);
    }

}
