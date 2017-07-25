package io.mrarm.irc.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class RecyclerViewScrollbarLayout extends FrameLayout {

    private RecyclerViewScrollbar mScrollbar;
    private RecyclerView mRecyclerView;

    public RecyclerViewScrollbarLayout(@NonNull Context context) {
        super(context);
    }

    public RecyclerViewScrollbarLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewScrollbarLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof RecyclerViewScrollbar) {
            mScrollbar = (RecyclerViewScrollbar) child;
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) mScrollbar.getLayoutParams();
            p.gravity = GravityCompat.END;
            mScrollbar.setLayoutParams(p);
            if (mRecyclerView != null)
                mScrollbar.setRecyclerView(mRecyclerView);
        } else if (child instanceof RecyclerView) {
            mRecyclerView = (RecyclerView) child;
            if (mScrollbar != null)
                mScrollbar.setRecyclerView(mRecyclerView);
        }
    }
}
