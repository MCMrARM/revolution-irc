package io.mrarm.irc.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

import io.mrarm.irc.R;

public class JumpToRecentButton extends ViewGroup {

    private ValueAnimator mVisibilityAnimator;
    private View mButton;
    private TextView mCounter;

    public JumpToRecentButton(Context context) {
        this(context, null);
    }

    public JumpToRecentButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JumpToRecentButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context)
                .inflate(R.layout.jump_to_recent_button, this, true);
        mButton = findViewById(R.id.button);
        mCounter = findViewById(R.id.counter);
        setClipToPadding(false);

        mVisibilityAnimator = new ValueAnimator();
        mVisibilityAnimator.setInterpolator(new AccelerateInterpolator());
        mVisibilityAnimator.setDuration(200L);
        mVisibilityAnimator.addUpdateListener((a) -> {
            setScaleX((Float) a.getAnimatedValue());
            setScaleY((Float) a.getAnimatedValue());
        });

        mCounter.setVisibility(View.GONE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mButton.measure(widthMeasureSpec, heightMeasureSpec);
        mCounter.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(
                mButton.getMeasuredWidth() + getPaddingLeft() + getPaddingRight(),
                mButton.getMeasuredHeight() + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mButton.layout(getWidth() / 2 - mButton.getMeasuredWidth() / 2,
                getHeight() / 2 - mButton.getMeasuredHeight() / 2,
                getWidth() / 2 + mButton.getMeasuredWidth() / 2,
                getHeight() / 2 + mButton.getMeasuredHeight() / 2);
        int cr = getWidth() / 2 + mButton.getMeasuredWidth() / 2 + mCounter.getMeasuredWidth() / 2;
        cr -= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                getResources().getDisplayMetrics());
        cr = Math.min(cr, getWidth());
        int cb = getHeight() / 2 + mButton.getMeasuredHeight() / 2;
        mCounter.layout(cr - mCounter.getMeasuredWidth(),
                cb - mCounter.getMeasuredHeight(),
                cr, cb);
    }

    public void setVisibleAnimated(boolean visible) {
        if (visible) {
            mVisibilityAnimator.removeListener(mHideListener);
            mVisibilityAnimator.setFloatValues(0.f, 1.f);
            setVisibility(View.VISIBLE);
        } else {
            mVisibilityAnimator.addListener(mHideListener);
            mVisibilityAnimator.setFloatValues(1.f, 0.f);
        }
        mVisibilityAnimator.start();
    }

    public void setCounter(int value) {
        mCounter.setVisibility(value > 0 ? View.VISIBLE : View.GONE);
        mCounter.setText(String.valueOf(value));
    }

    private Animator.AnimatorListener mHideListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            setVisibility(View.INVISIBLE);
        }
    };

}
