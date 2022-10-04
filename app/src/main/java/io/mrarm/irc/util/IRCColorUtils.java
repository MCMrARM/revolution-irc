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

/*
 * NOTE: colours are referred to in several different ways:
 *   1. "Color": a 32-bit value comprising 4 8-bit values, A,R,G,B. This is the
 *      native Android "Color" type.
 *   2. "Color ID": an opaque value that we can retrieve a value from the
 *      runtime configuration. (Essentially it's a protobuf index number that's
 *      assigned when compiling the attr.xml resources file.)
 *   3. "Color Code": a 32-bit int that decomposes into 8 tag bits (bits 31-24)
 *      and a 24-bit value (bits 23-0). The interpretation of the 'value' then
 *      depends on the tag bits: which are checked in this order:
 *        → bit 27 means use the default color (and the value and the other
 *          tag bits are ignored when the color code is interpreted, but should
 *          be set to mIRC color index 99 with both bits 27 and 24 set)
 *        → bit 26 means the value is an ANSI color index between 0 and 255;
 *        → bit 25 means the value is an mIRC color index between 0 and 98;
 *        → bit 24 means the value is an RGB value (with A is fixed as 255);
 *        → if all bits 24-31 are 0, treat the value is a color-ID (as above);
 *        ← any other value is illegal and will raise an exception
 *  The "fg" and "bg" variables hold "tagged color codes", so that the logic
 *  that sets them doesn't have to do mapping between colour spaces.
 */

public class IRCColorUtils {

    public static final int COLOR_MEMBER_OWNER = R.styleable.IRCColors_colorMemberOwner;
    public static final int COLOR_MEMBER_ADMIN = R.styleable.IRCColors_colorMemberAdmin;
    public static final int COLOR_MEMBER_OP = R.styleable.IRCColors_colorMemberOp;
    public static final int COLOR_MEMBER_HALF_OP = R.styleable.IRCColors_colorMemberHalfOp;
    public static final int COLOR_MEMBER_VOICE = R.styleable.IRCColors_colorMemberVoice;
    public static final int COLOR_MEMBER_NORMAL = R.styleable.IRCColors_colorMemberNormal;

    private static final int COLOR_SPACE_24BIT  = 0x1000000;
    private static final int COLOR_SPACE_MIRC   = 0x2000000;
    private static final int COLOR_SPACE_ANSI   = 0x4000000;
    private static final int COLOR_SPACE_DEFAULT = 0x8000000;
    private static final int COLOR_DEFAULT      = COLOR_SPACE_DEFAULT|COLOR_SPACE_MIRC|99;

    private static class BaseColorException extends RuntimeException {};
    private static class InvalidColorCodeException extends BaseColorException {
        InvalidColorCodeException(int c) { super("Invalid color code "+Integer.toHexString(c)); }
    };
    private static class MissingThemeColorException extends BaseColorException {
        MissingThemeColorException(String m) { super(m); }
        MissingThemeColorException() { super(); }
    };

