package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import io.mrarm.irc.ColoredTextBuilder;
import io.mrarm.irc.R;

public class IRCColorUtils {

    public static int[] COLOR_IDS = new int[] {
            R.color.ircBlack,
            R.color.ircWhite,
            R.color.ircBlue,
            R.color.ircGreen,
            R.color.ircLightRed,
            R.color.ircBrown,
            R.color.ircPurple,
            R.color.ircOrange,
            R.color.ircYellow,
            R.color.ircLightGreen,
            R.color.ircCyan,
            R.color.ircLightCyan,
            R.color.ircLightBlue,
            R.color.ircPink,
            R.color.ircGray,
            R.color.ircLightGray
    };

    public static int[] NICK_COLORS = new int[] { 3, 4, 7, 8, 9, 10, 11, 12, 13 };

    public static int getColor(Context context, int colorId) {
        return context.getResources().getColor(COLOR_IDS[colorId]);
    }

    public static int getNickColor(Context context, String nick) {
        int sum = 0;
        for (int i = 0; i < nick.length(); i++)
            sum += nick.charAt(i);
        return getColor(context, NICK_COLORS[sum % NICK_COLORS.length]);
    }

    public static void appendFormattedString(Context context, ColoredTextBuilder builder,
                                             String string) {
        int fg = 99, bg = 99;
        SpannableStringBuilder spannable = builder.getSpannable();
        for (int i = 0; i < string.length(); i++) {
            switch (string.charAt(i)) {
                case 0x02: { // bold
                    i++;
                    builder.setSpan(new StyleSpan(Typeface.BOLD));
                    break;
                }
                case 0x1D: { // italic
                    i++;
                    builder.setSpan(new StyleSpan(Typeface.ITALIC));
                    break;
                }
                case 0x1F: { // underline
                    i++;
                    if (spannable.getSpans(spannable.length(), spannable.length(),
                            UnderlineSpan.class).length > 0)
                        continue;
                    builder.setSpan(new UnderlineSpan());
                    break;
                }
                case 0x0F: { // reset
                    i++;
                    builder.endSpans(Object.class);
                    break;
                }
                case '\n': { // newline
                    builder.endSpans(Object.class);
                    break;
                }
                case 0x03: { // color
                    fg = 0;
                    bg = 0;
                    i++;
                    for (int j = 0; j < 2 && i < string.length(); i++, j++) {
                        if (string.charAt(i) < '0' || string.charAt(i) > '9')
                            break;
                        fg = fg * 10 + string.charAt(i) - '0';
                    }
                    if (string.charAt(i++) != ',')
                        throw new RuntimeException("Invalid formatting");
                    for (int j = 0; j < 2 && i < string.length(); i++, j++) {
                        if (string.charAt(i) < '0' || string.charAt(i) > '9')
                            break;
                        bg = bg * 10 + string.charAt(i) - '0';
                    }
                    if (((bg < 0 || bg > COLOR_IDS.length) && bg != 99) ||
                            ((fg < 0 || fg > COLOR_IDS.length) && fg != 99))
                        throw new RuntimeException("Invalid formatting");

                    builder.endSpans(ForegroundColorSpan.class);
                    builder.endSpans(BackgroundColorSpan.class);
                    if (fg != 99)
                        builder.setSpan(new ForegroundColorSpan(getColor(context, fg)));
                    if (bg != 99)
                        builder.setSpan(new BackgroundColorSpan(getColor(context, bg)));
                    break;
                }
                case 0x16: { // swap bg and fg
                    i++;
                    int tmp = fg;
                    fg = bg;
                    bg = tmp;

                    builder.endSpans(ForegroundColorSpan.class);
                    builder.endSpans(BackgroundColorSpan.class);
                    if (fg != 99)
                        builder.setSpan(new ForegroundColorSpan(getColor(context, fg)));
                    if (bg != 99)
                        builder.setSpan(new BackgroundColorSpan(getColor(context, bg)));
                    break;
                }
            }
            spannable.append(string.charAt(i));
        }
    }

}
