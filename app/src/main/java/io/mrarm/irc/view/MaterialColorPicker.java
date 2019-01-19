package io.mrarm.irc.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private int[] mColorAccentVariants;
    private int[] mExtraColorVariants;
    private int mDisplayedColorVariantColor = -1;
    private int[] mDisplayedColorVariants;
    private int[] mDisplayedColorAccentVariants;
    private int mColorColumnCount = 4;
    private int mColorVariantsColumnCount = 3;
    private int mColorAccentVariantsColumnCount = 4;
    private Paint mPaint;
    private int mMaxWidth;

    private int mAnimExpandIndex = -1;
    private float mAnimExpandProgress = 0.f;
    private ValueAnimator mExpandAnimator = null;
    private ValueAnimator mCollapseAnimator = null;
    private float mAnimFadeProgress = 0.f;
    private ValueAnimator mFadeInAnimator = null;
    private float mAnimFadeOutProgress = 0.f;
    private ValueAnimator mFadeOutAnimator = null;
    private float mAnimFadeOutAccentProgress = 0.f;
    private ValueAnimator mFadeOutAccentAnimator = null;

    private BackButtonVisibilityCallback mBackButtonVisibilityCallback;
    private ColorPickCallback mColorPickCallback;

    public MaterialColorPicker(@NonNull Context context) {
        this(context, null);
    }

    public MaterialColorPicker(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialColorPicker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MaterialColorPicker, defStyleAttr, 0);
        mMaxWidth = ta.getDimensionPixelSize(R.styleable.MaterialColorPicker_maxWidth, -1);
        ta.recycle();

        mColors = context.getResources().getIntArray(R.array.color_picker_colors_main);
        mExtraColors = context.getResources().getIntArray(R.array.color_picker_colors_extra);
        ta = context.getResources().obtainTypedArray(R.array.color_picker_variants_main);
        mColorVariants = new int[ta.length()];
        for (int i = 0; i < mColorVariants.length; i++)
            mColorVariants[i] = ta.getResourceId(i, 0);
        ta.recycle();
        ta = context.getResources().obtainTypedArray(R.array.color_picker_variants_main_accent);
        mColorAccentVariants = new int[ta.length()];
        for (int i = 0; i < mColorAccentVariants.length; i++)
            mColorAccentVariants[i] = ta.getResourceId(i, 0);
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

    public void setBackButtonVisibilityCallback(BackButtonVisibilityCallback cb) {
        mBackButtonVisibilityCallback = cb;
    }

    public void setColorPickListener(ColorPickCallback cb) {
        mColorPickCallback = cb;
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

    private int getColorVariantIndexAt(int x, int y) {
        int baseTileSize = getWidth() / mColorVariantsColumnCount;
        int maxY = baseTileSize * ((mDisplayedColorVariants.length - 1) / mColorVariantsColumnCount + 1);
        if (x >= 0 && x < baseTileSize * mColorVariantsColumnCount && y >= 0 && y < maxY)
            return VARIANT_ORDERING[Math.min(mDisplayedColorVariants.length - 1,
                    y / baseTileSize * mColorVariantsColumnCount + x / baseTileSize)];

        if (mDisplayedColorAccentVariants == null)
            return -1;
        int minY = maxY + getWidth() / mColorColumnCount / 2;
        baseTileSize = getWidth() / mColorAccentVariantsColumnCount;
        maxY = minY + baseTileSize * ((mDisplayedColorAccentVariants.length - 1) /
                mColorAccentVariantsColumnCount + 1);
        if (x >= 0 && x < baseTileSize * mColorAccentVariantsColumnCount && y >= minY && y < maxY)
            return mDisplayedColorVariants.length + Math.min(
                    mDisplayedColorAccentVariants.length - 1,
                    (y - minY) / baseTileSize * mColorAccentVariantsColumnCount + x / baseTileSize);

        return -1;
    }

    private void cancelAllAnimations() {
        if (mExpandAnimator != null && mExpandAnimator.isRunning())
            mExpandAnimator.cancel();
        if (mCollapseAnimator != null && mCollapseAnimator.isRunning())
            mCollapseAnimator.cancel();
        if (mFadeInAnimator != null && mFadeInAnimator.isRunning())
            mFadeInAnimator.cancel();
        if (mFadeOutAnimator != null && mFadeOutAnimator.isRunning())
            mFadeOutAnimator.cancel();
        if (mFadeOutAccentAnimator != null && mFadeOutAccentAnimator.isRunning())
            mFadeOutAccentAnimator.cancel();
    }

    private void animateExpandColor(int index) {
        cancelAllAnimations();
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
            mExpandAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAnimExpandIndex = -1;
                }
            });
        }
        mAnimExpandIndex = index;
        mDisplayedColorVariants = null;
        mDisplayedColorAccentVariants = null;
        mDisplayedColorVariantColor = -1;
        mExpandAnimator.start();

        if (mBackButtonVisibilityCallback != null)
            mBackButtonVisibilityCallback.onBackButtonVisiblityChanged(true);
    }

    private void expandColor(int index, boolean subAnimate) {
        if (index >= mColors.length) {
            mDisplayedColorVariants = getResources().getIntArray(mExtraColorVariants[index - mColors.length]);
            mDisplayedColorAccentVariants = null;
        } else {
            mDisplayedColorVariants = getResources().getIntArray(mColorVariants[index]);
            mDisplayedColorAccentVariants = getResources().getIntArray(mColorAccentVariants[index]);
        }
        mDisplayedColorVariantColor = index;
        if (subAnimate) {
            if (mFadeInAnimator == null) {
                mFadeInAnimator = ValueAnimator.ofFloat(0.f, 9.f + 4.f);
                mFadeInAnimator.setDuration(750L);
                mFadeInAnimator.setInterpolator(new LinearInterpolator());
                mFadeInAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
                    mAnimFadeProgress = (Float) valueAnimator.getAnimatedValue();
                    invalidate();
                });
            }
            mAnimFadeProgress = 0.f;
            mAnimFadeOutProgress = 0.f;
            mAnimFadeOutAccentProgress = 0.f;
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
        cancelAllAnimations();
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

        if (mFadeOutAccentAnimator == null) {
            mFadeOutAccentAnimator = ValueAnimator.ofFloat(0.f, 4.f);
            mFadeOutAccentAnimator.setDuration(200L);
            mFadeOutAccentAnimator.setInterpolator(new LinearInterpolator());
            mFadeOutAccentAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
                mAnimFadeOutAccentProgress = (Float) valueAnimator.getAnimatedValue();
                invalidate();
            });
        }
        mFadeOutAccentAnimator.start();

        mAnimExpandIndex = mDisplayedColorVariantColor;
        if (mCollapseAnimator == null) {
            mCollapseAnimator = ValueAnimator.ofFloat(1.f, 0.f);
            mCollapseAnimator.setDuration(750L);
            mCollapseAnimator.setInterpolator(new AccelerateInterpolator());
            mCollapseAnimator.addUpdateListener((ValueAnimator valueAnimator) -> {
                mAnimExpandProgress = (Float) valueAnimator.getAnimatedValue();
                invalidate();
            });
            mCollapseAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAnimExpandIndex = -1;
                    mDisplayedColorVariants = null;
                    mDisplayedColorAccentVariants = null;
                }
            });
        }
        mCollapseAnimator.start();

        if (mBackButtonVisibilityCallback != null)
            mBackButtonVisibilityCallback.onBackButtonVisiblityChanged(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mAnimExpandIndex != -1)
                return true;
            if (mDisplayedColorVariants != null) {
                int i = getColorVariantIndexAt((int) event.getX(), (int) event.getY());
                if (i != -1 && mColorPickCallback != null) {
                    if (i >= mDisplayedColorVariants.length)
                        mColorPickCallback.onColorPicked(mDisplayedColorAccentVariants[i -
                                mDisplayedColorVariants.length]);
                    else
                        mColorPickCallback.onColorPicked(mDisplayedColorVariants[i]);
                }
                return true;
            }
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
        canvas.restore();
        if (mDisplayedColorAccentVariants != null) {
            y += ((mDisplayedColorVariants.length - 1) / mColorVariantsColumnCount + 1) * tileSize
                    + getWidth() / mColorColumnCount / 2.f;
            tileSize = w / mColorAccentVariantsColumnCount;

            for (int i = 0; i < mDisplayedColorAccentVariants.length; i++) {
                mPaint.setColor(mDisplayedColorAccentVariants[i]);
                int j = i + mDisplayedColorVariants.length;
                mPaint.setAlpha(Math.max(0, Math.min((int) ((mAnimFadeProgress - j) * 255.f),
                        255 - Math.max(0, (int) ((mAnimFadeOutAccentProgress - i) * 255.f)))));
                float mx = x + (i % mColorAccentVariantsColumnCount) * tileSize;
                float my = y + (i / mColorAccentVariantsColumnCount) * tileSize;
                canvas.drawRect(mx, my, mx + tileSize, my + tileSize, mPaint);
            }
        }
        mPaint.setAlpha(255);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mMaxWidth != -1 && getMeasuredWidth() > mMaxWidth)
            setMeasuredDimension(mMaxWidth, getMeasuredHeight());
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int baseTileSize = getMeasuredWidth() / mColorColumnCount;
        int vTileCount = ((mColors.length - 1) / mColorColumnCount + 1);
        vTileCount += ((mExtraColors.length - 1) / mColorColumnCount + 1);
        float height = (vTileCount + 0.5f) * baseTileSize;
        if (mode == MeasureSpec.AT_MOST) {
            if (MeasureSpec.getSize(heightMeasureSpec) < height) {
                baseTileSize = (int) (MeasureSpec.getSize(heightMeasureSpec) / (vTileCount + 0.5f));
                setMeasuredDimension(mColorColumnCount * baseTileSize,
                        (int) ((vTileCount + 0.5f) * baseTileSize));
            } else {
                setMeasuredDimension(getMeasuredWidth(), Math.min((int) height,
                        MeasureSpec.getSize(heightMeasureSpec)));
            }
        } else if (mode == MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(getMeasuredWidth(), (int) height);
        }
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

    public interface BackButtonVisibilityCallback {

        void onBackButtonVisiblityChanged(boolean visible);

    }

    public interface ColorPickCallback {

        void onColorPicked(int color);

    }

}
