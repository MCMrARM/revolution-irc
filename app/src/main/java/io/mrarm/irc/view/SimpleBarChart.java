package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import io.mrarm.irc.R;

public class SimpleBarChart extends View {

    private float[] mValues;
    private int[] mColors;
    private float mValuesTotal;
    private Path mPath = new Path();
    private RectF mTempRect = new RectF();
    private Paint mPaint = new Paint();

    public SimpleBarChart(Context context) {
        this(context, null);
    }

    public SimpleBarChart(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleBarChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setData(float[] values, int[] colors) {
        mValues = values;
        mColors = colors;
        float total = 0.f;
        for (float value : values)
            total += value;
        mValuesTotal = total;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mPath.reset();
        mTempRect.set(0, 0, right - left, bottom - top);
        float radius = getContext().getResources().getDimensionPixelSize(R.dimen.simple_bar_chart_radius);
        mPath.addRoundRect(mTempRect, radius, radius, Path.Direction.CCW);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        float x = getPaddingLeft();
        float y = getPaddingTop();
        float w = getWidth() - getPaddingLeft() - getPaddingRight();
        float h = getHeight() - getPaddingTop() - getPaddingBottom();
        int vl = mValues.length;

        canvas.clipPath(mPath);
        for (int i = 0; i < vl; i++) {
            mPaint.setColor(mColors[i]);
            float ew = w * mValues[i] / mValuesTotal;
            canvas.drawRect((float) Math.floor(x), y, (float) Math.ceil(x + ew), y + h, mPaint);
            x += ew;
        }
    }

}
