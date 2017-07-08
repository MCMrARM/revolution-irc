package io.mrarm.irc.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.widget.ImageViewCompat;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import io.mrarm.irc.ColorPickerDialog;
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
    private ImageButton mExtraButton;
    private OnChangeListener mChangeListener;

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
        mExtraButton = (ImageButton) findViewById(R.id.format_extra);
        mBoldButton.setOnClickListener((View v) -> {
            if (v.isSelected())
                removeSpan(new StyleSpan(Typeface.BOLD));
            else
                setSpan(new StyleSpan(Typeface.BOLD));
        });
        mItalicButton.setOnClickListener((View v) -> {
            if (v.isSelected())
                removeSpan(new StyleSpan(Typeface.ITALIC));
            else
                setSpan(new StyleSpan(Typeface.ITALIC));
        });
        mUnderlineButton.setOnClickListener((View v) -> {
            if (v.isSelected())
                removeSpan(UnderlineSpan.class);
            else
                setSpan(new UnderlineSpan());
        });
        mTextColorButton.setOnClickListener((View v) -> {
            ColorPickerDialog dialog = new ColorPickerDialog(getContext());
            dialog.setTitle(R.string.format_text_color);
            ColorStateList list = ImageViewCompat.getImageTintList(mTextColorValue);
            if (list != mTextColorValueDefault)
                dialog.setSelectedColor(list.getDefaultColor());
            dialog.setPositiveButton(R.string.action_cancel, null);
            dialog.setOnColorChangeListener((ColorPickerDialog d, int newColorIndex, int color) -> {
                setSpan(new ForegroundColorSpan(color));
                d.cancel();
            });
            dialog.show();
        });
        mFillColorButton.setOnClickListener((View v) -> {
            ColorPickerDialog dialog = new ColorPickerDialog(getContext());
            dialog.setTitle(R.string.format_fill_color);
            ColorStateList list = ImageViewCompat.getImageTintList(mFillColorValue);
            if (list != mFillColorValueDefault)
                dialog.setSelectedColor(list.getDefaultColor());
            dialog.setPositiveButton(R.string.action_cancel, null);
            dialog.setOnColorChangeListener((ColorPickerDialog d, int newColorIndex, int color) -> {
                setSpan(new BackgroundColorSpan(color));
                d.cancel();
            });
            dialog.show();
        });
        mClearButton.setOnClickListener((View v) -> {
            removeSpan(Object.class);
        });
        mTextColorValueDefault = ImageViewCompat.getImageTintList(mTextColorValue);
        mFillColorValueDefault = ImageViewCompat.getImageTintList(mFillColorValue);
        ImageViewCompat.setImageTintMode(mTextColorValue, PorterDuff.Mode.SRC_IN);
        ImageViewCompat.setImageTintMode(mFillColorValue, PorterDuff.Mode.SRC_IN);

        mBoldButton.setOnLongClickListener(mExplainationListener);
        mItalicButton.setOnLongClickListener(mExplainationListener);
        mUnderlineButton.setOnLongClickListener(mExplainationListener);
        mTextColorButton.setOnLongClickListener(mExplainationListener);
        mFillColorButton.setOnLongClickListener(mExplainationListener);
        mClearButton.setOnLongClickListener(mExplainationListener);
        mExtraButton.setOnLongClickListener(mExplainationListener);
    }

    public void setEditText(FormattableEditText editText) {
        mEditText = editText;
        updateFormattingAtCursor();
    }

    public FormattableEditText getEditText() {
        return mEditText;
    }

    public void setExtraButton(int icon, CharSequence contentDesc, OnClickListener listener) {
        mExtraButton.setImageResource(icon);
        mExtraButton.setContentDescription(contentDesc);
        mExtraButton.setOnClickListener(listener);
        mExtraButton.setVisibility(View.VISIBLE);
    }

    public void setOnChangeListener(OnChangeListener listener) {
        mChangeListener = listener;
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

    private void notifyChange() {
        updateFormattingAtCursor();
        if (mChangeListener != null)
            mChangeListener.onChange(this, mEditText);
    }

    private void removeSpan(Class span) {
        SpannableStringHelper.removeSpans(mEditText.getText(), span, mEditText.getSelectionStart(), mEditText.getSelectionEnd(), null, true);
        notifyChange();
    }

    private void removeSpan(Object span) {
        SpannableStringHelper.removeSpans(mEditText.getText(), span.getClass(), mEditText.getSelectionStart(), mEditText.getSelectionEnd(), span, true);
        notifyChange();
    }

    private void setSpan(Object span) {
        SpannableStringHelper.setAndMergeSpans(mEditText.getText(), span, mEditText.getSelectionStart(), mEditText.getSelectionEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        notifyChange();
    }

    public interface OnChangeListener {
        void onChange(TextFormatBar formatBar, FormattableEditText editText);
    }

    private static OnLongClickListener mExplainationListener = (View view) -> {
        Toast toast = Toast.makeText(view.getContext(), view.getContentDescription(), Toast.LENGTH_SHORT);
        Rect rectWindow = new Rect();
        Rect rectView = new Rect();
        view.getGlobalVisibleRect(rectView);
        ((Activity) view.getContext()).getWindow().getDecorView().getGlobalVisibleRect(rectWindow);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, rectView.centerX() - rectWindow.centerX(), rectWindow.bottom - rectView.top);
        toast.show();
        return true;
    };

}
