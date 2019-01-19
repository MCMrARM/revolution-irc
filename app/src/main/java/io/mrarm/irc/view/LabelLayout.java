package io.mrarm.irc.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.StyledAttributesHelper;

public class LabelLayout extends FrameLayout {

    private FrameLayout mInputFrame;
    private View mChild;
    private CharSequence mHint;
    private Paint mTextPaint;
    private ColorStateList mTextColorUnfocused;
    private int mTextColorFocused;
    private boolean mChildWasFocused;
    private float mTextX = 0.f;
    private float mTextYCollapsed = 0.f;
    private float mTextYExpanded = 0.f;
    private int mTextSizeCollapsed = 0;
    private int mTextSizeExpanded = 0;
    private float mAnimState = 1.f;
    private boolean mDoNotExpand = false;
    private ValueAnimator mAnimator;
    private Interpolator mTextSizeInterpolator = new FastOutSlowInInterpolator();
    private Interpolator mTextYInterpolator = new AccelerateInterpolator();

    public LabelLayout(Context context) {
        this(context, null);
    }

    public LabelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LabelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(false);
        setAddStatesFromChildren(true);

        mInputFrame = new FrameLayout(context);
        mInputFrame.setAddStatesFromChildren(true);
        super.addView(mInputFrame, -1, generateDefaultLayoutParams());

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        mTextPaint.setTypeface(Typeface.DEFAULT);// getTypeface());
        mTextSizeCollapsed = getResources().getDimensionPixelSize(R.dimen.abc_text_size_caption_material);
        mTextSizeExpanded = getResources().getDimensionPixelSize(R.dimen.abc_text_size_medium_material);
        mTextPaint.setTextSize(mTextSizeCollapsed);

        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(context, attrs,
                new int[] { R.attr.colorAccent, R.attr.doNotExpand, android.R.attr.hint, android.R.attr.textColorHint });
        try {
            mDoNotExpand = ta.getBoolean(R.attr.doNotExpand, false);
            mHint = ta.getString(android.R.attr.hint);
            mTextColorUnfocused = ta.getColorStateList(android.R.attr.textColorHint);
            mTextColorFocused = ta.getColor(R.attr.colorAccent, 0);
        } finally {
            ta.recycle();
        }
        mTextPaint.setColor(mTextColorUnfocused.getColorForState(getDrawableState(), mTextColorUnfocused.getDefaultColor()));

        mAnimator = ValueAnimator.ofFloat(0.f, 1.f);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setDuration(200);
        mAnimator.addUpdateListener((ValueAnimator animation) -> {
            mAnimState = (float) animation.getAnimatedValue();
            invalidate();
        });

        updateTopMargin();
        updateTextPositions();
    }

    public void setHint(CharSequence hint) {
        mHint = hint;
        updateTopMargin();
    }

    public void setExpanded(boolean expanded, boolean animated) {
        if (mDoNotExpand)
            expanded = false;
        if ((mAnimState <= 0.f || mAnimState >= 1.f) && expanded == (mAnimState == 1.f) && !mAnimator.isRunning())
            return;
        if (animated) {
            mAnimator.setFloatValues(mAnimState, (expanded ? 1.f : 0.f));
            mAnimator.start();
        } else {
            mAnimator.cancel();
            mAnimState = (expanded ? 1.f : 0.f);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateTextPositions();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        mChild = child;
        mInputFrame.addView(child, index, params);
        updateTopMargin();
        if (child instanceof ChipsEditText) {
            ((ChipsEditText) child).addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
                updateLabelExpandState(false);
            }));
        } else if (child instanceof EditText) {
            ((EditText) child).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    updateLabelExpandState(false);
                }
            });
        }
        updateLabelExpandState(false);
    }

    private void updateLabelExpandState(boolean animated) {
        setExpanded(isChildEmpty() && !hasFocusedChild(LabelLayout.this), animated);
    }

    private static boolean hasFocusedChild(View view) {
        if (view.isFocused())
            return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasFocusedChild(group.getChildAt(i)))
                    return true;
            }
        }
        return false;
    }

    private boolean isChildEmpty() {
        if (mChild == null)
            return false;
        if (mChild instanceof ChipsEditText)
            return ((ChipsEditText) mChild).isEmpty();
        if (mChild instanceof TextView)
            return ((TextView) mChild).getText().length() == 0;
        return false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mChild != null) {
            boolean focused = hasFocusedChild(this);
            if (focused)
                mTextPaint.setColor(mTextColorFocused);
            else
                mTextPaint.setColor(mTextColorUnfocused.getColorForState(getDrawableState(), mTextColorUnfocused.getDefaultColor()));
            if (mChildWasFocused != focused) {
                mChildWasFocused = focused;
                updateLabelExpandState(true);
                invalidate();
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        int restorePoint = canvas.save();
        if (mHint != null) {
            mAnimState = Math.min(Math.max(mAnimState, 0.f), 1.f);
            float textY = mTextYCollapsed + (mTextYExpanded - mTextYCollapsed) * (1.f - mTextYInterpolator.getInterpolation(1.f - mAnimState));
            int textSize = (mAnimState > 0.f ? mTextSizeExpanded : mTextSizeCollapsed);
            mTextPaint.setTextSize(textSize);
            mTextPaint.setLinearText(false);
            if (mAnimState > 0.f && mAnimState < 1.f) {
                float scale = (mTextSizeCollapsed + (mTextSizeExpanded - mTextSizeCollapsed) * (1.f - mTextSizeInterpolator.getInterpolation(1.f - mAnimState))) / mTextSizeExpanded;
                canvas.scale(scale, scale, mTextX, textY);
                mTextPaint.setLinearText(true);
            }
            canvas.drawText(mHint, 0, mHint.length(), mTextX, textY, this.mTextPaint);
        }

        canvas.restoreToCount(restorePoint);
    }


    private void updateTextPositions() {
        if (mChild == null)
            return;
        mTextX = mChild.getLeft() + mChild.getPaddingLeft();
        mTextPaint.setTextSize(mTextSizeCollapsed);
        mTextYCollapsed = getPaddingTop() - mTextPaint.ascent();
        mTextPaint.setTextSize(mTextSizeExpanded);
        int childTop = mChild.getTop() + (mChild instanceof TextView ? ((TextView) mChild).getCompoundPaddingTop() : mChild.getPaddingTop());
        int childBot = mChild.getBottom() - (mChild instanceof TextView ? ((TextView) mChild).getCompoundPaddingBottom() : mChild.getPaddingBottom());
        mTextYExpanded = mInputFrame.getTop() + (childTop + childBot) / 2;
        mTextYExpanded += (mTextPaint.descent() - mTextPaint.ascent()) / 2 - mTextPaint.descent();
    }

    private void updateTopMargin() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mInputFrame.getLayoutParams();
        mTextPaint.setTextSize(mTextSizeCollapsed);
        int newTopMargin = (int) -mTextPaint.ascent();
        if (newTopMargin != lp.topMargin) {
            lp.topMargin = newTopMargin;
            mInputFrame.requestLayout();
            requestLayout();
        }
    }

}