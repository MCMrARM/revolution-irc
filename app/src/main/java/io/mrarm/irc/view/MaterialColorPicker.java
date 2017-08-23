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
    private int[] mColorVariants;
    private int mDisplayedColorVariantsBg;
    private int[] mDisplayedColorVariants;
    private int mColorColumnCount = 4;
    private int mColorVariantsColumnCount = 3;
    private Paint mPaint;

    private int mAnimExpandIndex = -1;
    private float mAnimExpandProgress = 0.f;
    private float mAnimFadeProgress = 0.f;

    public MaterialColorPicker(@NonNull Context context) {
        this(context, null);
    }

    public MaterialColorPicker(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialColorPicker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mColors = context.getResources().getIntArray(R.array.color_picker_colors_main);
        TypedArray ta = context.getResources().obtainTypedArray(R.array.color_picker_variants_main);
        mColorVariants = new int[ta.length()];
        for (int i = 0; i < mColorVariants.length; i++)
            mColorVariants[i] = ta.getResourceId(i, 0);
        ta.recycle();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        invalidate();
    }

    private int getMainColorIndexAt(float x, float y) {
        int baseTileSize = getWidth() / mColorColumnCount;
        int xx = Math.max(0, Math.min(mColorColumnCount, (int) x / baseTileSize));
        int yy = Math.max(0, Math.min((mColors.length - 1) / mColorColumnCount, (int) y / baseTileSize));
        return Math.min(mColors.length - 1, yy * mColorColumnCount + xx);
    }

    private void animateExpandColor(int index) {
        ValueAnimator va = ValueAnimator.ofFloat(0.f, 1.f);
        va.setDuration(750L);
        va.setInterpolator(new DecelerateInterpolator());
        mAnimExpandIndex = index;
        va.addUpdateListener((ValueAnimator valueAnimator) -> {
            mAnimExpandProgress = (Float) valueAnimator.getAnimatedValue();
            invalidate();
            if (valueAnimator.getAnimatedFraction() >= 0.7f && mDisplayedColorVariantsBg == 0) {
                expandColor(index, true);
            }
        });
        va.start();
    }

    private void expandColor(int index, boolean subAnimate) {
        mDisplayedColorVariants = getResources().getIntArray(mColorVariants[index]);
        mDisplayedColorVariantsBg = mColors[index];
        //mAnimExpandIndex = -1;
        if (subAnimate) {
            ValueAnimator va = ValueAnimator.ofFloat(0.f, 9.f);
            va.setDuration(500L);
            va.setInterpolator(new LinearInterpolator());
            va.addUpdateListener((ValueAnimator valueAnimator) -> {
                mAnimFadeProgress = (Float) valueAnimator.getAnimatedValue();
                invalidate();
            });
            va.start();
        } else {
            mAnimExpandProgress = 9.f;
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            animateExpandColor(getMainColorIndexAt(event.getX(), event.getY()));
            return true;
        }
        return false;
    }

    private void drawColorVariants(Canvas canvas, float x, float y, float w) {
        float tileSize = w / mColorVariantsColumnCount;
        canvas.save();
        canvas.clipRect(x, y, x + tileSize * mColorVariantsColumnCount, y + tileSize * mColorVariantsColumnCount);

        mPaint.setColor(mDisplayedColorVariantsBg);
        canvas.drawRect(x, y, x + tileSize * mColorVariantsColumnCount, y + tileSize * mColorVariantsColumnCount, mPaint);

        for (int i = 0; i < mDisplayedColorVariants.length; i++) {
            int j = VARIANT_ORDERING[i];
            mPaint.setColor(mDisplayedColorVariants[i]);
            mPaint.setAlpha(Math.max(0, Math.min((int) ((mAnimFadeProgress - i) * 255.f), 255)));
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
            if ((i + 1) % mColorColumnCount == 0) {
                x = 0;
                y += isExpandingRow ? expandingTileSize : normalTileSize;
            }
        }

        if (mDisplayedColorVariants != null) {
            float mx = normalTileSize * (mAnimExpandIndex % mColorColumnCount);
            float my = normalTileSize * (mAnimExpandIndex / mColorColumnCount);
            drawColorVariants(canvas, mx, my, expandingTileSize);
        }
    }

}
