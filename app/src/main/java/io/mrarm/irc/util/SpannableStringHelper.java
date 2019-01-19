package io.mrarm.irc.util;

import android.content.Context;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.google.gson.JsonObject;

public class SpannableStringHelper {

    public static final String SPAN_TYPE_FOREGROUND = "foreground";
    public static final String SPAN_TYPE_BACKGROUND = "background";
    public static final String SPAN_TYPE_STYLE = "style";

    public static CharSequence format(CharSequence seq, Object... args) {
        int argI = 0;
        SpannableStringBuilder builder = new SpannableStringBuilder(seq);
        for (int i = 0; i < builder.length() - 1; i++) {
            if (builder.charAt(i) == '%') {
                int c = builder.charAt(++i);
                CharSequence replacement = null;
                switch (c) {
                    case 's':
                        replacement = (CharSequence) args[argI++];
                        break;
                    case '%':
                        replacement = "%";
                        break;
                }
                if (replacement != null) {
                    builder.replace(i - 1, i + 1, replacement);
                    i += replacement.length() - 2;
                }
            }
        }
        return builder;
    }

    public static CharSequence getText(Context context, int resId, Object... args) {
        return format(context.getText(resId), args);
    }

    public static Object cloneSpan(Object span) {
        if (span instanceof ForegroundColorSpan)
            return new ForegroundColorSpan(((ForegroundColorSpan) span).getForegroundColor());
        if (span instanceof BackgroundColorSpan)
            return new BackgroundColorSpan(((BackgroundColorSpan) span).getBackgroundColor());
        if (span instanceof StyleSpan)
            return new StyleSpan(((StyleSpan) span).getStyle());
        return null;
    }

    public static boolean areSpansEqual(Object span, Object span2) {
        if (span instanceof ForegroundColorSpan && span2 instanceof ForegroundColorSpan)
            return ((ForegroundColorSpan) span).getForegroundColor() ==
                    ((ForegroundColorSpan) span2).getForegroundColor();
        if (span instanceof BackgroundColorSpan && span2 instanceof BackgroundColorSpan)
            return ((BackgroundColorSpan) span).getBackgroundColor() ==
                    ((BackgroundColorSpan) span2).getBackgroundColor();
        if (span instanceof StyleSpan && span2 instanceof StyleSpan)
            return ((StyleSpan) span).getStyle() == ((StyleSpan) span2).getStyle();
        return true;
    }

    public static JsonObject spanToJson(Object span) {
        JsonObject ret = new JsonObject();
        if (span instanceof ForegroundColorSpan) {
            ret.addProperty("type", SPAN_TYPE_FOREGROUND);
            ret.addProperty("color", ((ForegroundColorSpan) span).getForegroundColor());
        } else if (span instanceof BackgroundColorSpan) {
            ret.addProperty("type", SPAN_TYPE_BACKGROUND);
            ret.addProperty("color", ((BackgroundColorSpan) span).getBackgroundColor());
        } else if (span instanceof StyleSpan) {
            ret.addProperty("type", SPAN_TYPE_STYLE);
            ret.addProperty("style", ((StyleSpan) span).getStyle());
        } else {
            return null;
        }
        return ret;
    }

    public static Object spanFromJson(JsonObject obj) {
        String type = obj.get("type").getAsString();
        if (type.equals(SPAN_TYPE_FOREGROUND))
            return new ForegroundColorSpan(obj.get("color").getAsNumber().intValue());
        if (type.equals(SPAN_TYPE_BACKGROUND))
            return new BackgroundColorSpan(obj.get("color").getAsNumber().intValue());
        if (type.equals(SPAN_TYPE_STYLE))
            return new StyleSpan(obj.get("style").getAsNumber().intValue());
        return null;
    }

    public static void removeSpans(Spannable text, Class<?> type, int start, int end, Object mustEqual, boolean excludeNoCopySpans) {
        Object[] spans = text.getSpans(start, end, type);
        for (Object span : spans) {
            if (excludeNoCopySpans && span instanceof NoCopySpan)
                continue;
            if (mustEqual != null && !areSpansEqual(span, mustEqual))
                continue;
            int flags = text.getSpanFlags(span);
            if ((flags & Spanned.SPAN_COMPOSING) != 0)
                continue;
            int pointFlags = flags & Spanned.SPAN_POINT_MARK_MASK;
            int otherFlags = flags & ~Spanned.SPAN_POINT_MARK_MASK;
            int sStart = text.getSpanStart(span);
            int sEnd = text.getSpanEnd(span);
            text.removeSpan(span);
            if (sStart < start) {
                int newPointFlags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
                if (pointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE || pointFlags == Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    newPointFlags = Spanned.SPAN_INCLUSIVE_EXCLUSIVE;
                text.setSpan(span, sStart, start, newPointFlags | otherFlags);
                if (sEnd > end)
                    span = cloneSpan(span);
            }
            if (sEnd > end) {
                int newPointFlags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
                if (pointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE || pointFlags == Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
                    newPointFlags = Spanned.SPAN_EXCLUSIVE_INCLUSIVE;
                text.setSpan(span, end, sEnd, newPointFlags | otherFlags);
            }
        }
    }

    public static void removeSpans(Spannable text, Class<?> type, int start, int end, boolean excludeNoCopySpans) {
        removeSpans(text, type, start, end, null, excludeNoCopySpans);
    }

    public static void setAndMergeSpans(Spannable text, Object what, int start, int end, int flags) {
        Object[] spans = text.getSpans(Math.max(start - 1, 0), Math.min(end + 1, 0), what.getClass());
        for (Object span : spans) {
            if (!areSpansEqual(span, what))
                continue;
            int sFlags = text.getSpanFlags(span);
            if ((sFlags & Spanned.SPAN_COMPOSING) != 0)
                continue;

            int sStart = text.getSpanStart(span);
            int sEnd = text.getSpanEnd(span);
            if (sEnd < start || sStart > end)
                continue;
            text.removeSpan(span);
            if (sStart < start)
                start = sStart;
            if (sEnd > end)
                end = sEnd;
        }
        text.setSpan(what, start, end, flags);
    }

    public static boolean checkSpanInclude(int spanStart, int spanEnd, int spanFlags, int start, int end) {
        int pointFlags = spanFlags & Spanned.SPAN_POINT_MARK_MASK;
        boolean includesStart = pointFlags == Spanned.SPAN_INCLUSIVE_EXCLUSIVE ||
                pointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE;
        boolean includesEnd = pointFlags == Spanned.SPAN_EXCLUSIVE_INCLUSIVE ||
                pointFlags == Spanned.SPAN_INCLUSIVE_INCLUSIVE;
        return (start > spanStart || (spanStart == start && includesStart)) &&
                (end < spanEnd || (spanEnd == end && includesEnd));
    }

    public static boolean checkSpanInclude(Spannable s, Object span, int start, int end) {
        return checkSpanInclude(s.getSpanStart(span), s.getSpanEnd(span), s.getSpanFlags(span), start, end);
    }

    public static CharSequence copyCharSequence(CharSequence msg) {
        SpannableString str = new SpannableString(msg);
        for (Object o : str.getSpans(0, str.length(), NoCopySpan.class))
            str.removeSpan(o);
        return str;
    }

}
