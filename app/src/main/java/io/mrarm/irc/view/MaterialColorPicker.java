package io.mrarm.irc.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import io.mrarm.irc.R;

public class MaterialColorPicker extends View {

    private static final int[] VARIANT_ORDERING = new int[] { 6, 7, 8, 5, 4, 3, 0, 1, 2 };

    private int[] mColors;
    private int[] mExtraColors;
    private int[] mColorVariants;
    private int[] mExtraColorVariants;
    private int mDisplayedColorVariantColor = -1;
    private int[] mDisplayedColorVariants;
    private int mColorColumnCount = 4;
    private int mColorVariantsColumnCount = 3;
    private Paint mPaint;

    private int mAnimExpandIndex = -1;
    private float mAnimExpandProgress = 0.f;
    private ValueAnimator mExpandAnimator = null;
    private ValueAnimator mCollapseAnimator = null;
    private float mAnimFadeProgress = 0.f;
    private ValueAnimator mFadeInAnimator = null;
    private float mAnimFadeOutProgress = 0.f;
    private ValueAnimator mFadeOutAnimator = null;

    public MaterialColorPicker(@NonNull Context context) {
        this(context, null);
    }

    public MaterialColorPicker(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialColorPicker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mColors = context.getResources().getIntArray(R.array.color_picker_colors_main);
        mExtraColors = context.getResources().getIntArray(R.array.color_picker_colors_extra);
        TypedArray ta = context.getResources().obtainTypedArray(R.array.color_picker_variants_main);
        mColorVariants = new int[ta.length()];
        for (int i = 0; i < mColorVariants.length; i++)
            mColorVariants[i] = ta.getResourceId(i, 0);
        ta.recycle();
        ta = context.getResources().obtainTypedArray(R.array.color_picker_variants_extra);
        mExtraColorVariants = new int[ta.length()];
        for (int i = 0; i < mExtraColorVariants.length; i++)
            mExtraColorVariants[i] = ta.getResourceId(i, 0);
        ta.recycle();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        invalidate();
    }

    private int getColorIndexAt(int x, int y) {
        int baseTileSize = getWidth() / mColorColumnCount;
        int maxY = baseTileSize * ((mColors.length - 1) / mColorColumnCount + 1);
        if (x >= 0 && x < baseTileSize * mColorColumnCount && y >= 0 && y < maxY)
            return Math.min(mColors.length - 1,
                    y / baseTileSize * mColorColumnCount + x / baseTileSize);

        int minY = maxY + baseTileSize / 2;
        maxY = minY + baseTileSize * ((mExtraColors.length - 1) / mColorColumnCount + 1);
        if (x >= 0 && x < baseTileSize * mColorColumnCount && y >= minY && y < maxY)
            return mColors.length + Math.min(mExtraColors.length - 1,
                    (y - minY) / baseTileSize * mColorColumnCount + x / baseTileSize);

        return -1;
    }

