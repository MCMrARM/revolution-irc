package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

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

    public static int getStatusTextColor(Context context) {
        return context.getResources().getColor(R.color.messageStatusText);
    }

    public static int getTimestampTextColor(Context context) {
        return context.getResources().getColor(R.color.messageTimestamp);
    }

    public static int getTopicTextColor(Context context) {
        return context.getResources().getColor(R.color.messageTopic);
    }

    public static int getBanMaskColor(Context context) {
        return context.getResources().getColor(R.color.ircLightRed);
    }

    public static int getNickColor(Context context, String nick) {
        int sum = 0;
        for (int i = 0; i < nick.length(); i++)
            sum += nick.charAt(i);
        return getColor(context, NICK_COLORS[sum % NICK_COLORS.length]);
    }

    public static int findNearestIRCColor(Context context, int color) {
        int ret = -1;
        int retDiff = -1;
        for (int i = 0; i < COLOR_IDS.length; i++) {
            int c = getColor(context, i);
            int diff = Math.abs(Color.red(c) - Color.red(color)) + Math.abs(Color.green(c) - Color.green(color)) + Math.abs(Color.blue(c) - Color.blue(color));
            if (diff < retDiff || retDiff == -1) {
                retDiff = diff;
                ret = i;
            }
        }
        return ret;
    }

    public static CharSequence getFormattedString(Context context, String string) {
        try {
            ColoredTextBuilder builder = new ColoredTextBuilder();
            appendFormattedString(context, builder, string);
            return builder.getSpannable();
        } catch (Exception e) {
            return new SpannableString(string);
        }
    }

    public static void appendFormattedString(Context context, ColoredTextBuilder builder,
                                             String string) {
        int fg = 99, bg = 99;
        SpannableStringBuilder spannable = builder.getSpannable();
        for (int i = 0; i < string.length(); ) {
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
                    i++;
                    spannable.append('\n');
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
                default: {
                    spannable.append(string.charAt(i));
                    i++;
                }
            }
        }
    }

    public static String convertSpannableToIRCString(Context context, Spannable spannable) {
        int n;
        int pFg = 99;
        int pBg = 99;
        boolean pBold = false;
        boolean pItalic = false;
        boolean pUnderline = false;
        StringBuilder ret = new StringBuilder(spannable.length());
        for (int i = 0; i < spannable.length(); i = n) {
            n = spannable.nextSpanTransition(i, spannable.length(), Object.class);
            int fg = 99;
            int bg = 99;
            boolean bold = false;
            boolean italic = false;
            boolean underline = false;
            for (Object span : spannable.getSpans(i, n, Object.class)) {
                int flags = spannable.getSpanFlags(span);
                if ((flags & Spannable.SPAN_COMPOSING) != 0)
                    continue;
                if (span instanceof ForegroundColorSpan) {
                    fg = findNearestIRCColor(context, ((ForegroundColorSpan) span).getForegroundColor());
                } else if (span instanceof BackgroundColorSpan) {
                    bg = findNearestIRCColor(context, ((BackgroundColorSpan) span).getBackgroundColor());
                } else if (span instanceof StyleSpan) {
                    int style = ((StyleSpan) span).getStyle();
                    if (style == Typeface.BOLD || style == Typeface.BOLD_ITALIC)
                        bold = true;
                    if (style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC)
                        italic = true;
                } else if (span instanceof UnderlineSpan) {
                    underline = true;
                }
            }
            if ((!bold && pBold) || (!italic && pItalic) || (!underline && pUnderline)) {
                ret.append((char) 0x0F);
                pFg = -1;
                pBg = -1;
                pBold = false;
                pItalic = false;
                pUnderline = false;
            }
            if (bold && !pBold)
                ret.append((char) 0x02);
            if (italic && !pItalic)
                ret.append((char) 0x1D);
            if (underline && !pUnderline)
                ret.append((char) 0x1F);
            if (fg != pFg || bg != pBg) {
                ret.append((char) 0x03);
                ret.append(fg);
                ret.append(',');
                ret.append(bg);
            }

            pFg = fg;
            pBg = bg;
            pBold = bold;
            pItalic = italic;
            pUnderline = underline;
            ret.append(spannable, i, n);
        }
        return ret.toString();
    }

}
