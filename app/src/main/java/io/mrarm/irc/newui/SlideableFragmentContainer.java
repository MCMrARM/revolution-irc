package io.mrarm.irc.newui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.mrarm.irc.util.StyledAttributesHelper;

public class SlideableFragmentContainer extends FrameLayout {

    private static final float MIN_VELOCITY = 20;
    private static final float MIN_ANIM_VELOCITY = 0.4f;
    private static final float PARENT_VIEW_TRANSLATION_M = 0.3f;

    private FragmentManager mFragmentManager;
    private final List<Fragment> mFragments = new ArrayList<>();
    private int mTouchSlop;
    private float mMinVelocity;
    private float mMinAnimVelocity;
    private View mDragView;
    private View mDragParentView;
    private ValueAnimator mDragAnimator = ValueAnimator.ofFloat(0, 0);
    private boolean mTouchDragActive = false;
    private float mTouchDragStartX;
    private VelocityTracker mTouchDragVelocity;
    private boolean mTouchDragUnsetBg;
    private int mFallbackBackgroundColor;
    private int mKeepFragmentsInMemory = 1;
    private final List<DragListener> mDragListeners = new ArrayList<>();
    private final FragmentLifecycleWatcher mFragmentLifecycleWatcher =
            new FragmentLifecycleWatcher();

    public SlideableFragmentContainer(@NonNull Context context) {
        this(context, null);
    }

