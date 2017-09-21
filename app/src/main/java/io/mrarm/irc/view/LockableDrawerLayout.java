package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

public class LockableDrawerLayout extends DrawerLayout {

    private boolean mLocked = false;

    public LockableDrawerLayout(Context context) {
        super(context);
    }

    public LockableDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockableDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
        if (locked) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            setScrimColor(Color.TRANSPARENT);
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
        requestLayout();
    }

    @Override
    public void closeDrawers() {
        if (mLocked)
            return;
        super.closeDrawers();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mLocked) {
            int ml = 0, mr = 0;
            for (int i = getChildCount() - 1; i >= 0; --i) {
                View child = getChildAt(i);
                LayoutParams p = (LayoutParams) child.getLayoutParams();
                int hg = p.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                if (hg == Gravity.LEFT)
                    ml = child.getMeasuredWidth();
                if (hg == Gravity.RIGHT)
                    mr = child.getMeasuredWidth();
            }
            for (int i = getChildCount() - 1; i >= 0; --i) {
                View child = getChildAt(i);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.gravity == Gravity.NO_GRAVITY) { // content view
                    lp.leftMargin += ml;
                    lp.rightMargin += mr;
                    child.setLayoutParams(lp);

                    int contentWidthSpec = MeasureSpec.makeMeasureSpec(
                            MeasureSpec.getSize(widthMeasureSpec)
                                    - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                    int contentHeightSpec = MeasureSpec.makeMeasureSpec(
                            MeasureSpec.getSize(heightMeasureSpec)
                                    - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                    child.measure(contentWidthSpec, contentHeightSpec);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mLocked)
            return false;
        return super.onInterceptTouchEvent(event);
    }

}
