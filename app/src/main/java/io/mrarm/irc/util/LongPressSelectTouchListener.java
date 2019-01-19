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
    private int mStartElementPos = -1;
    private int mEndElementPos = -1;
    private RecyclerViewScrollerRunnable mScroller;

    public LongPressSelectTouchListener(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mScroller = new RecyclerViewScrollerRunnable(recyclerView, (int scrollDir) -> {
            LinearLayoutManager llm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            updateHightlightedElements(mRecyclerView, scrollDir > 0
                    ? llm.findLastCompletelyVisibleItemPosition()
                    : llm.findFirstCompletelyVisibleItemPosition());
        });
    }

    public void startSelectMode(int startPos) {
        mSelectMode = true;
        mStartElementPos = startPos;
        mEndElementPos = -1;
        mListener.onElementSelected(mRecyclerView, startPos);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public boolean isElementHightlighted(int pos) {
        return pos == mStartElementPos ||
                (pos >= mStartElementPos && pos <= mEndElementPos) ||
                (pos <= mStartElementPos && pos >= mEndElementPos && mEndElementPos != -1);
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

}
