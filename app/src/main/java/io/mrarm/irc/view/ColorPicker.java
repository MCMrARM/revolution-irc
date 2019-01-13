package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class ColorPicker extends View {

    private int mCurrentHue = 0xFFFF0000;
    private Rect mTmpRect = new Rect();
    private LinearGradient mSaturationGradient;
    private Paint mSaturationGradientPaint;
    private LinearGradient mValueGradient;
    private Paint mValueGradientPaint;

    public ColorPicker(Context context) {
        this(context, null);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mSaturationGradientPaint = new Paint();
        mSaturationGradientPaint.setDither(true);
        mValueGradientPaint = new Paint();
        mValueGradientPaint.setDither(true);
    }

    public void setHue(int currentHue) {
        mCurrentHue = currentHue;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mSaturationGradient = new LinearGradient(0, 0, w, 0,
                0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        mSaturationGradientPaint.setShader(mSaturationGradient);
        mValueGradient = new LinearGradient(0, 0, 0, h,
                0x00000000, 0xFF000000, Shader.TileMode.CLAMP);
        mValueGradientPaint.setShader(mValueGradient);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawColor(mCurrentHue);
        mTmpRect.set(0, 0, getWidth(), getHeight());
        canvas.drawRect(mTmpRect, mSaturationGradientPaint);
        canvas.drawRect(mTmpRect, mValueGradientPaint);
    }


}
