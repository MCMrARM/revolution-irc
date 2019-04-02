package io.mrarm.irc.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class AlwaysInterceptRecyclerView extends RecyclerView {

    public AlwaysInterceptRecyclerView(@NonNull Context context) {
        super(context);
    }

    public AlwaysInterceptRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AlwaysInterceptRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean ret = super.onTouchEvent(e);
        if (e.getAction() == MotionEvent.ACTION_MOVE && getScrollState() == SCROLL_STATE_DRAGGING)
            getParent().requestDisallowInterceptTouchEvent(true);
        return ret;
    }

}
