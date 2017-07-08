package io.mrarm.irc.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import io.mrarm.irc.R;

public class TextFormatBar extends FrameLayout {

    private FormattableEditText mEditText;
    private View mBoldButton;
    private View mItalicButton;
    private View mUnderlineButton;
    private View mTextColorButton;
    private ImageView mTextColorValue;
    private ColorStateList mTextColorValueDefault;
    private View mFillColorButton;
    private ImageView mFillColorValue;
    private ColorStateList mFillColorValueDefault;
    private View mClearButton;

    public TextFormatBar(Context context) {
        this(context, null);
    }

    public TextFormatBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.textFormatBarStyle);
    }

    public TextFormatBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(getContext(), R.layout.format_bar, this);
        mBoldButton = findViewById(R.id.format_bold);
        mItalicButton = findViewById(R.id.format_italic);
        mUnderlineButton = findViewById(R.id.format_underline);
        mTextColorButton = findViewById(R.id.format_text_color);
        mTextColorValue = (ImageView) findViewById(R.id.format_text_color_value);
        mFillColorButton = findViewById(R.id.format_fill_color);
        mFillColorValue = (ImageView) findViewById(R.id.format_fill_color_value);
        mClearButton = findViewById(R.id.format_clear);
        mBoldButton.setOnClickListener((View v) -> {
            if (v.isSelected())
                removeSpan(new StyleSpan(Typeface.BOLD));
            else
                setSpan(new StyleSpan(Typeface.BOLD));
            updateFormattingAtCursor();
        });
        mItalicButton.setOnClickListener((View v) -> {
            if (v.isSelected())
                removeSpan(new StyleSpan(Typeface.ITALIC));
            else
                setSpan(new StyleSpan(Typeface.ITALIC));
            updateFormattingAtCursor();
        });
        mUnderlineButton.setOnClickListener((View v) -> {
            if (v.isSelected())
                removeSpan(UnderlineSpan.class);
            else
                setSpan(new UnderlineSpan());
            updateFormattingAtCursor();
        });
        mTextColorButton.setOnClickListener((View v) -> {
            //
        });
        mFillColorButton.setOnClickListener((View v) -> {
            //
        });
        mClearButton.setOnClickListener((View v) -> {
            removeSpan(Object.class);
            updateFormattingAtCursor();
        });
        mTextColorValueDefault = ImageViewCompat.getImageTintList(mTextColorValue);
        mFillColorValueDefault = ImageViewCompat.getImageTintList(mFillColorValue);
        ImageViewCompat.setImageTintMode(mTextColorValue, PorterDuff.Mode.SRC_IN);
        ImageViewCompat.setImageTintMode(mFillColorValue, PorterDuff.Mode.SRC_IN);
    }

    public void setEditText(FormattableEditText editText) {
        mEditText = editText;
        updateFormattingAtCursor();
    }

    public void updateFormattingAtCursor() {
        if (mEditText == null)
            return;
        Editable text = mEditText.getText();
        int start = mEditText.getSelectionStart();
        int end = mEditText.getSelectionEnd();
        Object[] spans = text.getSpans(start, end, Object.class);

        mBoldButton.setSelected(false);
        mItalicButton.setSelected(false);
        mUnderlineButton.setSelected(false);

        int fgColor = -1;
        int bgColor = -1;

        for (Object span : spans) {
            int pointFlags = text.getSpanFlags(span) & Spanned.SPAN_POINT_MARK_MASK;
            boolean includesEnd = pointFlags == Spanned.SPAN_EXCLUSIVE_INCLUSIVE ||
                    pointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE;
            boolean includesStart = pointFlags == Spanned.SPAN_INCLUSIVE_EXCLUSIVE ||
                    pointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE;
            if (text.getSpanStart(span) > start || text.getSpanEnd(span) < end ||
                    (text.getSpanEnd(span) == end && !includesEnd) ||
                    (text.getSpanStart(span) == start && !includesStart))
                continue;
            if (span instanceof StyleSpan) {
                int style = ((StyleSpan) span).getStyle();
                if (style == Typeface.BOLD)
                    mBoldButton.setSelected(true);
                else if (style == Typeface.ITALIC)
                    mItalicButton.setSelected(true);
            } else if (span instanceof UnderlineSpan) {
                mUnderlineButton.setSelected(true);
            } else if (span instanceof ForegroundColorSpan) {
                fgColor = ((ForegroundColorSpan) span).getForegroundColor();
            } else if (span instanceof BackgroundColorSpan) {
                bgColor = ((BackgroundColorSpan) span).getBackgroundColor();
            }
        }

        ImageViewCompat.setImageTintList(mTextColorValue, fgColor != -1
                ? ColorStateList.valueOf(fgColor) : mTextColorValueDefault);
        ImageViewCompat.setImageTintList(mFillColorValue, bgColor != -1
                ? ColorStateList.valueOf(bgColor) : mFillColorValueDefault);
    }

    private void removeSpan(Class span) {
        SpannableStringHelper.removeSpans(mEditText.getText(), span, mEditText.getSelectionStart(), mEditText.getSelectionEnd(), null, true);
    }

    private void removeSpan(Object span) {
        SpannableStringHelper.removeSpans(mEditText.getText(), span.getClass(), mEditText.getSelectionStart(), mEditText.getSelectionEnd(), span, true);
    }

    private void setSpan(Object span) {
        SpannableStringHelper.setAndMergeSpans(mEditText.getText(), span, mEditText.getSelectionStart(), mEditText.getSelectionEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

}