    public SlideableFragmentContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideableFragmentContainer(@NonNull Context context, @Nullable AttributeSet attrs,
                                      int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFallbackBackgroundColor = StyledAttributesHelper.getColor(
                context, android.R.attr.colorBackground, 0);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMinVelocity = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_VELOCITY,
                context.getResources().getDisplayMetrics());
        mMinAnimVelocity = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_ANIM_VELOCITY,
                context.getResources().getDisplayMetrics());
        mDragAnimator.addUpdateListener((a) -> setDragValue((float) a.getAnimatedValue()));
        mDragAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void addDragListener(DragListener listener) {
        mDragListeners.add(listener);
    }

    public void removeDragListener(DragListener listener) {
        mDragListeners.remove(listener);
    }

    public void setFragmentManager(FragmentManager mgr) {
        mFragmentManager = mgr;
        mFragmentManager.registerFragmentLifecycleCallbacks(mFragmentLifecycleWatcher, false);
    }

    public Fragment getCurrentFragment() {
        if (mFragments.size() > 0)
            return mFragments.get(mFragments.size() - 1);
        return null;
    }

    public int getFragmentCount() {
        return mFragments.size();
    }

    public Fragment getFragment(int index) {
        return mFragments.get(mFragments.size() - 1 - index);
    }

    public void push(Fragment fragment) {
        mFragments.add(fragment);
        mFragmentManager.beginTransaction()
                .add(getId(), fragment)
                .commit();
    }

    public void pop() {
        Fragment fragment = mFragments.remove(mFragments.size() - 1);
        mFragmentManager.beginTransaction()
                .remove(fragment)
                .commitNow();
    }

    public void popAnim() {
        setDragValueAnimated(getWidth(), 500, () -> {
            cancelDrag();
            pop();
        });
    }

    private View attachParentFragment() {
        if (getChildCount() > 1) {
            View v = getChildAt(getChildCount() - 2);
            v.setVisibility(View.VISIBLE);
            return v;
        }
        if (mFragments.size() <= 1)
            return null;
        Fragment df = mFragments.get(mFragments.size() - 2);
        mFragmentManager.beginTransaction()
                .attach(df)
                .commitNow();
        return getChildAt(getChildCount() - 1);
    }

    private void detachParentFragments() {
        if (mFragments.size() == 0)
            return;
        Set<Fragment> mKeepFragments = new HashSet<>();
        mKeepFragments.add(mFragments.get(mFragments.size() - 1));
        for (int i = 1; i <= Math.min(mKeepFragmentsInMemory,
                Math.min(getChildCount(), mFragments.size()) - 1); i++) {
            View v = getChildAt(getChildCount() - 1 - i);
            v.setVisibility(View.GONE);
            mKeepFragments.add(mFragments.get(mFragments.size() - 1 - i));
        }
        FragmentTransaction t = mFragmentManager.beginTransaction();
        for (Fragment f : mFragmentManager.getFragments()) {
            if (f.getId() == getId() && !mKeepFragments.contains(f))
                t.detach(f);
        }
        t.commitNow();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDragVelocity = VelocityTracker.obtain();
                mTouchDragStartX = ev.getX();
                mTouchDragVelocity.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchDragVelocity != null) {
                    mTouchDragVelocity.addMovement(ev);
                    if (ev.getX() - mTouchDragStartX > mTouchSlop) {
                        mTouchDragStartX += mTouchSlop;
                        if (!prepareDrag()) {
                            mTouchDragVelocity.recycle();
                            mTouchDragVelocity = null;
                            break;
                        }
                        mTouchDragActive = true;
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mTouchDragActive = false;
                if (mTouchDragVelocity != null) {
                    mTouchDragVelocity.recycle();
                    mTouchDragVelocity = null;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (mTouchDragActive) {
                    mTouchDragVelocity.addMovement(ev);
                    setDragValue(Math.max(ev.getX() - mTouchDragStartX, 0));
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (mTouchDragActive) {
                    mTouchDragVelocity.computeCurrentVelocity(1000);
                    float animDurationM = Math.min(1000.f /
                                    Math.abs(mTouchDragVelocity.getXVelocity()), mMinAnimVelocity);
                    if (mTouchDragVelocity.getXVelocity() > mMinVelocity) {
                        View v = mDragView;
                        int animDuration = Math.min((int) ((getWidth() - v.getTranslationX()) *
                                animDurationM), 300);
                        setDragValueAnimated(getWidth(), animDuration, () -> {
                            cancelDrag();
                            pop();
                        });
                    } else {
                        View v = mDragView;
                        int animDuration = Math.min((int) (v.getTranslationX() * animDurationM),
                                300);
                        setDragValueAnimated(0, animDuration, () -> {
                            cancelDrag();
                            detachParentFragments();
                        });
                    }
                }
                if (mTouchDragVelocity != null) {
                    mTouchDragVelocity.recycle();
                    mTouchDragVelocity = null;
                }
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    private boolean prepareDrag() {
        if (mDragView != null && mDragParentView != null)
            return true;
        View currentView = getChildAt(getChildCount() - 1);
        View pv = attachParentFragment();
        if (pv == null)
            return false;
        mDragView = currentView;
        mDragParentView = pv;
        mDragView.bringToFront();
        elevateView(mDragView);
        for (DragListener l : mDragListeners)
            l.onDragStarted(mDragView, mDragParentView);
        return true;
    }

    private void cancelDrag() {
        if (mDragView == null)
            return;
        deelevateView(mDragView);
        mDragView = null;
        mDragParentView = null;
        for (DragListener l : mDragListeners)
            l.onDragEnded();
    }

    private void setDragValue(float value) {
        if (mDragView == null && !prepareDrag())
            return;
        float m = value;
        mDragView.setTranslationX(m);
        if (mDragParentView != null) {
            mDragParentView.setTranslationX(
                    (m - getWidth()) * PARENT_VIEW_TRANSLATION_M);
        }
        for (DragListener l : mDragListeners)
            l.onDragValueChanged(value);
    }

    private void setDragValueAnimated(float value, int duration, Runnable finishCb) {
        if (mDragView == null && !prepareDrag())
            return;
        mDragAnimator.setFloatValues(mDragView.getTranslationX(), value);
        mDragAnimator.setDuration(duration);
        mDragAnimator.removeAllListeners();
        mDragAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (finishCb != null)
                    finishCb.run();
                mDragAnimator.removeAllListeners();
            }
        });
        mDragAnimator.start();
    }

    private void elevateView(View v) {
        if (v.getBackground() == null) {
            mTouchDragUnsetBg = true;
            v.setBackgroundColor(mFallbackBackgroundColor);
        }
        ViewCompat.setElevation(v,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.f,
                        getResources().getDisplayMetrics()));
    }

    private void deelevateView(View v) {
        if (mTouchDragUnsetBg) {
            v.setBackground(null);
            mTouchDragUnsetBg = false;
        }
        ViewCompat.setElevation(v, 0.f);
    }

    private class FragmentLifecycleWatcher extends FragmentManager.FragmentLifecycleCallbacks {

        @Override
        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                          @NonNull View v, @Nullable Bundle savedInstanceState) {
            if (f != mFragments.get(mFragments.size() - 1))
                return;
            v.setTranslationX(getWidth());
            setDragValueAnimated(0.f, 500, () -> {
                cancelDrag();
                detachParentFragments();
            });
        }
    }

    public interface DragListener {

        void onDragStarted(View dragView, View parentView);

        void onDragValueChanged(float value);

        void onDragEnded();

    }

}
