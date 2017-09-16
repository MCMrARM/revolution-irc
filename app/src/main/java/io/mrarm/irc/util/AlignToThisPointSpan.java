package io.mrarm.irc.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.style.LeadingMarginSpan;
import android.widget.TextView;

public class AlignToThisPointSpan implements LeadingMarginSpan, NoCopySpan {

    private int mOffset = 0;
    private int mMargin = 0;

    public AlignToThisPointSpan(int referToOffset) {
        mOffset = referToOffset;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return first ? 0 : mMargin;
    }

    @Override
    public void drawLeadingMargin(Canvas canvas, Paint paint, int i, int i1, int i2, int i3, int i4,
                                  CharSequence charSequence, int i5, int i6, boolean b,
                                  Layout layout) {
        // stub
    }

    public static CharSequence apply(TextView textView, CharSequence text) {
        if (text == null || !(text instanceof Spannable))
            return text;
        Spannable s = (Spannable) text;
        for (AlignToThisPointSpan span : s.getSpans(0, text.length(),
                AlignToThisPointSpan.class)) {
            span.mMargin = (int) Layout.getDesiredWidth(text, 0, s.getSpanStart(span) +
                            span.mOffset, textView.getPaint());
        }
        return text;
    }

}
