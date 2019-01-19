package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;

public class ColorAlphaPicker extends ColorSlider {

    private Bitmap mBitmap;
    private Paint mBmpPaint;
    private RectF mTmpRectF = new RectF();
    private float mTileSize;
    private LinearGradient mValueGradient;
    private Paint mValueGradientPaint;
    private int mBaseColor;

    public ColorAlphaPicker(Context context) {
        this(context, null);
    }

    public ColorAlphaPicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorAlphaPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBmpPaint = new Paint();
        mTileSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                4.f, getResources().getDisplayMetrics());
        createCheckerboard();
        mValueGradientPaint = new Paint();
        mValueGradientPaint.setDither(true);
        setColor(0xFFFFFFFF);
    }

    @Override
    protected float getMinValue() {
        return 1.f;
    }

    @Override
    protected float getMaxValue() {
        return 0.f;
    }

    public void attachToPicker(ColorPicker picker) {
        setColor(picker.getColor());
        picker.addColorChangeListener(this::setColor);
    }

    public void setColor(int color) {
        color |= 0xFF000000;
        mBaseColor = color;
        mValueGradientPaint.setColorFilter(
                new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        invalidate();
    }

    @Override
    protected int getHandleFillColor() {
        return mBaseColor;
    }

    private void createCheckerboard() {
        mBitmap = Bitmap.createBitmap((int) (mTileSize * 2), (int) (mTileSize * 2),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(mBitmap);
        Paint p = new Paint();
        p.setColor(0xFFEEEEEE);
        c.drawRect(0.f, 0.f, mTileSize, mTileSize, p);
        c.drawRect(mTileSize, mTileSize, mTileSize * 2, mTileSize * 2, p);
        p.setColor(0xFF757575);
        c.drawRect(mTileSize, 0, mTileSize * 2, mTileSize, p);
        c.drawRect(0, mTileSize, mTileSize, mTileSize * 2, p);
        mBmpPaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mValueGradient = new LinearGradient(0, getPaddingTop(), 0,
                h - getPaddingRight(),
                0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        mValueGradientPaint.setShader(mValueGradient);
    }

    @Override
    protected void drawBackground(Canvas canvas) {
        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        mTmpRectF.set(0, 0, w, h);
        canvas.drawRoundRect(mTmpRectF, mRadius, mRadius, mBmpPaint);
        mValueGradientPaint.setColor(0xFFFF0000);
        canvas.drawRoundRect(mTmpRectF, mRadius, mRadius, mValueGradientPaint);
        canvas.restore();
    }
}
