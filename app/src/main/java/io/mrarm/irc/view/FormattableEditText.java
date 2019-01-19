package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Rect;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

public class FormattableEditText extends AppCompatEditText {

    private TextFormatBar mFormatBar;
    private boolean mSettingText = false;

    public FormattableEditText(Context context) {
        super(context);
        init();
    }

    public FormattableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FormattableEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        addTextChangedListener(new TextWatcher() {
            private List<SpanData> mBackedUpSpans = new ArrayList<>();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (mSettingText)
                    return;
                int selStart = getSelectionStart();
                for (Object span : getText().getSpans(start, start + count, Object.class)) {
                    int flags = getText().getSpanFlags(span);
                    if (span instanceof NoCopySpan || (flags & Spanned.SPAN_COMPOSING) != 0
                            || (flags & Spanned.SPAN_PARAGRAPH) != 0)
                        continue;
                    SpanData data = new SpanData();
                    data.span = span;
                    data.start = getText().getSpanStart(span);
                    data.end = getText().getSpanEnd(span);
                    data.flags = flags;
                    int spanPointFlags = flags & Spanned.SPAN_POINT_MARK_MASK;
                    if ((data.start >= selStart || data.start >= start + count) &&
                            !(data.start == selStart && (spanPointFlags == Spanned.SPAN_INCLUSIVE_EXCLUSIVE || spanPointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE)) &&
                            !(data.end == selStart && (spanPointFlags == Spanned.SPAN_EXCLUSIVE_INCLUSIVE || spanPointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE)))
                        data.start += after - count;
                    if (data.end == selStart &&
                            (spanPointFlags == Spanned.SPAN_EXCLUSIVE_INCLUSIVE ||
                                    spanPointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE)) {
                        data.extendToCursor = true;
                    } else if (data.end > selStart) {
                        data.end = Math.max(data.end + after - count, data.start);
                    }
                    mBackedUpSpans.add(data);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mSettingText)
                    return;
                int selStart = getSelectionStart();
                for (SpanData span : mBackedUpSpans) {
                    if (span.extendToCursor)
                        span.end = selStart;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mSettingText)
                    return;
                mSettingText = true;
                for (SpanData span : mBackedUpSpans) {
                    span.start = Math.max(span.start, 0);
                    span.end = Math.min(span.end, s.length());
                    int spanPointFlags = span.flags & Spanned.SPAN_POINT_MARK_MASK;
                    if (span.start >= s.length() || span.end < 0 || span.start > span.end ||
                            (span.start == span.end && (spanPointFlags == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE || spanPointFlags == Spanned.SPAN_INCLUSIVE_EXCLUSIVE))) {
                        s.removeSpan(span.span);
                        continue;
                    }
                    s.setSpan(span.span, span.start, span.end, span.flags);
                }
                mBackedUpSpans.clear();
                mSettingText = false;
            }
        });
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused)
            mFormatBar.setEditText(this);
        else
            mFormatBar.setEditText(null);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mFormatBar != null)
            mFormatBar.updateFormattingAtCursor();
    }

    public void setFormatBar(TextFormatBar formatBar) {
        mFormatBar = formatBar;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        mSettingText = true;
        super.setText(text, type);
        mSettingText = false;
    }

    private static class SpanData {
        Object span;
        int start;
        int end;
        int flags;
        boolean extendToCursor;
    }

}
