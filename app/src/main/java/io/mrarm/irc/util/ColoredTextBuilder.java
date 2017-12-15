package io.mrarm.irc.util;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class ColoredTextBuilder {

    private SpannableStringBuilder builder = new SpannableStringBuilder();

    public void appendWithFlags(CharSequence text, int flags, Object... what) {
        builder.append(text);
        for (Object o : what)
            builder.setSpan(o, builder.length() - text.length(), builder.length(), flags);
    }

    public void append(CharSequence text, Object... what) {
        appendWithFlags(text, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, what);
    }

    public void setSpan(Object span) {
        builder.setSpan(span, builder.length(), builder.length(), Spanned.SPAN_MARK_POINT);
    }

    public <T> void endSpans(Class<T> kind, EndSpanChecker<T> lambda) {
        for (T span : builder.getSpans(builder.length(), builder.length(), kind)) {
            int type = builder.getSpanFlags(span) & Spannable.SPAN_POINT_MARK_MASK;
            if (type == Spannable.SPAN_MARK_POINT || type == Spannable.SPAN_POINT_POINT) {
                if (lambda != null && !lambda.shouldEndSpan(span))
                    continue;
                int start = builder.getSpanStart(span);
                builder.removeSpan(span);
                builder.setSpan(span, start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        | (type & ~Spannable.SPAN_POINT_MARK_MASK));
            }
        }
    }

    public void endSpans(Class<?> kind) {
        endSpans(kind, null);
    }

    public SpannableStringBuilder getSpannable() {
        return builder;
    }

    public interface EndSpanChecker<T> {

        boolean shouldEndSpan(T span);

    }

}
