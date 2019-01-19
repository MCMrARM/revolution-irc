package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ColorSlider extends View {

    private static final long MAX_TAP_TIME = 100000000L; // 0.1s, in ns

    private RectF mTmpRectF = new RectF();
    private float mCurrentValue = 0.f;
    protected float mRadius;

    private Paint mHandlePaint;
    private Paint mHandleInnerPaint;
    private float mHandleHeight;
    private float mHandleTouchHeight;
    private float mHandleRadius;
    private boolean mHandleDragging;
    private boolean mHandlePossiblyTap;
    private float mTouchStartX;
    private float mTouchStartY;
    private long mTouchStartTime;
    private float mTouchPrevY;
    private float mTouchTapMaxDist;

    private List<ValueChangeListener> mListeners = new ArrayList<>();

    public ColorSlider(Context context) {
        this(context, null);
    }

    public ColorSlider(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorSlider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHandlePaint = new Paint();
        mHandlePaint.setColor(0xFFFFFFFF);
        mHandlePaint.setStyle(Paint.Style.STROKE);
        mHandlePaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                3.f, getResources().getDisplayMetrics()));
        mHandleInnerPaint = new Paint();
        mHandleHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                8.f, getResources().getDisplayMetrics());
        mHandleTouchHeight = mHandleHeight * 2.5f;
        mTouchTapMaxDist = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                16.f, getResources().getDisplayMetrics());
        mRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2.f, getResources().getDisplayMetrics());
        mHandleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2.f, getResources().getDisplayMetrics());
    }

    public void addValueChangeListener(ValueChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeValueChangeListener(ValueChangeListener listener) {
        mListeners.remove(listener);
    }

    public float getValue() {
        return mCurrentValue;
    }

    protected float getMinValue() {
        return 0.f;
    }

    protected float getMaxValue() {
        return 1.f;
    }

    public void setValue(float val) {
        mCurrentValue = Math.min(Math.max(val, Math.min(getMinValue(), getMaxValue())),
                Math.max(getMinValue(), getMaxValue()));
        invalidate();
        for (ValueChangeListener listener : mListeners)
            listener.onValueChanged(mCurrentValue);
    }

    protected float getHandleY() {
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        return getPaddingTop() + h * (mCurrentValue - getMinValue())
                / (getMaxValue() -  getMinValue());
    }

    protected int getHandleFillColor() {
        return 0xFFFFFFFF;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float handleY = getHandleY();
            mTouchStartX = event.getX();
            mTouchStartY = event.getY();
            mTouchStartTime = System.nanoTime();
            mTouchPrevY = mTouchStartY;
            mHandleDragging = true;
            mHandlePossiblyTap = true;
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        if ((event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_MOVE) &&
                (Math.abs(event.getX() - mTouchStartX) >= mTouchTapMaxDist ||
                        Math.abs(event.getY() - mTouchStartY) >= mTouchTapMaxDist)) {
            mHandlePossiblyTap = false;
        }
        if (event.getAction() == MotionEvent.ACTION_UP && mHandlePossiblyTap &&
                System.nanoTime() - mTouchStartTime < MAX_TAP_TIME) {
            float val = getMinValue() + (event.getY() - getPaddingTop()) /
                    (getHeight() - getPaddingTop() - getPaddingBottom()) *
                    (getMaxValue() - getMinValue());
            setValue(val);
        } else if (mHandleDragging && (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE ||
                event.getAction() == MotionEvent.ACTION_UP)) {
            float val = mCurrentValue + (event.getY() - mTouchPrevY) /
                    (getHeight() - getPaddingTop() - getPaddingBottom()) *
                    (getMaxValue() - getMinValue());
            setValue(val);
            mTouchPrevY = event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL ||
                event.getAction() == MotionEvent.ACTION_UP) {
            getParent().requestDisallowInterceptTouchEvent(false);
            mHandleDragging = false;
        }
        return (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE ||
                event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) || super.onTouchEvent(event);
    }

    protected void drawBackground(Canvas canvas) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        float handleY = getHandleY();
        mTmpRectF.set(getPaddingLeft(), handleY - mHandleHeight / 2.f,
                getWidth() - getPaddingRight(), handleY + mHandleHeight / 2.f);
        mHandleInnerPaint.setColor(getHandleFillColor());
        canvas.drawRoundRect(mTmpRectF, mHandleRadius, mHandleRadius, mHandleInnerPaint);
        canvas.drawRoundRect(mTmpRectF, mHandleRadius, mHandleRadius, mHandlePaint);
    }

    public interface ValueChangeListener {

        void onValueChanged(float newHue);

    }

}
