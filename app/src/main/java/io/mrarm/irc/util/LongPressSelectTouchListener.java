package io.mrarm.irc.util;

import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

import io.mrarm.irc.R;

public class LongPressSelectTouchListener implements RecyclerView.OnItemTouchListener {

    private boolean mSelectMode = false;
    private RecyclerView mRecyclerView;
    private Listener mListener;
    private Rect mTempRect = new Rect();
    private int mStartElementPos = -1;
    private int mEndElementPos = -1;
    private ScrollerRunnable mScroller;

    public LongPressSelectTouchListener(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mScroller = new ScrollerRunnable();
    }

    public void startSelectMode(int startPos) {
        mSelectMode = true;
        mStartElementPos = startPos;
        mEndElementPos = -1;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public boolean isElementHightlighted(int pos) {
        return pos == mStartElementPos ||
                (pos >= mStartElementPos && pos <= mEndElementPos) ||
                (pos <= mStartElementPos && pos >= mEndElementPos && mEndElementPos != -1
                );
    }

    private void updateHightlightedElements(RecyclerView recyclerView, int endPos) {
        if (mStartElementPos == -1) {
            mStartElementPos = endPos;
            mListener.onElementHighlighted(recyclerView, mStartElementPos, true);
            return;
        }
        for (int i = Math.max(mEndElementPos, mStartElementPos) + 1; i <= endPos; i++)
            mListener.onElementHighlighted(recyclerView, i, true);
        for (int i = Math.min(mEndElementPos == -1 ? mStartElementPos : mEndElementPos,
                mStartElementPos) - 1; i >= endPos; i--)
            mListener.onElementHighlighted(recyclerView, i, true);

        if (mEndElementPos != -1) {
            for (int i = Math.max(endPos, mStartElementPos) + 1; i <= mEndElementPos; i++)
                mListener.onElementHighlighted(recyclerView, i, false);
            for (int i = Math.min(endPos, mStartElementPos) - 1; i >= mEndElementPos; i--)
                mListener.onElementHighlighted(recyclerView, i, false);
        }

        mEndElementPos = endPos;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (mSelectMode) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                    motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                if (mListener != null && mStartElementPos != -1) {
                    int start = mStartElementPos;
                    int end = mEndElementPos == -1 ? mStartElementPos : mEndElementPos;
                    if (start > end) {
                        start = mEndElementPos;
                        end = mStartElementPos;
                    }
                    for (int i = start; i <= end; i++)
                        mListener.onElementSelected(recyclerView, i);
                }
                mStartElementPos = -1;
                mEndElementPos = -1;
                mSelectMode = false;
                mScroller.setScrollDir(0);
                return true;
            }
            if (mListener == null)
                return false;
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();

            if (y < 0) {
                mScroller.setScrollDir(-1);
            } else if (y > mRecyclerView.getHeight()) {
                mScroller.setScrollDir(1);
            } else {
                mScroller.setScrollDir(0);
            }

            x = Math.max(Math.min(x, mRecyclerView.getWidth()), 0);
            y = Math.max(Math.min(y, mRecyclerView.getHeight()), 0);

            int pos = -1;
            int childCount = recyclerView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = recyclerView.getChildAt(i);
                view.getHitRect(mTempRect);
                if (mTempRect.contains(x, y))
                    pos = recyclerView.getChildAdapterPosition(view);
            }
            if (pos >= 0)
                updateHightlightedElements(recyclerView, pos);
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        onInterceptTouchEvent(recyclerView, motionEvent);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {
    }

    public interface Listener {

        void onElementSelected(RecyclerView recyclerView, int adapterPos);

        void onElementHighlighted(RecyclerView recyclerView, int adapterPos, boolean highlight);

    }

    private class ScrollerRunnable implements Runnable {

        private int mScrollDir = 0;
        private int mAutoscrollAmount;
        private long mPrevTime;

        public ScrollerRunnable() {
            mAutoscrollAmount = mRecyclerView.getContext().getResources().getDimensionPixelSize(R.dimen.touch_press_select_autoscroll_amount);
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
            LinearLayoutManager llm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            updateHightlightedElements(mRecyclerView, mScrollDir > 0
                    ? llm.findLastCompletelyVisibleItemPosition()
                    : llm.findFirstCompletelyVisibleItemPosition());
            ViewCompat.postOnAnimation(mRecyclerView, this);
        }

    }

}
