package io.mrarm.irc.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;

import java.util.Arrays;

import io.mrarm.irc.R;

public class IRCColorUtils {

    public static final int COLOR_MEMBER_OWNER = R.styleable.IRCColors_colorMemberOwner;
    public static final int COLOR_MEMBER_ADMIN = R.styleable.IRCColors_colorMemberAdmin;
    public static final int COLOR_MEMBER_OP = R.styleable.IRCColors_colorMemberOp;
    public static final int COLOR_MEMBER_HALF_OP = R.styleable.IRCColors_colorMemberHalfOp;
    public static final int COLOR_MEMBER_VOICE = R.styleable.IRCColors_colorMemberVoice;
    public static final int COLOR_MEMBER_NORMAL = R.styleable.IRCColors_colorMemberNormal;

    private static int[] COLOR_IDS = new int[] {
            R.styleable.IRCColors_colorWhite,
            R.styleable.IRCColors_colorBlack,
            R.styleable.IRCColors_colorBlue,
            R.styleable.IRCColors_colorGreen,
            R.styleable.IRCColors_colorLightRed,
            R.styleable.IRCColors_colorBrown,
            R.styleable.IRCColors_colorPurple,
            R.styleable.IRCColors_colorOrange,
            R.styleable.IRCColors_colorYellow,
            R.styleable.IRCColors_colorLightGreen,

            R.styleable.IRCColors_colorCyan,
            R.styleable.IRCColors_colorLightCyan,
            R.styleable.IRCColors_colorLightBlue,
            R.styleable.IRCColors_colorPink,
            R.styleable.IRCColors_colorGray,
            R.styleable.IRCColors_colorLightGray,
            0xff470000, 0xff472100, 0xff474700, 0xff324700,

            0xff004700, 0xff00472c, 0xff004747, 0xff002747, 0xff000047,
            0xff2e0047, 0xff470047, 0xff47002a, 0xff740000, 0xff743a00,

            0xff747400, 0xff517400, 0xff007400, 0xff007449, 0xff007474,
            0xff004074, 0xff000074, 0xff4b0074, 0xff740074, 0xff740045,

            0xffb50000, 0xffb56300, 0xffb5b500, 0xff7db500, 0xff00b500,
            0xff00b571, 0xff00b5b5, 0xff0063b5, 0xff0000b5, 0xff7500b5,

            0xffb500b5, 0xffb5006b, 0xffff0000, 0xffff8c00, 0xffffff00,
            0xffb2ff00, 0xff00ff00, 0xff00ffa0, 0xff00ffff, 0xff008cff,

            0xff0000ff, 0xffa500ff, 0xffff00ff, 0xffff0098, 0xffff5959,
            0xffffb459, 0xffffff71, 0xffcfff60, 0xff6fff6f, 0xff65ffc9,

            0xff6dffff, 0xff59b4ff, 0xff5959ff, 0xffc459ff, 0xffff66ff,
            0xffff59bc, 0xffff9c9c, 0xffffd39c, 0xffffff9c, 0xffe2ff9c,

            0xff9cff9c, 0xff9cffdb, 0xff9cffff, 0xff9cd3ff, 0xff9c9cff,
            0xffdc9cff, 0xffff9cff, 0xffff94d3, 0xff000000, 0xff131313,

            0xff282828, 0xff363636, 0xff4d4d4d, 0xff656565, 0xff818181,
            0xff9f9f9f, 0xffbcbcbc, 0xffe2e2e2, 0xffffffff
    };

    private static int[] NICK_COLORS = new int[] { 3, 4, 7, 8, 9, 10, 11, 12, 13 };

    private static int[] sColorValues = null;

    public static void loadColors(Resources.Theme theme, int resId) {
        TypedArray ta = theme.obtainStyledAttributes(resId, R.styleable.IRCColors);
        sColorValues = new int[R.styleable.IRCColors.length];
        for (int i = 0; i < sColorValues.length; i++) {
            try {
                int j = i;
                TypedValue tv;
                while ((tv = ta.peekValue(j)) != null && tv.type == TypedValue.TYPE_ATTRIBUTE)
                    j = Arrays.binarySearch(R.styleable.IRCColors, tv.data);
                sColorValues[i] = ta.getColor(j, Color.RED);
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                sColorValues[i] = Color.RED;
            }
        }
        ta.recycle();
    }

    private static void loadColors(Context context) {
        loadColors(context.getTheme(), R.style.AppTheme_IRCColors);
    }

