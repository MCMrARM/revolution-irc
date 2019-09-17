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
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.Observable;

import io.mrarm.irc.BR;
import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ServerIconView extends View {

    private final Paint mPaint = new Paint();
    private final Rect mTmpRect = new Rect();
    private final RectF mTmpRectF = new RectF();
    private int mDefaultBgColor;
    private int mTextSize;
    private String mServerName;
    private CustomizationInfo mCustomizationInfo;
    private boolean mAttachedToWindow;
    private Observable.OnPropertyChangedCallback mCustomizationChangedCb =
            new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    invalidate();
                }
            };

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
        mDefaultBgColor = StyledAttributesHelper.getColor(context, R.attr.colorAccent, 0);
    }

    public void setServerName(String name) {
        if (name != null) {
            mServerName = name.substring(0, 1);
        } else {
            mServerName = null;
        }
        invalidate();
    }

    public void setIconCustomization(CustomizationInfo info) {
        if (mAttachedToWindow && mCustomizationInfo != null)
            mCustomizationInfo.removeOnPropertyChangedCallback(mCustomizationChangedCb);
        mCustomizationInfo = info;
        if (mAttachedToWindow)
            info.addOnPropertyChangedCallback(mCustomizationChangedCb);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        if (mCustomizationInfo != null)
            mCustomizationInfo.addOnPropertyChangedCallback(mCustomizationChangedCb);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
        if (mCustomizationInfo != null)
            mCustomizationInfo.removeOnPropertyChangedCallback(mCustomizationChangedCb);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mTmpRectF.set(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        mPaint.setColor(mDefaultBgColor);
        if (mCustomizationInfo != null && mCustomizationInfo.getColor() != -1)
            mPaint.setColor(mCustomizationInfo.getColor());
        canvas.drawOval(mTmpRectF, mPaint);

        String serverName = mServerName;
        if (mCustomizationInfo != null && mCustomizationInfo.getCustomText() != null)
            serverName = mCustomizationInfo.getCustomText();
        if (serverName == null || serverName.isEmpty())
            return;

        mPaint.setColor(0xFFFFFFFF);
        mPaint.setTextSize(mTextSize);
        mPaint.getTextBounds(serverName, 0, 1, mTmpRect);
        canvas.drawText(serverName, getWidth() / 2.f - mTmpRect.exactCenterX(),
                getHeight() / 2.f - mTmpRect.exactCenterY(), mPaint);
    }


    public static class CustomizationInfo extends BaseObservable {

        private int mColor = -1;
        private int mColorResId = -1;
        private String mCustomText;

        @Bindable
        public int getColor() {
            return mColor;
        }

        public void setColor(int color) {
            mColor = color;
            notifyPropertyChanged(BR.color);
        }

        @Bindable
        public String getCustomText() {
            return mCustomText;
        }

        public void setCustomText(String text) {
            mCustomText = text;
            notifyPropertyChanged(BR.customText);
        }

    }

}
