package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.google.android.material.textfield.TextInputLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class StaticLabelTextInputLayout extends TextInputLayout {

    private boolean mForceShowHint = false;
    private boolean mWasHintEnabled = false;
    private CharSequence mForceHint;
    private Paint mTextPaint;
    private int mTextColorUnfocused;
    private int mTextColorFocused;
    private boolean mEditTextWasFocused;
    private float mTextX = 0.f;
    private float mTextY = 0.f;

    public StaticLabelTextInputLayout(Context context) {
        this(context, null);
    }

    public StaticLabelTextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaticLabelTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        mTextPaint.setTypeface(getTypeface());
        mTextPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.abc_text_size_caption_material));
        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(context,
                new int[] { R.attr.colorAccent, android.R.attr.textColorHint });
        mTextColorUnfocused = ta.getColor(android.R.attr.textColorHint, 0);
        mTextColorFocused = ta.getColor(R.attr.colorAccent, 0);
        ta.recycle();
        mTextPaint.setColor(mTextColorUnfocused);
    }

    @Override
    public boolean isHintEnabled() {
        return super.isHintEnabled() || mForceShowHint;
    }

    @Override
    public void setHintEnabled(boolean enabled) {
        if (mForceShowHint) {
            mWasHintEnabled = enabled;
            return;
        }
        super.setHintEnabled(enabled);
    }

    public void setForceShowHint(boolean enabled, CharSequence hint) {
        CharSequence oldHint = mForceHint;
        mForceHint = hint;
        if (enabled == mForceShowHint)
            return;
        mWasHintEnabled = isHintEnabled();
        if (enabled)
            setHintEnabled(false);
        mForceShowHint = enabled;

        FrameLayout inputFrame = (FrameLayout) getChildAt(0);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) inputFrame.getLayoutParams();
        int newTopMargin = 0;
        if (enabled) {
            newTopMargin = (int) -mTextPaint.ascent();
        }

        if(newTopMargin != lp.topMargin) {
            lp.topMargin = newTopMargin;
            inputFrame.requestLayout();
        }

        if (!enabled) {
            setHintEnabled(mWasHintEnabled);
            setHint(oldHint);
        }
    }

    public void setForceShowHint(boolean enabled) {
        setForceShowHint(enabled, enabled ? getHint() : null);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getEditText() != null) {
            mTextX = getEditText().getLeft() + getEditText().getCompoundPaddingLeft();
            mTextY = getPaddingTop();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (getEditText() != null) {
            boolean focused = getEditText().isFocused();
            if (mEditTextWasFocused != focused) {
                mEditTextWasFocused = focused;
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
        if (mForceShowHint && mForceHint != null)
            canvas.drawText(mForceHint, 0, mForceHint.length(), mTextX, mTextY - mTextPaint.ascent(), this.mTextPaint);

        canvas.restoreToCount(restorePoint);
    }

}