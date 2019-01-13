package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ColorHuePicker extends View {

    private Bitmap mBitmap;
    private Paint mPaint;
    private float[] mTmpHSV = new float[3];
    private Rect mBmpRect = new Rect();
    private Rect mTmpRect = new Rect();
    private RectF mTmpRectF = new RectF();
    private float mCurrentValue = 0.f;

    private Paint mHandlePaint;
    private Paint mHandleInnerPaint;
    private float mHandleHeight;

    private List<HueChangeListener> mListeners = new ArrayList<>();

    public ColorHuePicker(Context context) {
        this(context, null);
    }

    public ColorHuePicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorHuePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mHandlePaint = new Paint();
        mHandlePaint.setColor(0xFF000000);
        mHandlePaint.setStyle(Paint.Style.STROKE);
        mHandlePaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                3.f, getResources().getDisplayMetrics()));
        mHandleInnerPaint = new Paint();
        mHandleHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                8.f, getResources().getDisplayMetrics());
    }

    public void addHueChangeListener(HueChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeHueChangeListener(HueChangeListener listener) {
        mListeners.remove(listener);
    }

    public float getHueValue() {
        return mCurrentValue;
    }

    public int getColorValue() {
        mTmpHSV[0] = mCurrentValue;
        mTmpHSV[1] = 1.f;
        mTmpHSV[2] = 1.f;
        return Color.HSVToColor(mTmpHSV);
    }

    public void setHueValue(float val) {
        mCurrentValue = val;
        invalidate();
        for (HueChangeListener listener : mListeners)
            listener.onHueChanged(val);
    }

    private void createBitmap() {
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        mBitmap = Bitmap.createBitmap(1, h, Bitmap.Config.ARGB_8888);
        mTmpHSV[1] = 1.f;
        mTmpHSV[2] = 1.f;
        for (int i = 0; i < h; i++) {
            mTmpHSV[0] = (float) i / (h - 1) * 360.f;
            mBitmap.setPixel(0, i, Color.HSVToColor(mTmpHSV));
        }
        mBmpRect.set(0, 0, 1, h);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE ||
                event.getAction() == MotionEvent.ACTION_UP) {
            float val = (event.getY() - getPaddingTop()) /
                    (getHeight() - getPaddingTop() - getPaddingBottom()) * 360.f;
            val = Math.min(Math.max(val, 0.f), 360.f);
            setHueValue(val);
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (mBitmap == null || mBitmap.getHeight() != h)
            createBitmap();
        mTmpRect.set(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        canvas.drawBitmap(mBitmap, mBmpRect, mTmpRect, mPaint);
        float handleY = getPaddingTop() + h * mCurrentValue / 360.f;
        mTmpRectF.set(getPaddingLeft(), handleY - mHandleHeight / 2.f,
                getWidth() - getPaddingRight(), handleY + mHandleHeight / 2.f);
        mHandleInnerPaint.setColor(getColorValue());
        canvas.drawRect(mTmpRectF, mHandleInnerPaint);
        canvas.drawRect(mTmpRectF, mHandlePaint);
    }

    public interface HueChangeListener {

        void onHueChanged(float newHue);

    }

}