    private static int[] MIRC_COLOR_MAP_TEMPLATE = new int[] {
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

    private static int[] MIRC_COLOR_MAP = null;

    private static int[] ANSI_COLOR_MAP_TEMPLATE = new int[] {
            R.styleable.IRCColors_colorBlack,
            R.styleable.IRCColors_colorRed,     /* only in ANSI color-space */
            R.styleable.IRCColors_colorGreen,
            R.styleable.IRCColors_colorBrown,
            R.styleable.IRCColors_colorBlue,
            R.styleable.IRCColors_colorPurple,
            R.styleable.IRCColors_colorCyan,
            R.styleable.IRCColors_colorLightGray,

            R.styleable.IRCColors_colorGray,
            R.styleable.IRCColors_colorLightRed,
            R.styleable.IRCColors_colorLightGreen,
            R.styleable.IRCColors_colorYellow,
            R.styleable.IRCColors_colorLightBlue,
            R.styleable.IRCColors_colorPink,    /* light purple */
            R.styleable.IRCColors_colorLightCyan,
            R.styleable.IRCColors_colorWhite,

            /* 6×6×6 color cube */
            0xff000000, 0xff00005f, 0xff000087, 0xff0000af, 0xff0000d7, 0xff0000ff,
            0xff005f00, 0xff005f5f, 0xff005f87, 0xff005faf, 0xff005fd7, 0xff005fff,
            0xff008700, 0xff00875f, 0xff008787, 0xff0087af, 0xff0087d7, 0xff0087ff,
            0xff00af00, 0xff00af5f, 0xff00af87, 0xff00afaf, 0xff00afd7, 0xff00afff,
            0xff00d700, 0xff00d75f, 0xff00d787, 0xff00d7af, 0xff00d7d7, 0xff00d7ff,
            0xff00ff00, 0xff00ff5f, 0xff00ff87, 0xff00ffaf, 0xff00ffd7, 0xff00ffff,

            0xff5f0000, 0xff5f005f, 0xff5f0087, 0xff5f00af, 0xff5f00d7, 0xff5f00ff,
            0xff5f5f00, 0xff5f5f5f, 0xff5f5f87, 0xff5f5faf, 0xff5f5fd7, 0xff5f5fff,
            0xff5f8700, 0xff5f875f, 0xff5f8787, 0xff5f87af, 0xff5f87d7, 0xff5f87ff,
            0xff5faf00, 0xff5faf5f, 0xff5faf87, 0xff5fafaf, 0xff5fafd7, 0xff5fafff,
            0xff5fd700, 0xff5fd75f, 0xff5fd787, 0xff5fd7af, 0xff5fd7d7, 0xff5fd7ff,
            0xff5fff00, 0xff5fff5f, 0xff5fff87, 0xff5fffaf, 0xff5fffd7, 0xff5fffff,

            0xff870000, 0xff87005f, 0xff870087, 0xff8700af, 0xff8700d7, 0xff8700ff,
            0xff875f00, 0xff875f5f, 0xff875f87, 0xff875faf, 0xff875fd7, 0xff875fff,
            0xff878700, 0xff87875f, 0xff878787, 0xff8787af, 0xff8787d7, 0xff8787ff,
            0xff87af00, 0xff87af5f, 0xff87af87, 0xff87afaf, 0xff87afd7, 0xff87afff,
            0xff87d700, 0xff87d75f, 0xff87d787, 0xff87d7af, 0xff87d7d7, 0xff87d7ff,
            0xff87ff00, 0xff87ff5f, 0xff87ff87, 0xff87ffaf, 0xff87ffd7, 0xff87ffff,

            0xffaf0000, 0xffaf005f, 0xffaf0087, 0xffaf00af, 0xffaf00d7, 0xffaf00ff,
            0xffaf5f00, 0xffaf5f5f, 0xffaf5f87, 0xffaf5faf, 0xffaf5fd7, 0xffaf5fff,
            0xffaf8700, 0xffaf875f, 0xffaf8787, 0xffaf87af, 0xffaf87d7, 0xffaf87ff,
            0xffafaf00, 0xffafaf5f, 0xffafaf87, 0xffafafaf, 0xffafafd7, 0xffafafff,
            0xffafd700, 0xffafd75f, 0xffafd787, 0xffafd7af, 0xffafd7d7, 0xffafd7ff,
            0xffafff00, 0xffafff5f, 0xffafff87, 0xffafffaf, 0xffafffd7, 0xffafffff,

            0xffd70000, 0xffd7005f, 0xffd70087, 0xffd700af, 0xffd700d7, 0xffd700ff,
            0xffd75f00, 0xffd75f5f, 0xffd75f87, 0xffd75faf, 0xffd75fd7, 0xffd75fff,
            0xffd78700, 0xffd7875f, 0xffd78787, 0xffd787af, 0xffd787d7, 0xffd787ff,
            0xffd7af00, 0xffd7af5f, 0xffd7af87, 0xffd7afaf, 0xffd7afd7, 0xffd7afff,
            0xffd7d700, 0xffd7d75f, 0xffd7d787, 0xffd7d7af, 0xffd7d7d7, 0xffd7d7ff,
            0xffd7ff00, 0xffd7ff5f, 0xffd7ff87, 0xffd7ffaf, 0xffd7ffd7, 0xffd7ffff,

            0xffff0000, 0xffff005f, 0xffff0087, 0xffff00af, 0xffff00d7, 0xffff00ff,
            0xffff5f00, 0xffff5f5f, 0xffff5f87, 0xffff5faf, 0xffff5fd7, 0xffff5fff,
            0xffff8700, 0xffff875f, 0xffff8787, 0xffff87af, 0xffff87d7, 0xffff87ff,
            0xffffaf00, 0xffffaf5f, 0xffffaf87, 0xffffafaf, 0xffffafd7, 0xffffafff,
            0xffffd700, 0xffffd75f, 0xffffd787, 0xffffd7af, 0xffffd7d7, 0xffffd7ff,
            0xffffff00, 0xffffff5f, 0xffffff87, 0xffffffaf, 0xffffffd7, 0xffffffff,

            /* 24-point gray-scale */
            0xff080808, 0xff121212, 0xff1c1c1c, 0xff262626, 0xff303030, 0xff3a3a3a,
            0xff444444, 0xff4e4e4e, 0xff585858, 0xff626262, 0xff6c6c6c, 0xff767676,
            0xff808080, 0xff8a8a8a, 0xff949494, 0xff9e9e9e, 0xffa8a8a8, 0xffb2b2b2,
            0xffbcbcbc, 0xffc6c6c6, 0xffd0d0d0, 0xffdadada, 0xffe4e4e4, 0xffeeeeee
    };

    private static int[] ANSI_COLOR_MAP = null;

    private static int colorMapResId = R.style.AppTheme_IRCColors;

    private static int[] NICK_COLORS = new int[] { 3, 4, 7, 8, 9, 10, 11, 12, 13 };

    //private static int[] sColorValues = null;

    /* loadColorIdMap makes a fast cached version of the ColorId to Color
     * mappings in the Styling resources; */
    public static void loadColorIdMap(Resources.Theme theme, int resId) {
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
        // invalidate the color code maps
        MIRC_COLOR_MAP = null;
        ANSI_COLOR_MAP = null;
    }

    public static int getColorById(Context context, int colorId) {
        if (sColorValues == null)
            loadColorIdMap(context.getTheme(), R.style.AppTheme_IRCColors);
        return sColorValues[colorId];
    }

    private static int[] fillColorMapTemplate(Context context, int[] map_template) {
        int[] newmap = new int[map_template.length];
        int i;
        for (i=0;i<map_template.length;++i) {
            int v = map_template[i];
            try {
                if ((v & COLOR_SPACE_24BIT) != 0)
                    newmap[i] = v | 0xff000000;
                else if ((v & 0xff000000) == 0)
                    newmap[i] = getColorById(context, v);
                else
                    newmap[i] = 0x80ff7777; /* semi-transparent red as a warning */
            } catch (Exception e) {}
        }
        return newmap;
    }

    /* This should not be called for a "default" color */
    public static int getIrcColor(Context context, int colorCode) {
        int f = colorCode & ~0xffffff;
        int v = colorCode & 0xffffff;
        if ((f & COLOR_SPACE_24BIT) != 0)
            return v | 0xff000000; /* fully opaque */
        if ((f & COLOR_SPACE_DEFAULT) != 0)
            throw new InvalidColorCodeException(colorCode); /* never call this function with a "default" color */
        if ((f & COLOR_SPACE_MIRC) != 0) {
            if (MIRC_COLOR_MAP == null)
                MIRC_COLOR_MAP = fillColorMapTemplate(context, MIRC_COLOR_MAP_TEMPLATE);
            if (v >= MIRC_COLOR_MAP.length)
                throw new InvalidColorCodeException(colorCode);
            return MIRC_COLOR_MAP[v];
        }
        if ((f & COLOR_SPACE_ANSI) != 0) {
            if (ANSI_COLOR_MAP == null)
                ANSI_COLOR_MAP = fillColorMapTemplate(context, ANSI_COLOR_MAP_TEMPLATE);
            if (v >= ANSI_COLOR_MAP.length)
                throw new InvalidColorCodeException(colorCode);
            return ANSI_COLOR_MAP[v];
        }
        try {
            if (f==0)
                return getColorById(context, v);
        } catch(Exception e) {}
        throw new InvalidColorCodeException(colorCode);
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
        return getColorById(context, R.styleable.IRCColors_colorLightRed);
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
            int c = getIrcColor(context, i|COLOR_SPACE_MIRC);
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
        int fg = COLOR_DEFAULT,
            bg = COLOR_DEFAULT;
        boolean ansi_inverse = false,
                bold = false,
                italic = false,
                underline = false;
        SpannableStringBuilder spannable = builder.getSpannable();
        int len = string.length();
        for (int i = 0; i < len; ) {
            int ofg = fg,
                obg = bg;
            if (ansi_inverse) {
                ofg = bg;
                obg = fg;
            }
            boolean oBold = bold,
                    oItalic = italic,
                    oUnderline = underline;
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
                        fg = fg == 99 ? COLOR_DEFAULT
                                      : fg | COLOR_SPACE_MIRC;
                        if (i+1 < len && string.charAt(i) == ',' && isAsciiDigit(c = string.charAt(i+1))) {
                            i+=2;
                            bg = c - '0';
                            if (i < len && isAsciiDigit(c = string.charAt(i))) {
                                i++;
                                bg *= 10;
                                bg += c - '0';
                            }
                            bg = bg == 99 ? COLOR_DEFAULT
                                          : bg | COLOR_SPACE_MIRC;
                        }
                    } else
                        fg = bg = COLOR_DEFAULT;
                    break;
                }
                case '\n': { // ^J, newline, \n
                    spannable.append('\n');
                    fg = bg = COLOR_DEFAULT;
                    bold = italic = underline = false;
                    break;
                }
                case 0x0F: { // ^O, reset
                reset_all:
                    fg = bg = COLOR_DEFAULT;
                    bold = italic = underline = false;
                    break;
                }
                case 0x16: { // ^W, swap bg and fg
                    fg = pBg;
                    bg = pFg;
                    break;
                }
                case 0x1B: { // ^[, ESC, \e
                    int oi = i;
                    if (i+1 < len && string.charAt(i) == '[') {
                        i++;
                        /* we have seen CSI (the start of an ANSI sequence, "\e[") */
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
                                // (empty parameter list "\e[m" is correctly treated as "\e[0m")
                                params.add(x);
                                for (int j = 0 ; j<params.length; j++) {
                                    int p = params.get(j);
                                    switch (p) {
                                        case 0: {
                                            /* Reset everything */
                                            fg = bg = COLOR_DEFAULT;
                                            ansi_inverse =
                                            bold = italic = underline = false;
                                        }
                                        case  1: bold         = true;  break;
                                        case 22: bold         = false; break;
                                        case  3: italic       = true;  break;
                                        case 23: italic       = false; break;
                                        case  4: underline    = true;  break;
                                        case 24: underline    = false; break;
                                        case  7: ansi_inverse = true;  break;
                                        case 27: ansi_inverse = false; break;

                                        case 30: case 31: case 32: case 33: case 34: case 35: case 36: case 37: {
                                            fg = p-30 | COLOR_SPACE_ANSI;
                                            break;
                                        }
                                        case 38: {
                                            if (j+4 < params.length && params.get(j+1) == 2) {
                                                /* direct RGB specification  ESC [ 38 ; 2 ; R ; G ; B m */
                                                fg = params.get(j+2) << 16
                                                   | params.get(j+3) << 8
                                                   | params.get(j+4)
                                                   | COLOR_SPACE_24BIT;
                                                j += 4;
                                                break;
                                            }
                                            if (j+2 < params.length && params.get(j+1) == 5) {
                                                /* ANSI colour lookup  ESC [ 38 ; 5 ; LOOKUP m */
                                                fg = params.get[j+2] | COLOR_SPACE_ANSI;
                                                j+=2;
                                                break;
                                            }
                                            /* ANSI colour with unknown colourspace */
                                            j = params.length;  /* immediately stop */
                                            fg = COLOR_DEFAULT;
                                            break;
                                        }
                                        case 39: fg = COLOR_DEFAULT; break;    // foreground default
                                        case 90: case 91: case 92: case 93: case 94: case 95: case 96: case 97: {
                                            /* bright foreground */
                                            fg = p-90  | 8 | COLOR_SPACE_ANSI;
                                            break;
                                        }

                                        case 40: case 41: case 42: case 43: case 44: case 45: case 46: case 47: {
                                            /* standard background */
                                            bg = p-40 | COLOR_SPACE_ANSI;
                                            break;
                                        }
                                        case 48: {
                                            if (j+4 < params.length && params.get(j+1) == 2) {
                                                /* direct RGB specification  ESC [ 48 ; 2 ; R ; G ; B m */
                                                bg = params.get(j+2) << 16
                                                   | params.get(j+3) << 8
                                                   | params.get(j+4)
                                                   | COLOR_SPACE_24BIT;
                                                j += 4;
                                                break;
                                            }
                                            if (j+2 < params.length && params.get(j+1) == 5) {
                                                /* ANSI colour lookup  ESC [ 48 ; 5 ; LOOKUP m */
                                                bg = params.get[j+2] | COLOR_SPACE_ANSI;
                                                j+=2;
                                                break;
                                            }
                                            /* ANSI colour with unknown colourspace */
                                            j = params.length; /* immediately stop */
                                            bg = COLOR_DEFAULT;
                                            break;
                                        }
                                        case 49: bg = COLOR_DEFAULT; break;    // background default
                                        case 100: case 101: case 102: case 103: case 104: case 105: case 106: case 107: {
                                            /* bright background */
                                            bg = p-100 | 8 | COLOR_SPACE_ANSI;
                                            break;
                                        }

                                    }
                                }
                                /* We've seen a valid attribute-setting escape
                                 * sequence, so resume parsing from the the char
                                 * after the 'm' */
                                break;
                            }
                        }
                    }
                    /* We've encountered a broken or invalid escape sequence */
                    /*  (a) display a "printable ESC symbol" U+241B */
                    spannable.append((char) 0x241b);
                    /*  (b) resume parsing immediately following the ESC character. */
                    i = oi;
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
                    /* Only appending text; skip all the attribute checking logic below */
                    continue;
                }
            }

            if (ansi_inverse) {
                /* The mIRC 'invert' code simply swaps the current foreground
                 * and background colours.
                 * In contrast, ANSI inverse is a flag that is either set or
                 * reset; when it changes, the foreground & background colours
                 * do swap, but that's not all: attempting to set the
                 * foreground colour while ansi_inverse is 'on' will actually
                 * set the background colour, and vice versa. */
                int t = bg;
                        bg = fg;
                             fg = t;
            }

            /* Skip if no changes */
            if (   fg        == pFg
                && bg        == pBg
                && bold      == oBold
                && italic    == oItalic
                && underline == oUnderline )
                continue;

            if (fg == COLOR_DEFAULT && bg == COLOR_DEFAULT && ! bold && ! italic && ! underline) {
                /* Quickly reset everything to defaults: by closing all spans */
                builder.endSpans(Object.class);
                continue;
            }

            if (italic != oItalic) {
                if (italic)
                    builder.setSpan(new StyleSpan(Typeface.ITALIC));
                else
                    builder.endSpans(StyleSpan.class,
                            (StyleSpan s) -> s.getStyle() == Typeface.ITALIC);
            }
            if (bold != oBold) {
                if (bold)
                    builder.setSpan(new StyleSpan(Typeface.BOLD));
                else
                    builder.endSpans(StyleSpan.class,
                            (StyleSpan s) -> s.getStyle() == Typeface.BOLD);
            }
            if (underline != oUnderline) {
                if (underline)
                    builder.setSpan(new UnderlineSpan());
                else
                    builder.endSpans(UnderlineSpan.class);
            }
            /* Change fg and/or bg */
            if (bg != pBg) {
                /* Use this if spans have to nest properly */
                if (false && pFg != COLOR_DEFAULT) {
                    builder.endSpans(ForegroundColorSpan.class);
                    pFg = COLOR_DEFAULT;
                }
                /* end nesting enforcement */
                if (pBg != COLOR_DEFAULT)
                    builder.endSpans(BackgroundColorSpan.class);
                if (bg != COLOR_DEFAULT)
                    builder.setSpan(new BackgroundColorSpan(getIrcColor(context, bg)));
            }
            if (fg != pFg) {
                if (pFg != COLOR_DEFAULT)
                    builder.endSpans(ForegroundColorSpan.class);
                if (fg != COLOR_DEFAULT)
                    builder.setSpan(new ForegroundColorSpan(getIrcColor(context, fg)));
            }
        }
    }

    public static String convertSpannableToIRCString(Context context, Spannable spannable) {
        int n;
<<<<<<< HEAD
        int fg = COLOR_DEFAULT;
        int bg = COLOR_DEFAULT;
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
=======
        int pFg = COLOR_DEFAULT;
        int pBg = COLOR_DEFAULT;
        boolean pBold = false;
        boolean pItalic = false;
        boolean pUnderline = false;
        StringBuilder ret = new StringBuilder(spannable.length());
        for (int i = 0; i < spannable.length(); i = n) {
            n = spannable.nextSpanTransition(i, spannable.length(), Object.class);
            int fg = COLOR_DEFAULT;
            int bg = COLOR_DEFAULT;
            boolean bold = false;
            boolean italic = false;
            boolean underline = false;
>>>>>>> 566ae32... Convert "colour ID" tables to colour map templates
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
