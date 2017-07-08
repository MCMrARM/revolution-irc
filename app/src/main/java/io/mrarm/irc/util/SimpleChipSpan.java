package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;

public class SimpleChipSpan extends ImageSpan {

    public SimpleChipSpan(Context context, String text, boolean transparent) {
        super(new SimpleChipDrawable(context, text, transparent));
        getDrawable().setBounds(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        SimpleChipDrawable chipDrawable = ((SimpleChipDrawable) getDrawable());
        Paint myPaint = chipDrawable.getPaint();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            int style = 0;
            chipDrawable.setDefaultTextColor();
            for (Object o : spannable.getSpans(start, end, Object.class)) {
                if (!SpannableStringHelper.checkSpanInclude(spannable, o, start, start))
                    continue;
                if (o instanceof ForegroundColorSpan) {
                    myPaint.setColor(((ForegroundColorSpan) o).getForegroundColor());
                } else if (o instanceof BackgroundColorSpan) {
                    int c = paint.getColor();
                    paint.setColor(((BackgroundColorSpan) o).getBackgroundColor());
                    canvas.drawRect(x, top, x + chipDrawable.getBounds().width(), bottom, paint);
                    paint.setColor(c);
                } else if (o instanceof StyleSpan) {
                    style |= ((StyleSpan) o).getStyle();
                }
            }
            myPaint.setTypeface(Typeface.create(Typeface.DEFAULT, style));
        }
        super.draw(canvas, text, start, end, x, top, y, bottom, paint);
    }
}
