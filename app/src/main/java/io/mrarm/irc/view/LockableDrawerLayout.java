package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import io.mrarm.irc.drawer.DrawerNavigationView;

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

    public boolean isLocked() {
        return mLocked;
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
        if (locked) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            setScrimColor(Color.TRANSPARENT);
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            setScrimColor(0x99000000);
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
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mLocked) {
            int ml = 0, mr = 0;
            for (int i = getChildCount() - 1; i >= 0; --i) {
                View child = getChildAt(i);
                LayoutParams p = (LayoutParams) child.getLayoutParams();
                int hg = p.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                if ((hg != Gravity.LEFT && hg != Gravity.RIGHT) || !isDrawerOpen(child))
                    continue;
                if (hg == Gravity.LEFT)
                    ml = child.getRight();
                if (hg == Gravity.RIGHT)
                    mr = child.getLeft();
            }
            if (ml == 0 && mr == 0)
                return;
            for (int i = getChildCount() - 1; i >= 0; --i) {
                View child = getChildAt(i);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.gravity == Gravity.NO_GRAVITY) { // content view
                    lp.leftMargin = ml;
                    lp.rightMargin = mr;
                    child.setLayoutParams(lp);
                }
            }
            super.onLayout(changed, l, t, r, b);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mLocked)
            return false;
        return super.onInterceptTouchEvent(event);
    }

    public static class ActionBarDrawerToggle implements DrawerListener {

        private DrawerArrowDrawable mDrawable;
        private Toolbar mToolbar;
        private int mOpenTextId;
        private int mCloseTextId;

        public ActionBarDrawerToggle(Toolbar toolbar, LockableDrawerLayout drawerLayout,
                                     int openTextId, int closeTextId) {
            mDrawable = new DrawerArrowDrawable(toolbar.getContext());
            mToolbar = toolbar;
            mOpenTextId = openTextId;
            mCloseTextId = closeTextId;
            drawerLayout.addDrawerListener(this);
            toolbar.setNavigationIcon(mDrawable);
            toolbar.setNavigationContentDescription(drawerLayout.isDrawerOpen(Gravity.START)
                    ? closeTextId : openTextId);
            toolbar.setNavigationOnClickListener((View view) -> {
                if (drawerLayout.isDrawerOpen(Gravity.START))
                    drawerLayout.closeDrawer(Gravity.START, !drawerLayout.isLocked());
                else
                    drawerLayout.openDrawer(Gravity.START, !drawerLayout.isLocked());
                drawerLayout.requestLayout();
            });
            mOpenTextId = openTextId;
            mCloseTextId = closeTextId;
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (((LayoutParams) drawerView.getLayoutParams()).gravity != Gravity.START)
                return;
            mToolbar.setNavigationContentDescription(mCloseTextId);
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            if (((LayoutParams) drawerView.getLayoutParams()).gravity != Gravity.START)
                return;
            mToolbar.setNavigationContentDescription(mOpenTextId);
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
        }

    }

}