    private void animateExpandColor(int index) {
        if (mExpandAnimator != null && mExpandAnimator.isRunning())
            mExpandAnimator.cancel();
        if (mCollapseAnimator != null && mCollapseAnimator.isRunning())
            mCollapseAnimator.cancel();
        if (mFadeInAnimator != null && mFadeInAnimator.isRunning())
            mFadeInAnimator.cancel();
        if (mFadeOutAnimator != null && mFadeOutAnimator.isRunning())
            mFadeOutAnimator.cancel();

        if (mExpandAnimator == null) {
            mExpandAnimator = ValueAnimator.ofFloat(0.f, 1.f);
            mExpandAnimator.setDuration(750L);
            mExpandAnimator.setInterpolator(new DecelerateInterpolator());
            mExpandAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
                mAnimExpandProgress = (Float) valueAnimator.getAnimatedValue();
                invalidate();
                if (valueAnimator.getAnimatedFraction() >= 0.7f)
                    expandColor(mAnimExpandIndex, true);
            });
        }
        mAnimExpandIndex = index;
        mDisplayedColorVariants = null;
        mDisplayedColorVariantColor = -1;
        mExpandAnimator.start();
    }

    private void expandColor(int index, boolean subAnimate) {
        if (index >= mColors.length)
            mDisplayedColorVariants = getResources().getIntArray(mExtraColorVariants[index - mColors.length]);
        else
            mDisplayedColorVariants = getResources().getIntArray(mColorVariants[index]);
        mDisplayedColorVariantColor = index;
        if (subAnimate) {
            if (mFadeInAnimator == null) {
                mFadeInAnimator = ValueAnimator.ofFloat(0.f, 9.f);
                mFadeInAnimator.setDuration(500L);
                mFadeInAnimator.setInterpolator(new LinearInterpolator());
                mFadeInAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
                    mAnimFadeProgress = (Float) valueAnimator.getAnimatedValue();
                    invalidate();
                });
            }
            mAnimFadeProgress = 0.f;
            mAnimFadeOutProgress = 0.f;
            if (mFadeOutAnimator != null)
                mFadeOutAnimator.cancel();
            if (!mFadeInAnimator.isRunning())
                mFadeInAnimator.start();
        } else {
            mAnimExpandProgress = 9.f;
            invalidate();
        }
    }

    public void closeColor() {
        if (mFadeOutAnimator == null) {
            mFadeOutAnimator = ValueAnimator.ofFloat(0.f, 9.f);
            mFadeOutAnimator.setDuration(500L);
            mFadeOutAnimator.setInterpolator(new LinearInterpolator());
            mFadeOutAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
                mAnimFadeOutProgress = (Float) valueAnimator.getAnimatedValue();
                invalidate();
            });
        }
        mFadeOutAnimator.start();

        mAnimExpandIndex = mDisplayedColorVariantColor;
        if (mCollapseAnimator == null) {
            mCollapseAnimator = ValueAnimator.ofFloat(1.f, 0.f);
            mCollapseAnimator.setDuration(750L);
            mCollapseAnimator.setInterpolator(new AccelerateInterpolator());
            mCollapseAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
                mAnimExpandProgress = (Float) valueAnimator.getAnimatedValue();
                invalidate();
            });
        }
        mCollapseAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            int i = getColorIndexAt((int) event.getX(), (int) event.getY());
            if (i != -1)
                animateExpandColor(i);
            return true;
        }
        return false;
    }

    private void drawColorVariants(Canvas canvas, float x, float y, float w) {
        float tileSize = w / mColorVariantsColumnCount;
        canvas.save();
        canvas.clipRect(x, y, x + tileSize * mColorVariantsColumnCount, y + tileSize * mColorVariantsColumnCount);

        if (mDisplayedColorVariantColor >= mColors.length)
            mPaint.setColor(mExtraColors[mDisplayedColorVariantColor - mColors.length]);
        else
            mPaint.setColor(mColors[mDisplayedColorVariantColor]);
        canvas.drawRect(x, y, x + tileSize * mColorVariantsColumnCount, y + tileSize * mColorVariantsColumnCount, mPaint);

        for (int i = 0; i < mDisplayedColorVariants.length; i++) {
            int j = VARIANT_ORDERING[i];
            mPaint.setColor(mDisplayedColorVariants[i]);
            mPaint.setAlpha(Math.max(0, Math.min((int) ((mAnimFadeProgress - i) * 255.f),
                    255 - Math.max(0, (int) ((mAnimFadeOutProgress - i) * 255.f)))));
            float mx = x + (j % mColorVariantsColumnCount) * tileSize;
            float my = y + (j / mColorVariantsColumnCount) * tileSize;
            canvas.drawRect(mx, my, mx + tileSize, my + tileSize, mPaint);
        }
        mPaint.setAlpha(255);
        canvas.restore();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mAnimExpandIndex == -1 && mDisplayedColorVariants != null) {
            drawColorVariants(canvas, 0, 0, getWidth());
            return;
        }

        canvas.save();
        int baseTileSize = getWidth() / mColorColumnCount;
        canvas.clipRect(0, 0, baseTileSize * mColorColumnCount, baseTileSize * mColorColumnCount);
        float normalTileSize = baseTileSize * (mAnimExpandIndex == -1 ? 1.f : (1.f - mAnimExpandProgress));
        float expandingTileSize = baseTileSize * mColorColumnCount - normalTileSize * (mColorColumnCount - 1);
        float x = 0.f;
        float y = 0.f;
        for (int i = 0; i < mColors.length; i++) {
            mPaint.setColor(mColors[i]);
            boolean isExpandingRow = i / mColorColumnCount == mAnimExpandIndex / mColorColumnCount;
            boolean isExpandingColumn = i % mColorColumnCount == mAnimExpandIndex % mColorColumnCount;
            float size = isExpandingRow || isExpandingColumn ? expandingTileSize : normalTileSize;
            float mx = x;
            float my = y;
            if (isExpandingRow)
                mx += (normalTileSize - expandingTileSize) * (mAnimExpandIndex % mColorColumnCount);
            if (isExpandingColumn)
                my = expandingTileSize * (i / mColorColumnCount) + (normalTileSize - expandingTileSize) * (mAnimExpandIndex / mColorColumnCount);
            canvas.drawRect(mx, my, mx + size, my + size, mPaint);
            x += size;
            if ((i + 1) % mColorColumnCount == 0 || i == mColors.length - 1) {
                x = 0;
                y += isExpandingRow ? expandingTileSize : normalTileSize;
            }
        }
        canvas.restore();

        float extraTileSize = (mAnimExpandIndex >= mColors.length ? expandingTileSize : baseTileSize);
        float extraPadding = (mAnimExpandIndex >= mColors.length ? normalTileSize : baseTileSize) / 2.f;
        y += extraPadding;
        for (int i = 0; i < mExtraColors.length; i++) {
            mPaint.setColor(mExtraColors[i]);
            if (mAnimExpandIndex >= 0 && mAnimExpandIndex < mColors.length)
                mPaint.setAlpha((int) (255.f * (1.f - mAnimExpandProgress)));
            boolean isExpandingRow = mAnimExpandIndex >= mColors.length &&
                    i / mColorColumnCount == (mAnimExpandIndex - mColors.length) / mColorColumnCount;
            float mx = x;
            if (isExpandingRow)
                mx += (normalTileSize - expandingTileSize) * (mAnimExpandIndex % mColorColumnCount);
            canvas.drawRect(mx, y, mx + extraTileSize, y + extraTileSize, mPaint);
            x += extraTileSize;
            if ((i + 1) % mColorColumnCount == 0) {
                x = 0;
                y += extraTileSize;
            }
        }
        mPaint.setAlpha(255);

        if (mDisplayedColorVariants != null) {
            float mx = normalTileSize * (mAnimExpandIndex % mColorColumnCount);
            float my = normalTileSize * (mAnimExpandIndex / mColorColumnCount);
            if (mAnimExpandIndex >= mColors.length)
                my += extraPadding;
            drawColorVariants(canvas, mx, my, expandingTileSize);
        }
    }

}
