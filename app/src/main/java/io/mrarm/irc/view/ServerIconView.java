package io.mrarm.irc.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ServerIconView extends View {

    private final Paint mPaint = new Paint();
    private final Rect mTmpRect = new Rect();
    private final RectF mTmpRectF = new RectF();
    private int mBgColor;
    private int mTextSize;
    private String mServerName;

    public ServerIconView(Context context) {
        this(context, null);
    }

    public ServerIconView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ServerIconView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ServerIconView);
        mTextSize = ta.getDimensionPixelSize(R.styleable.ServerIconView_android_textSize, 0);
        mServerName = ta.getString(R.styleable.ServerIconView_serverName);
        ta.recycle();

        mPaint.setAntiAlias(true);
        mBgColor = StyledAttributesHelper.getColor(context, R.attr.colorAccent, 0);
    }

    public void setServerName(String name) {
        if (name != null) {
            mServerName = name.substring(0, 1);
        } else {
            mServerName = null;
        }
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mTmpRectF.set(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        mPaint.setColor(mBgColor);
        canvas.drawOval(mTmpRectF, mPaint);

        if (mServerName == null || mServerName.isEmpty())
            return;

        mPaint.setColor(0xFFFFFFFF);
        mPaint.setTextSize(mTextSize);
        mPaint.getTextBounds(mServerName, 0, 1, mTmpRect);
        canvas.drawText(mServerName, getWidth() / 2.f - mTmpRect.exactCenterX(),
                getHeight() / 2.f - mTmpRect.exactCenterY(), mPaint);
    }
}
