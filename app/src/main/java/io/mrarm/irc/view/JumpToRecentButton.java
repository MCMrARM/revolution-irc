package io.mrarm.irc.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.RelativeLayout;

import io.mrarm.irc.R;

public class JumpToRecentButton extends RelativeLayout {

    private ValueAnimator mVisibilityAnimator;

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

        mVisibilityAnimator = new ValueAnimator();
        mVisibilityAnimator.setInterpolator(new AccelerateInterpolator());
        mVisibilityAnimator.setDuration(200L);
        mVisibilityAnimator.addUpdateListener((a) -> {
            setScaleX((Float) a.getAnimatedValue());
            setScaleY((Float) a.getAnimatedValue());
        });
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

    private Animator.AnimatorListener mHideListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            setVisibility(View.GONE);
        }
    };

}
