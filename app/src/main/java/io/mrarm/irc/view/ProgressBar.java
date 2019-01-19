package io.mrarm.irc.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ProgressBar extends View {

    private float mStrokeWidth;
    private float mMinSize = 20.f;
    private float mMaxSize = 270.f;
    private int mRotationTime = 1500;
    private int mExpandTime = 500;
    private int mCollapseTime = 1000;

    private Paint mPaint = new Paint();
    private RectF mTmpRectF = new RectF();
    private float mCRotation = 0.f;
    private float mCSize = 0.f;

    private ValueAnimator mRotationAnimator;
    private ValueAnimator mExpandAnimator;
    private ValueAnimator mCollapseAnimator;
    private float mRotationPreviousValue = 0.f;
    private float mCollapsePreviousValue = 0.f;

    public ProgressBar(Context context) {
        this(context, null);
    }

    public ProgressBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(false);

        mPaint.setAntiAlias(true);
        mPaint.setColor(StyledAttributesHelper.getColor(context, R.attr.colorAccent, 0));
        mStrokeWidth = context.getResources().getDimensionPixelSize(R.dimen.progressbar_arc_thickness);

        mRotationAnimator = ValueAnimator.ofFloat(0.f, 360.f);
        mRotationAnimator.setDuration(mRotationTime);
        mRotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mRotationAnimator.setInterpolator(new LinearInterpolator());
        mRotationAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
            float rot = (Float) valueAnimator.getAnimatedValue();
            mCRotation += rot - mRotationPreviousValue;
            mRotationPreviousValue = rot;
            invalidate();
        });

        mExpandAnimator = ValueAnimator.ofFloat(0.f, 1.f);
        mExpandAnimator.setDuration(mExpandTime);
        mExpandAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mExpandAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
            mCSize = (Float) valueAnimator.getAnimatedValue();
            invalidate();
        });
        mExpandAnimator.addListener(mSizeAnimatorListener);

        mCollapseAnimator = ValueAnimator.ofFloat(1.f, 0.f);
        mCollapseAnimator.setDuration(mCollapseTime);
        mCollapseAnimator.setInterpolator(new DecelerateInterpolator());
        mCollapseAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
            mCSize = (Float) valueAnimator.getAnimatedValue();
            mCRotation += (mMaxSize - mMinSize) * (mCollapsePreviousValue - mCSize);
            mCollapsePreviousValue = mCSize;
            invalidate();
        });
        mCollapseAnimator.addListener(mSizeAnimatorListener);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (visibility == View.VISIBLE) {
            mRotationAnimator.start();
            mExpandAnimator.start();
        } else {
            mRotationAnimator.cancel();
            mExpandAnimator.cancel();
            mCollapseAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);

        mTmpRectF.set(mStrokeWidth / 2.f, mStrokeWidth / 2.f, w - mStrokeWidth / 2.f, h - mStrokeWidth / 2.f);
        canvas.drawArc(mTmpRectF, mCRotation, mMinSize + (mMaxSize - mMinSize) * mCSize, false, mPaint);
    }

    private Animator.AnimatorListener mSizeAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (animator == mCollapseAnimator) {
                mExpandAnimator.start();
            } else {
                mCollapsePreviousValue = 1.f;
                mCollapseAnimator.start();
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }
    };

}
