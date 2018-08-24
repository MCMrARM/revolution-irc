package io.mrarm.irc.util;

import android.content.Context;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.URLSpan;
import android.text.style.UpdateAppearance;

public class TextSelectionHelper {

    public static long packTextRange(int start, int end) {
        return (((long) start) << 32) | end;
    }
    public static int unpackTextRangeStart(long range) {
        return (int) ((range >> 32) & 0xFFFFFF);
    }
    public static int unpackTextRangeEnd(long range) {
        return (int) (range & 0xFFFFFF);
    }


    public static long getWordAt(CharSequence sequence, int start, int end) {
        start = Math.min(Math.max(start, 0), sequence.length() - 1); // length - 1 because of end
        end = Math.min(Math.max(end, start), sequence.length() - 1);

        // Check for URL spans
        if (sequence instanceof Spanned) {
            Spanned spanned = (Spanned) sequence;
            Object[] spans = spanned.getSpans(start, end, URLSpan.class);
            if (spans.length >= 1) {
                return packTextRange(spanned.getSpanStart(spans[0]), spanned.getSpanEnd(spans[0]));
            }
        }

        while (start > 0 && Character.isLetterOrDigit(sequence.charAt(start - 1)))
            start--;
        while (end < sequence.length() && Character.isLetterOrDigit(sequence.charAt(end)))
            end++;
        return packTextRange(start, end);
    }

    public static void setSelection(Context context, Spannable sequence, int start, int end) {
        Object[] spans = sequence.getSpans(0, sequence.length(), SelectionSpan.class);
        SelectionSpan span;
        if (spans.length > 0)
            span = (SelectionSpan) spans[0];
        else
            span = new SelectionSpan(StyledAttributesHelper.getColor(context,
                    android.R.attr.textColorHighlight, 0));
        sequence.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                | Spannable.SPAN_COMPOSING);
    }

    public static void removeSelection(Spannable sequence) {
        Object[] spans = sequence.getSpans(0, sequence.length(), SelectionSpan.class);
        for (Object span : spans)
            sequence.removeSpan(span);
    }


    public static class SelectionSpan extends CharacterStyle implements UpdateAppearance {

        private int mColor;

        public SelectionSpan(int color) {
            mColor = color;
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.bgColor = mColor;
        }

    }

}
