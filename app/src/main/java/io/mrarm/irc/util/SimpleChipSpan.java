package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;

public class SimpleChipSpan extends ImageSpan {

    public SimpleChipSpan(Context context, String text, boolean transparent) {
        super(new SimpleChipDrawable(context, text, transparent));
        getDrawable().setBounds(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
    }

    public SimpleChipSpan(Context context, String text, Drawable drawable, boolean transparent) {
        super(new SimpleChipDrawable(context, text, drawable, transparent));
        getDrawable().setBounds(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        SimpleChipDrawable chipDrawable = ((SimpleChipDrawable) getDrawable());
        Paint myPaint = chipDrawable.getPaint();
        if (text instanceof Spanned) {
            Spanned spannable = (Spanned) text;
            int style = 0;
            int fgColor = -1;
            int fgColorS = -1;
            for (Object o : spannable.getSpans(start, end, Object.class)) {
                int spanStart = spannable.getSpanStart(o);
                if (spanStart > start)
                    continue;
                if (o instanceof ForegroundColorSpan) {
                    if (spanStart > fgColorS) {
                        fgColor = ((ForegroundColorSpan) o).getForegroundColor();
                        fgColorS = spanStart;
                    }
                } else if (o instanceof BackgroundColorSpan) {
                    int c = paint.getColor();
                    paint.setColor(((BackgroundColorSpan) o).getBackgroundColor());
                    canvas.drawRect(x, top, x + chipDrawable.getBounds().width(), bottom, paint);
                    paint.setColor(c);
                } else if (o instanceof StyleSpan) {
                    style |= ((StyleSpan) o).getStyle();
                }
            }
            if (myPaint != null) {
                if (fgColor != -1)
                    myPaint.setColor(fgColor);
                else
                    chipDrawable.setDefaultTextColor();
                myPaint.setTypeface(Typeface.create(Typeface.DEFAULT, style));
            }
        }
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);
    }
}
