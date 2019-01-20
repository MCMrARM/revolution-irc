package io.mrarm.irc.util;

import android.graphics.Rect;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MotionEvent;
import android.view.View;

public class LongPressSelectTouchListener implements RecyclerView.OnItemTouchListener {

    private boolean mSelectMode = false;
    private RecyclerView mRecyclerView;
    private Listener mListener;
    private Rect mTempRect = new Rect();
    private long mStartElementId = -1;
    private long mEndElementId = -1;
    private RecyclerViewScrollerRunnable mScroller;

    public LongPressSelectTouchListener(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mScroller = new RecyclerViewScrollerRunnable(recyclerView, (int scrollDir) -> {
            LinearLayoutManager llm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            updateHighlightedElements(mRecyclerView, mRecyclerView.getAdapter().getItemId(
                    scrollDir > 0
                            ? llm.findLastCompletelyVisibleItemPosition()
                            : llm.findFirstCompletelyVisibleItemPosition()));
        });
    }

    public void startSelectMode(long startPos) {
        mSelectMode = true;
        mStartElementId = startPos;
        mEndElementId = -1;
        mListener.onElementSelected(mRecyclerView, startPos);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public boolean isElementHighlighted(long id) {
        return id == mStartElementId ||
                (id >= mStartElementId && id <= mEndElementId) ||
                (id <= mStartElementId && id >= mEndElementId && mEndElementId != -1);
    }

    private void updateHighlightedElements(RecyclerView recyclerView, long endId) {
        if (mStartElementId == -1) {
            mStartElementId = endId;
            mListener.onElementHighlighted(recyclerView, mStartElementId, true);
            return;
        }
        for (long i = Math.max(mEndElementId, mStartElementId) + 1; i <= endId; i++)
            mListener.onElementHighlighted(recyclerView, i, true);
        for (long i = Math.min(mEndElementId == -1 ? mStartElementId : mEndElementId,
                mStartElementId) - 1; i >= endId; i--)
            mListener.onElementHighlighted(recyclerView, i, true);

        if (mEndElementId != -1) {
            for (long i = Math.max(endId, mStartElementId) + 1; i <= mEndElementId; i++)
                mListener.onElementHighlighted(recyclerView, i, false);
            for (long i = Math.min(endId, mStartElementId) - 1; i >= mEndElementId; i--)
                mListener.onElementHighlighted(recyclerView, i, false);
        }

        mEndElementId = endId;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (mSelectMode) {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                    motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                if (mListener != null && mStartElementId != -1) {
                    long start = mStartElementId;
                    long end = mEndElementId == -1 ? mStartElementId : mEndElementId;
                    if (start > end) {
                        start = mEndElementId;
                        end = mStartElementId;
                    }
                    for (long i = start; i <= end; i++)
                        mListener.onElementSelected(recyclerView, i);
                }
                mStartElementId = -1;
                mEndElementId = -1;
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

            long id = Long.MIN_VALUE;
            int childCount = recyclerView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = recyclerView.getChildAt(i);
                view.getHitRect(mTempRect);
                if (mTempRect.contains(x, y))
                    id = recyclerView.getChildItemId(view);
            }
            if (id != Long.MIN_VALUE)
                updateHighlightedElements(recyclerView, id);
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

        void onElementSelected(RecyclerView recyclerView, long adapterPos);

        void onElementHighlighted(RecyclerView recyclerView, long adapterId, boolean highlight);

    }

}
