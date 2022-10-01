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

    private static final int COLOR_SPACE_MIRC      = 0x1000000;
    private static final int COLOR_SPACE_ANSI      = 0x2000000;
    private static final int COLOR_SPACE_TRUECOLOR = 0x4000000;

    private static int[] MIRC_COLOR_IDS = new int[] {
            R.styleable.IRCColors_colorWhite,
            R.styleable.IRCColors_colorBlack,
            R.styleable.IRCColors_colorBlue,
            R.styleable.IRCColors_colorGreen,
            R.styleable.IRCColors_colorLightRed,
            R.styleable.IRCColors_colorBrown,
            R.styleable.IRCColors_colorPurple,
            R.styleable.IRCColors_colorOrange,      /* only in mIRC color-space */
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
        int c = MIRC_COLOR_IDS[colorId];
        if (c & COLOR_SPACE_TRUECOLOR)
            return c | 0xff000000; /* fully opaque */
        return getColorById(context, c);
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
        for (int i = 0; i < MIRC_COLOR_IDS.length; i++) {
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

    private static bool isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static void appendFormattedString(Context context, ColoredTextBuilder builder,
                                             String string) {
        int fg = 99,
            bg = 99;
        boolean bold = false,
                italic = false,
                underline = false;
        SpannableStringBuilder spannable = builder.getSpannable();
        int len = string.length();
        for (int i = 0; i < len; ) {
            int ofg = fg, obg = bg;
            boolean obold = bold, oitalic = italic, ounderline = underline;
            char c = string.charAt(i);
            i++;
            switch (c) {
                case 0x02: { // ^B, bold
                    bold = !bold;
                    break;
                }
                case 0x03: { // ^C, color
                    if (i < len && isAsciiDigit(c = string.charAt(i))) {
                        i++;
                        fg = c - '0';
                        if (i < len && isAsciiDigit(c = string.charAt(i))) {
                            i++;
                            fg *= 10;
                            fg += c - '0';
                        }
                        if (i+1 < len && string.charAt(i) == ',' && isAsciiDigit(c = string.charAt(i+1))) {
                            i+=2;
                            bg = c - '0';
                            if (i < len && isAsciiDigit(c = string.charAt(i))) {
                                i++;
                                bg *= 10;
                                bg += c - '0';
                            }
                        }
                    } else
                        fg = bg = 99;
                    break;
                }
                case '\n': { // ^J, newline
                    spannable.append('\n');
                    fg = bg = 99;
                    bold = italic = underline = false;
                    break;
                }
                case 0x0F: { // ^O, reset
                reset_all:
                    fg = bg = 99;
                    bold = italic = underline = false;
                    break;
                }
                case 0x16: { // ^W, swap bg and fg
                    fg = obg;
                    bg = ofg;
                    break;
                }
                case 0x1B: { // ^[, ESC
                    int oi = i;
                    if (i+1 < len && string.charAt(i) == '[') {
                        i++;
                        /* in C we could write the following loop as just:
                         * i += strspn(string+i, " !\"#$%&'()*+,-./0123456789:;<=>?"); */
                        usable = true;
                        while (i < len && (c = string.charAt(i++)) >= 0x20 && c <= 0x3f)
                            if (!isAsciiDigit(c) && c != ';')
                                usable = false;
                        if (c >= 0x40 && c <= 0x7f) {
                            /* We have a syntactically valid ANSI escape sequence */
                            if (c == 'm' && usable) {
                                /* Attribute setting ... */
                                ArrayList<int> params = new ArrayList<int>();
                                int x = 0;
                                for (int j=oi+1 ; j<i-1; ++j)
                                    if ((c = string.charAt(j)) == ';') {
                                        params.add(x);
                                        x = 0;
                                    } else {
                                        x *= 10;
                                        x += c - '0';
                                    }
                                params.add(x);
                                for (int j = 0 ; j<params.length; j++ ) {
                                    int p = params.get(j);
                                    switch (p) {
                                        case 0: {
                                            /* Reset everything */
                                            fg = bg = 99;
                                            bold = italic = underline = false;
                                        }
                                        case  1: bold = true;  break;
                                        case 22: bold = false; break;
                                        case  5: underline = true;  break;
                                        case 25: underline = false; break;
                                        case  6: italic = true;  break;
                                        case 26: italic = false; break;
                                        case 30: fg = 0; break;     // fg white
                                        case 31: fg = 4; break;     // fg red
                                        case 32: fg = 3; break;     // fg green
                                        case 33: fg = 8; break;     // fg yellow
                                        case 34: fg = 2; break;     // fg blue
                                        case 35: fg = 6; break;     // fg purple
                                        case 36: fg = 10; break;    // fg cyan
                                        case 37: fg = 1; break;     // fg white
                                        case 39: fg = 99; break;    // fg default
                                        case 40: bg = 0; break;     // bg white
                                        case 41: bg = 4; break;     // bg red
                                        case 42: bg = 3; break;     // bg green
                                        case 43: bg = 8; break;     // bg yellow
                                        case 44: bg = 2; break;     // bg blue
                                        case 45: bg = 6; break;     // bg purple
                                        case 46: bg = 10; break;    // bg cyan
                                        case 47: bg = 1; break;     // bg white
                                        case 49: bg = 99; break;    // bg default
                                    }
                                }
                            }
                            break;
                        }
                    }
                    i = oi;
                    spannable.append(0x241b);
                    continue;
                }
                case 0x1D: { // ^], italic
                    italic = !italic;
                    break;
                }
                case 0x1F: { // ^_, underline
                    underline = !underline;
                    break;
                }
                default: {
                    if (c < ' ')
                        c += 0x2400;
                    spannable.append(c);
                    continue;
                }
            }

            /* Skip if no changes */
            if (   fg        == ofg
                && bg        == obg
                && bold      == obold
                && italic    == oitalic
                && underline == ounderline )
                continue;

            if (fg == 99 && bg == 99 && ! bold && ! italic && ! underline) {
                /* Quickly reset everything to defaults: by closing all spans */
                builder.endSpans(Object.class);
                continue;
            }

            if (italic != oitalic) {
                if (italic)
                    builder.setSpan(new StyleSpan(Typeface.ITALIC));
                else
                    builder.endSpans(StyleSpan.class,
                            (StyleSpan s) -> s.getStyle() == Typeface.ITALIC);
            }
            if (bold != obold) {
                if (bold)
                    builder.setSpan(new StyleSpan(Typeface.BOLD));
                else
                    builder.endSpans(StyleSpan.class,
                            (StyleSpan s) -> s.getStyle() == Typeface.BOLD);
            }
            if (underline != ounderline) {
                if (underline)
                    builder.setSpan(new UnderlineSpan());
                else
                    builder.endSpans(UnderlineSpan.class);
            }
            /* Change fg and/or bg */
            if (bg != obg) {
                /* Use this if spans have to nest properly */
                if (false && ofg != 99) {
                    builder.endSpans(ForegroundColorSpan.class);
                    ofg = 99;
                }
                /* end nesting enforcement */
                if (obg != 99)
                    builder.endSpans(BackgroundColorSpan.class);
                if (bg != 99)
                    builder.setSpan(new BackgroundColorSpan(getIrcColor(context, bg)));
            }
            if (fg != ofg) {
                if (ofg != 99)
                    builder.endSpans(ForegroundColorSpan.class);
                if (fg != 99)
                    builder.setSpan(new ForegroundColorSpan(getIrcColor(context, fg)));
            }
        }
    }

    public static String convertSpannableToIRCString(Context context, Spannable spannable) {
        int n;
        int fg = 99;
        int bg = 99;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        StringBuilder ret = new StringBuilder(spannable.length());
        for (int i = 0; i < spannable.length(); i = n) {
            n = spannable.nextSpanTransition(i, spannable.length(), Object.class);
            int nFg = 99;
            int nBg = 99;
            boolean nBold = false;
            boolean nItalic = false;
            boolean nUnderline = false;
            for (Object span : spannable.getSpans(i, n, Object.class)) {
                int flags = spannable.getSpanFlags(span);
                if ((flags & Spannable.SPAN_COMPOSING) != 0)
                    continue;
                if (span instanceof ForegroundColorSpan) {
                    nFg = findNearestIRCColor(context, ((ForegroundColorSpan) span).getForegroundColor());
                } else if (span instanceof BackgroundColorSpan) {
                    nBg = findNearestIRCColor(context, ((BackgroundColorSpan) span).getBackgroundColor());
                } else if (span instanceof StyleSpan) {
                    int style = ((StyleSpan) span).getStyle();
                    if (style == Typeface.BOLD || style == Typeface.BOLD_ITALIC)
                        nBold = true;
                    if (style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC)
                        nItalic = true;
                } else if (span instanceof UnderlineSpan) {
                    nUnderline = true;
                }
            }
            if ((!nBold && bold) || (!nItalic && italic) || (!nUnderline && underline)) {
                ret.append((char) 0x0F);
                fg = -1;
                bg = -1;
                bold = false;
                italic = false;
                underline = false;
            }
            if (nBold && !bold)
                ret.append((char) 0x02);
            if (nItalic && !italic)
                ret.append((char) 0x1D);
            if (nUnderline && !underline)
                ret.append((char) 0x1F);
            if (nFg != fg || bg != bg) {
                ret.append((char) 0x03);
                ret.append(nFg);
                ret.append(',');
                ret.append(nBg);
            }

            fg = nFg;
            bg = nBg;
            bold = nBold;
            italic = nItalic;
            underline = nUnderline;
            ret.append(spannable, i, n);
        }
        return ret.toString();
    }

}
