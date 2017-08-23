package io.mrarm.irc.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import io.mrarm.irc.R;

public class MaterialColorPicker extends View {

    private int[] mColors;
    private int mColorColumnCount = 4;
    private Paint mPaint;

    private int mAnimExpandIndex = -1;
    private float mAnimProgress = 0.f;

    public MaterialColorPicker(@NonNull Context context) {
        this(context, null);
    }

    public MaterialColorPicker(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialColorPicker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mColors = context.getResources().getIntArray(R.array.color_picker_colors_main);
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

    private void expandColor(int index) {
        ValueAnimator va = ValueAnimator.ofFloat(0.f, 1.f);
        va.setDuration(750L);
        va.setInterpolator(new AccelerateInterpolator());
        mAnimExpandIndex = index;
        va.addUpdateListener((ValueAnimator valueAnimator) -> {
            mAnimProgress = (Float) valueAnimator.getAnimatedValue();
            invalidate();
        });
        va.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            expandColor(getMainColorIndexAt(event.getX(), event.getY()));
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        int baseTileSize = getWidth() / mColorColumnCount;
        canvas.clipRect(0, 0, baseTileSize * mColorColumnCount, baseTileSize * mColorColumnCount);
        float normalTileSize = baseTileSize * (mAnimExpandIndex == -1 ? 1.f : (1.f - mAnimProgress));
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
    }

}
