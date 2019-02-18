package io.mrarm.irc.newui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
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
    private View mTouchDragView;
    private View mTouchDragParentView;
    private float mTouchDragStartX;
    private VelocityTracker mTouchDragVelocity;
    private boolean mTouchDragUnsetBg;
    private int mFallbackBackgroundColor;
    private int mKeepFragmentsInMemory = 1;
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
    }

    public void setFragmentManager(FragmentManager mgr) {
        mFragmentManager = mgr;
        mFragmentManager.registerFragmentLifecycleCallbacks(mFragmentLifecycleWatcher, false);
    }

    public void push(Fragment fragment) {
        mFragmentManager.beginTransaction()
                .add(getId(), fragment)
                .commit();
        mFragments.add(fragment);
    }

    public void pop() {
        Fragment fragment = mFragments.remove(mFragments.size() - 1);
        mFragmentManager.beginTransaction()
                .remove(fragment)
                .commit();
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
                        View currentView = getChildAt(getChildCount() - 1);
                        View pv = attachParentFragment();
                        if (pv == null) {
                            mTouchDragVelocity.recycle();
                            mTouchDragVelocity = null;
                            break;
                        }
                        mTouchDragView = currentView;
                        mTouchDragParentView = pv;
                        currentView.bringToFront();
                        elevateView(mTouchDragView);
                        mTouchDragView.animate().setListener(null).cancel();
                        mTouchDragParentView.animate().setListener(null).cancel();
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mTouchDragVelocity != null) {
                    mTouchDragView = null;
                    mTouchDragParentView = null;
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
                if (mTouchDragView != null) {
                    mTouchDragVelocity.addMovement(ev);
                    float m = Math.max(ev.getX() - mTouchDragStartX, 0);
                    mTouchDragView.setTranslationX(m);
                    mTouchDragParentView.setTranslationX(
                            (m - getWidth()) * PARENT_VIEW_TRANSLATION_M);
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (mTouchDragView != null) {
                    mTouchDragVelocity.computeCurrentVelocity(1000);
                    float animDurationM = Math.min(1000.f /
                                    Math.abs(mTouchDragVelocity.getXVelocity()), mMinAnimVelocity);
                    if (mTouchDragVelocity.getXVelocity() > mMinVelocity) {
                        View v = mTouchDragView;
                        int animDuration = Math.min((int) ((getWidth() - v.getTranslationX()) *
                                animDurationM), 300);
                        v.animate().translationX(getWidth()).setDuration(animDuration)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        pop();
                                        deelevateView(v);
                                        v.animate().setListener(null);
                                    }
                                }).start();
                        mTouchDragParentView.animate().translationX(0).setDuration(animDuration)
                                .setListener(null).start();
                    } else {
                        View v = mTouchDragView;
                        int animDuration = Math.min((int) (v.getTranslationX() * animDurationM),
                                300);
                        v.animate().translationX(0).setDuration(animDuration)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        // detach parent fragment
                                        detachParentFragments();
                                    }
                                }).start();
                        mTouchDragParentView.animate().translationX(
                                - getWidth() * PARENT_VIEW_TRANSLATION_M)
                                .setDuration(animDuration).setListener(null).start();
                    }
                    mTouchDragView = null;
                    mTouchDragParentView = null;
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
            elevateView(v);
            v.setTranslationX(getWidth());
            v.animate().translationX(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            deelevateView(v);
                            detachParentFragments();
                        }
                    }).start();
        }
    }

}
