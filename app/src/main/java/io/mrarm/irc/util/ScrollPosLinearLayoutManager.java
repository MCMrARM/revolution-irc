package io.mrarm.irc.util;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;

public class ScrollPosLinearLayoutManager extends LinearLayoutManager {

    private int mPendingScrollPosition;

    public ScrollPosLinearLayoutManager(Context context) {
        super(context);
    }

    public ScrollPosLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public ScrollPosLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
        mPendingScrollPosition = position;
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        mPendingScrollPosition = -1;
    }

    public int getPendingScrollPosition() {
        return mPendingScrollPosition;
    }

}
