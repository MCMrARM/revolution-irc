package io.mrarm.irc.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.style.ReplacementSpan;

import java.util.regex.Pattern;

public class FixedWidthTimestampSpan extends ReplacementSpan {

    private static final String MEASURE_NUMBER_CHARS = "1234567890";
    private float[] mNumberWidths = new float[MEASURE_NUMBER_CHARS.length()];

    private static final Pattern sMatchNumbersRegex = Pattern.compile("[0-9]");

    private int mPreOffset;

    public FixedWidthTimestampSpan(int preOffset) {
        mPreOffset = preOffset;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
                       @Nullable Paint.FontMetricsInt fm) {
        paint.getTextWidths(MEASURE_NUMBER_CHARS, mNumberWidths);
        float mw = 0.f;
        char ld = '0';
        for (int i = MEASURE_NUMBER_CHARS.length() - 1; i >= 0; --i) {
            if (mNumberWidths[i] > mw) {
                mNumberWidths[i] = mw;
                ld = MEASURE_NUMBER_CHARS.charAt(i);
            }
        }
        CharSequence s = text.subSequence(start - mPreOffset, start);
        String rs = sMatchNumbersRegex.matcher(s).replaceAll(String.valueOf(ld));
        return (int) (paint.measureText(rs) - paint.measureText(s, 0, s.length()));
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
    }

}
