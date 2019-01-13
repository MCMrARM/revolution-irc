package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class ColorPicker extends View {

    private float mCurrentHue = 0.f;
    private float mCurrentSaturation = 1.f;
    private float mCurrentValue = 1.f;
    private Paint mFillPaint;
    private Rect mTmpRect = new Rect();
    private LinearGradient mSaturationGradient;
    private Paint mSaturationGradientPaint;
    private LinearGradient mValueGradient;
    private Paint mValueGradientPaint;
    private Paint mCirclePaint;
    private Paint mCircleInnerPaint;
    private float mCircleSize;
    private float[] mTmpHSV = new float[3];

    public ColorPicker(Context context) {
        this(context, null);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mFillPaint = new Paint();
        mSaturationGradientPaint = new Paint();
        mSaturationGradientPaint.setDither(true);
        mValueGradientPaint = new Paint();
        mValueGradientPaint.setDither(true);
        mCirclePaint = new Paint();
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                3.f, getResources().getDisplayMetrics()));
        mCircleInnerPaint = new Paint();
        mCircleSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                8.f, getResources().getDisplayMetrics());
    }

    public void attachToHuePicker(ColorHuePicker picker) {
        setHue(picker.getHueValue());
        picker.addHueChangeListener(this::setHue);
    }

    public void setHue(float currentHue) {
        mCurrentHue = currentHue;
        invalidate();
    }

    public int getColor() {
        mTmpHSV[0] = mCurrentHue;
        mTmpHSV[1] = mCurrentSaturation;
        mTmpHSV[2] = mCurrentValue;
        return Color.HSVToColor(mTmpHSV);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mSaturationGradient = new LinearGradient(getPaddingLeft(), 0,
                w - getPaddingRight(), 0,
                0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        mSaturationGradientPaint.setShader(mSaturationGradient);
        mValueGradient = new LinearGradient(0, getPaddingTop(), 0,
                h - getPaddingRight(),
                0x00000000, 0xFF000000, Shader.TileMode.CLAMP);
        mValueGradientPaint.setShader(mValueGradient);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE ||
                event.getAction() == MotionEvent.ACTION_UP) {
            mCurrentSaturation = (event.getX() - getPaddingLeft()) /
                    (getWidth() - getPaddingLeft() - getPaddingRight());
            mCurrentValue = 1.f - (event.getY() - getPaddingTop()) /
                    (getHeight() - getPaddingTop() - getPaddingBottom());
            mCurrentSaturation = Math.min(Math.max(mCurrentSaturation, 0.f), 1.f);
            mCurrentValue = Math.min(Math.max(mCurrentValue, 0.f), 1.f);
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mTmpHSV[0] = mCurrentHue;
        mTmpHSV[1] = 1.f;
        mTmpHSV[2] = 1.f;
        mFillPaint.setColor(Color.HSVToColor(mTmpHSV));
        mTmpRect.set(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        canvas.drawRect(mTmpRect, mFillPaint);
        canvas.drawRect(mTmpRect, mSaturationGradientPaint);
        canvas.drawRect(mTmpRect, mValueGradientPaint);
        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        int currentColor = getColor();
        mCircleInnerPaint.setColor(currentColor);
        if (Color.red(currentColor) < 128 && Color.green(currentColor) < 128 &&
                Color.blue(currentColor) < 128)
            mCirclePaint.setColor(0xFFFFFFFF);
        else
            mCirclePaint.setColor(0xFF000000);
        canvas.drawCircle(getPaddingLeft() + w * mCurrentSaturation, getPaddingTop() + h * (1.f - mCurrentValue), mCircleSize, mCircleInnerPaint);
        canvas.drawCircle(getPaddingLeft() + w * mCurrentSaturation, getPaddingTop() + h * (1.f - mCurrentValue), mCircleSize, mCirclePaint);
    }


}
