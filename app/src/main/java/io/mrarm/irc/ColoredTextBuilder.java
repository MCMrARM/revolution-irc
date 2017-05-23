package io.mrarm.irc;

import android.text.Spannable;
import android.text.SpannableStringBuilder;

public class ColoredTextBuilder {

    private SpannableStringBuilder builder = new SpannableStringBuilder();

    public void appendWithFlags(String text, int flags, Object... what) {
        builder.append(text);
        for (Object o : what)
            builder.setSpan(o, builder.length() - text.length(), builder.length(), flags);
    }

    public void append(String text, Object... what) {
        appendWithFlags(text, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, what);
    }

    public SpannableStringBuilder getSpannable() {
        return builder;
    }

}
