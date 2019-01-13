package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class ColorHuePicker extends View {

    private Bitmap mBitmap;
    private Paint mPaint;
    private float[] mTmpHSV = new float[3];
    private Rect mBmpRect = new Rect();
    private Rect mTmpRect = new Rect();

    public ColorHuePicker(Context context) {
        this(context, null);
    }

    public ColorHuePicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorHuePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
    }

    private void createBitmap() {
        int h = getHeight();
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap == null || mBitmap.getHeight() != getHeight())
            createBitmap();
        mTmpRect.set(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(mBitmap, mBmpRect, mTmpRect, mPaint);
    }
}
