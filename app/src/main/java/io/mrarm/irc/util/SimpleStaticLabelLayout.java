package io.mrarm.irc.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Spinner;

import io.mrarm.irc.R;

public class SimpleStaticLabelLayout extends FrameLayout {

    private FrameLayout mInputFrame;
    private View mChild;
    private CharSequence mHint;
    private Paint mTextPaint;
    private int mTextColorUnfocused;
    private int mTextColorFocused;
    private boolean mChildWasFocused;
    private float mTextX = 0.f;
    private float mTextY = 0.f;

    public SimpleStaticLabelLayout(Context context) {
        this(context, null);
    }

    public SimpleStaticLabelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleStaticLabelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(false);
        setAddStatesFromChildren(true);

        mInputFrame = new FrameLayout(context);
        mInputFrame.setAddStatesFromChildren(true);
        super.addView(mInputFrame, -1, generateDefaultLayoutParams());

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        mTextPaint.setTypeface(Typeface.DEFAULT);// getTypeface());
        mTextPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.abc_text_size_caption_material));

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs,
                new int[] { android.R.attr.hint, android.R.attr.textColorHint, R.attr.colorAccent },
                0, 0);
        try {
            mHint = ta.getString(0);
            mTextColorUnfocused = ta.getColor(1, 0);
            mTextColorFocused = ta.getColor(2, 0);
        } finally {
            ta.recycle();
        }
        mTextPaint.setColor(mTextColorUnfocused);
    }


    public void setHint(CharSequence hint) {
        mHint = hint;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInputFrame.getLayoutParams();
        int newTopMargin = (int) -mTextPaint.ascent();

        if (newTopMargin != lp.topMargin) {
            lp.topMargin = newTopMargin;
            mInputFrame.requestLayout();
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mChild != null) {
            mTextX = mChild.getLeft() + mChild.getPaddingLeft();
            mTextY = getPaddingTop();
            setHint(mHint);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        mChild = child;
        mInputFrame.addView(child, index, params);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mChild != null) {
            boolean focused = mChild.isFocused();
            if (mChildWasFocused != focused) {
                mChildWasFocused = focused;
                if (focused)
                    mTextPaint.setColor(mTextColorFocused);
                else
                    mTextPaint.setColor(mTextColorUnfocused);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        int restorePoint = canvas.save();
        if (mHint != null)
            canvas.drawText(mHint, 0, mHint.length(), mTextX, mTextY - mTextPaint.ascent(), this.mTextPaint);

        canvas.restoreToCount(restorePoint);
    }

}