package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

public class ColorHuePicker extends ColorSlider {

    private Bitmap mBitmap;
    private Paint mBmpPaint;
    private float[] mTmpHSV = new float[3];
    private Rect mBmpRect = new Rect();
    private RectF mTmpRectF = new RectF();


    public ColorHuePicker(Context context) {
        this(context, null);
    }

    public ColorHuePicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorHuePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBmpPaint = new Paint();
    }

    public int getColorValue() {
        mTmpHSV[0] = getValue();
        mTmpHSV[1] = 1.f;
        mTmpHSV[2] = 1.f;
        return Color.HSVToColor(mTmpHSV);
    }

    @Override
    protected float getMaxValue() {
        return 360.f;
    }

    @Override
    protected int getHandleFillColor() {
        return getColorValue();
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
        mBmpPaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP));
        mBmpRect.set(0, 0, 1, h);
    }

    @Override
    protected void drawBackground(Canvas canvas) {
        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (mBitmap == null || mBitmap.getHeight() != h)
            createBitmap();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        mTmpRectF.set(0, 0, w, h);
        canvas.drawRoundRect(mTmpRectF, mRadius, mRadius, mBmpPaint);
        canvas.translate(-getPaddingLeft(), -getPaddingTop());
    }

}
