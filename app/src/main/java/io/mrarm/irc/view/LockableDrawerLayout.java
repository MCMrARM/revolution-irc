package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcelable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LockableDrawerLayout extends DrawerLayout {

    private boolean mLockable = false;
    private boolean mLocked = false;
    private List<WeakReference<LockableStateListener>> mLockableListener = new ArrayList<>();

    public LockableDrawerLayout(Context context) {
        super(context);
    }

    public LockableDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockableDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    /**
     * Adds a lockable state listener, as a weak reference.
     */
    public void addLockableStateListener(LockableStateListener listener) {
        mLockableListener.add(new WeakReference<>(listener));
    }

    public boolean isLockable() {
        return mLockable;
    }

    public boolean isLocked() {
        return mLocked;
    }

    public boolean isCurrentlyLocked() {
        return mLocked && mLockable;
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
        updateLockState();
    }

    private void updateLockState() {
        if (isCurrentlyLocked()) {
            if (getDrawerLockMode(Gravity.START) == DrawerLayout.LOCK_MODE_LOCKED_OPEN)
                return;
            openDrawer(Gravity.START, false);
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, GravityCompat.START);
            setScrimColor(Color.TRANSPARENT);
        } else {
            if (getDrawerLockMode(Gravity.START) == DrawerLayout.LOCK_MODE_UNLOCKED)
                return;
            closeDrawer(Gravity.START, false);
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
            setScrimColor(0x99000000);
        }
        requestLayout();
        Iterator<WeakReference<LockableStateListener>> iterator = mLockableListener.iterator();
        while (iterator.hasNext()) {
            LockableStateListener listener = iterator.next().get();
            if (listener != null)
                listener.onLockableStateChanged(isCurrentlyLocked());
            else
                iterator.remove();
        }
    }

    private int getDrawerWidth() {
        for (int i = getChildCount() - 1; i >= 0; --i) {
            View child = getChildAt(i);
            LayoutParams p = (LayoutParams) child.getLayoutParams();
            int hg = p.gravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
            if (hg == GravityCompat.START)
                return child.getMeasuredWidth();
        }
        return 0;
    }

    @Override
    public void closeDrawers() {
        if (isCurrentlyLocked())
            return;
        super.closeDrawers();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mLockable = MeasureSpec.getSize(widthMeasureSpec) >= getDrawerWidth() * 2;
        if (mLocked)
            updateLockState();

        if (isCurrentlyLocked()) {
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

                    int cw = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec)
                            - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                    int ch = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec)
                            - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                    child.measure(cw, ch);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isCurrentlyLocked())
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
                    drawerLayout.closeDrawer(Gravity.START, !drawerLayout.isCurrentlyLocked());
                else
                    drawerLayout.openDrawer(Gravity.START, !drawerLayout.isCurrentlyLocked());
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

    public interface LockableStateListener {

        void onLockableStateChanged(boolean lockable);

    }

}