    public static int getColorById(Context context, int colorId) {
        if (sColorValues == null)
            loadColors(context);
        return sColorValues[colorId];
    }

    public static int getIrcColor(Context context, int colorId) {
        return getColorById(context, COLOR_IDS[colorId]);
    }

    public static int getStatusTextColor(Context context) {
        return getColorById(context, R.styleable.IRCColors_colorStatusText);
    }

    public static int getTimestampTextColor(Context context) {
        return getColorById(context, R.styleable.IRCColors_colorTimestamp);
    }

    public static int getTopicTextColor(Context context) {
        return getColorById(context, R.styleable.IRCColors_colorTopic);
    }

    public static int getSenderFallbackColor(Context context) {
        return getColorById(context, R.styleable.IRCColors_colorSenderFallbackColor);
    }

    public static int getBanMaskColor(Context context) {
        return getIrcColor(context, 4 /* light red */);
    }

    public static int getNickColor(Context context, String nick) {
        int sum = 0;
        for (int i = 0; i < nick.length(); i++)
            sum += nick.charAt(i);
        return getIrcColor(context, NICK_COLORS[sum % NICK_COLORS.length]);
    }

    public static int findNearestIRCColor(Context context, int color) {
        int ret = -1;
        int retDiff = -1;
        for (int i = 0; i < COLOR_IDS.length; i++) {
            int c = getIrcColor(context, i);
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
        boolean bold = false, italic = false, underline = false;
        SpannableStringBuilder spannable = builder.getSpannable();
        for (int i = 0; i < string.length(); ) {
            switch (string.charAt(i)) {
                case 0x02: { // bold
                    i++;
                    bold = !bold;
                    if (bold)
                        builder.setSpan(new StyleSpan(Typeface.BOLD));
                    else
                        builder.endSpans(StyleSpan.class,
                                (StyleSpan s) -> s.getStyle() == Typeface.BOLD);
                    break;
                }
                case 0x1D: { // italic
                    i++;
                    italic = !italic;
                    if (italic)
                        builder.setSpan(new StyleSpan(Typeface.ITALIC));
                    else
                        builder.endSpans(StyleSpan.class,
                                (StyleSpan s) -> s.getStyle() == Typeface.ITALIC);
                    break;
                }
                case 0x1F: { // underline
                    i++;
                    underline = !underline;
                    if (underline)
                        builder.setSpan(new UnderlineSpan());
                    else
                        builder.endSpans(UnderlineSpan.class);
                    break;
                }
                case 0x0F: { // reset
                    i++;
                    builder.endSpans(Object.class);
                    fg = bg = 99;
                    bold = italic = underline = false;
                    break;
                }
                case '\n': { // newline
                    i++;
                    spannable.append('\n');
                    builder.endSpans(Object.class);
                    break;
                }
                case 0x03: { // color
                    fg = -1;
                    i++;
                    for (int j = 0; j < 2 && i < string.length(); i++, j++) {
                        if (string.charAt(i) < '0' || string.charAt(i) > '9')
                            break;
                        fg = Math.max(fg, 0) * 10 + string.charAt(i) - '0';
                    }
                    if (fg == -1) {
                        fg = bg = 99;
                        builder.endSpans(ForegroundColorSpan.class);
                        builder.endSpans(BackgroundColorSpan.class);
                        continue;
                    }
                    if (((fg < 0 || fg >= COLOR_IDS.length) && fg != 99))
                        throw new RuntimeException("Invalid formatting");

                    builder.endSpans(ForegroundColorSpan.class);
                    if (fg != 99)
                        builder.setSpan(new ForegroundColorSpan(getIrcColor(context, fg)));

                    if (string.charAt(i) != ',')
                        break;
                    i++;
                    bg = 0;
                    for (int j = 0; j < 2 && i < string.length(); i++, j++) {
                        if (string.charAt(i) < '0' || string.charAt(i) > '9')
                            break;
                        bg = bg * 10 + string.charAt(i) - '0';
                    }
                    if (((bg < 0 || bg >= COLOR_IDS.length) && bg != 99) ||
                            ((fg < 0 || fg >= COLOR_IDS.length) && fg != 99))
                        throw new RuntimeException("Invalid formatting");

                    builder.endSpans(BackgroundColorSpan.class);
                    if (bg != 99)
                        builder.setSpan(new BackgroundColorSpan(getIrcColor(context, bg)));
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
                        builder.setSpan(new ForegroundColorSpan(getIrcColor(context, fg)));
                    if (bg != 99)
                        builder.setSpan(new BackgroundColorSpan(getIrcColor(context, bg)));
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
