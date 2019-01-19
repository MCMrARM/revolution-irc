package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ColorPicker extends View {

    private float mCurrentHue = 0.f;
    private float mCurrentSaturation = 1.f;
    private float mCurrentValue = 1.f;
    private Paint mFillPaint;
    private RectF mTmpRectF = new RectF();
    private LinearGradient mSaturationGradient;
    private Paint mSaturationGradientPaint;
    private LinearGradient mValueGradient;
    private Paint mValueGradientPaint;
    private float mRadius;
    private Paint mCirclePaint;
    private Paint mCircleInnerPaint;
    private float mCircleSize;
    private float mCircleTouchSize;
    private boolean mCircleDragging = false;
    private float mTouchStartX;
    private float mTouchStartY;
    private float mTouchTapMaxDist;
    private float[] mTmpHSV = new float[3];
    private ColorHuePicker mHuePicker;
    private List<ColorChangeListener> mListeners = new ArrayList<>();

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
        mCircleTouchSize = mCircleSize * 2.5f;
        mTouchTapMaxDist = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                8.f, getResources().getDisplayMetrics());
        mRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2.f, getResources().getDisplayMetrics());
    }

    public void attachToHuePicker(ColorHuePicker picker) {
        mHuePicker = picker;
        setHue(picker.getValue());
        picker.addValueChangeListener(this::setHue);
    }

    public void addColorChangeListener(ColorChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeColorChangeListener(ColorChangeListener listener) {
        mListeners.remove(listener);
    }

    public void setHue(float currentHue) {
        mCurrentHue = currentHue;
        invalidate();
        int newColor = getColor();
        for (ColorChangeListener listener : mListeners)
            listener.onColorChanged(newColor);
    }

    public void setColor(int color) {
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), mTmpHSV);
        mCurrentSaturation = mTmpHSV[1];
        mCurrentValue = mTmpHSV[2];
        if (mHuePicker != null)
            mHuePicker.setValue(mTmpHSV[0]); // will call setHue from callback
        else
            setHue(mTmpHSV[0]);
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

    private float getHandleX() {
        return getPaddingLeft() +
                (getWidth() - getPaddingLeft() - getPaddingRight()) * mCurrentSaturation;
    }

    private float getHandleY() {
        return getPaddingTop() +
                (getHeight() - getPaddingTop() - getPaddingBottom()) * (1.f - mCurrentValue);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float handleX = getHandleX();
            float handleY = getHandleY();
            mTouchStartX = event.getX();
            mTouchStartY = event.getY();
            if (event.getX() >= handleX - mCircleTouchSize &&
                    event.getX() <= handleX + mCircleTouchSize &&
                    event.getY() >= handleY - mCircleTouchSize &&
                    event.getY() <= handleY + mCircleTouchSize) {
                mCircleDragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        boolean shouldUpdatePosAnyway = false;
        if (event.getAction() == MotionEvent.ACTION_UP && !mCircleDragging &&
                Math.abs(event.getX() - mTouchStartX) < mTouchTapMaxDist &&
                Math.abs(event.getY() - mTouchStartY) < mTouchTapMaxDist) {
            shouldUpdatePosAnyway = true;
        }
        if ((mCircleDragging && (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE ||
                event.getAction() == MotionEvent.ACTION_UP)) || shouldUpdatePosAnyway) {
            mCurrentSaturation = (event.getX() - getPaddingLeft()) /
                    (getWidth() - getPaddingLeft() - getPaddingRight());
            mCurrentValue = 1.f - (event.getY() - getPaddingTop()) /
                    (getHeight() - getPaddingTop() - getPaddingBottom());
            mCurrentSaturation = Math.min(Math.max(mCurrentSaturation, 0.f), 1.f);
            mCurrentValue = Math.min(Math.max(mCurrentValue, 0.f), 1.f);
            invalidate();
            int newColor = getColor();
            for (ColorChangeListener listener : mListeners)
                listener.onColorChanged(newColor);
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL ||
                event.getAction() == MotionEvent.ACTION_UP) {
            getParent().requestDisallowInterceptTouchEvent(false);
            mCircleDragging = false;
        }
        return (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE ||
                event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) || super.onTouchEvent(event);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mTmpHSV[0] = mCurrentHue;
        mTmpHSV[1] = 1.f;
        mTmpHSV[2] = 1.f;
        mFillPaint.setColor(Color.HSVToColor(mTmpHSV));
        mTmpRectF.set(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        canvas.drawRoundRect(mTmpRectF, mRadius, mRadius, mFillPaint);
        canvas.drawRoundRect(mTmpRectF, mRadius, mRadius, mSaturationGradientPaint);
        canvas.drawRoundRect(mTmpRectF, mRadius, mRadius, mValueGradientPaint);
        int currentColor = getColor();
        mCircleInnerPaint.setColor(currentColor);
        if (Color.red(currentColor) > 140 && Color.green(currentColor) > 140 &&
                Color.blue(currentColor) > 140)
            mCirclePaint.setColor(0xFF000000);
        else
            mCirclePaint.setColor(0xFFFFFFFF);
        float circleX = getHandleX();
        float circleY = getHandleY();
        canvas.drawCircle(circleX, circleY, mCircleSize, mCircleInnerPaint);
        canvas.drawCircle(circleX, circleY, mCircleSize, mCirclePaint);
    }


    public interface ColorChangeListener {

        void onColorChanged(int newColor);

    }

}
