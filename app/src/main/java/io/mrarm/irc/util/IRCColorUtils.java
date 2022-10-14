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
 * NOTE: colors are referred to in several different ways:
 *   1. A native Android "Color": a 32-bit integer intepreted as 4 8-bit
 *      channels (Alpha, Red, Green, & Blue).
 *   2. "ColorId": an opaque key whose value we can retrieve from the runtime
 *      configuration. (Essentially it's a protobuf field index number that's
 *      assigned when compiling the attr.xml resources file.)
 *   3. "ColorCode" or "tagged Color Code": a 32-bit int that comprises an
 *      8-bit tag, and a 24-bit value. The tag determines the interpretation of
 *      the value.
 *
 *  COLOR_ARGB_FLAG is chosen to be the bottom bit of Alpha so that setting it
 *  within an ARGB value should be imperceptible, but just to be sure, treat
 *  fully transparent as a separate case.
 *
 *  COLOR_SPACE_ID is chosen to be 0 so that ColorId values are directly usable
 *  as ColorCode values.
 */

public class IRCColorUtils {

    public static final int COLOR_MEMBER_OWNER = R.styleable.IRCColors_colorMemberOwner;
    public static final int COLOR_MEMBER_ADMIN = R.styleable.IRCColors_colorMemberAdmin;
    public static final int COLOR_MEMBER_OP = R.styleable.IRCColors_colorMemberOp;
    public static final int COLOR_MEMBER_HALF_OP = R.styleable.IRCColors_colorMemberHalfOp;
    public static final int COLOR_MEMBER_VOICE = R.styleable.IRCColors_colorMemberVoice;
    public static final int COLOR_MEMBER_NORMAL = R.styleable.IRCColors_colorMemberNormal;

    private static final int COLOR_ARGB_FLAG  = 0x1000000;  // @ColorInt
    private static final int COLOR_SPACE_ID   = 0x0000000;  // @ColorInt
    private static final int COLOR_SPACE_MIRC = 0x2000000;  // @ColorInt
    private static final int COLOR_SPACE_ANSI = 0x4000000;  // @ColorInt
    private static final int COLOR_INVISIBLE  = 0x6000000;  // @ColorInt
    private static final int COLOR_DEFAULT    = 0x8000000;  // @ColorInt

    private static final Color COLOR_FAULT = Color.RED;     // @ColorInt

    private static class BaseColorException extends RuntimeException {
        BaseColorException(String m, int c) { super(m + " " + Integer.toHexString(c)); }
        BaseColorException(String m) { super(m); }
        BaseColorException(int c) { super(Integer.toHexString(c)); }
    };
    private static class InvalidColorCodeException extends BaseColorException {
        InvalidColorCodeException(int c) { super("Invalid color code", c); }
    };
    private static class InvalidColorIdException extends BaseColorException {
        InvalidColorIdException(int c) { super("Invalid color ID ", c); }
    };
    private static class MissingThemeColorException extends BaseColorException {
        MissingThemeColorException(String m) { super(m); }
        MissingThemeColorException() { super("Missing Theme or Element"); }
    };

    /*==========================================================================
     *
     *  Map "ColorId" values to (tagged) "ColorCode" values.
     *
     *  This will be a direct ARGB code (with the COLOR_ARGB_FLAG bit set),
     *  except for two cases:
     *   1. If the configuration does not contain a specification for a given
     *      ColorId, then COLOR_DEFAULT is substituted.
     *   2. If the specified ARGB value has a 0 alpha value (fully transparent)
     *      then COLOR_INVISIBLE is substituted; this is done because setting
     *      COLOR_ARGB_FLAG will prevent the alpha value being 0.
     */

    private static int[] colorIdMap = null;

    /* loadColorIdMap makes a fast cached version of the ColorId to ColorCode
     * mappings in the Styling resources; */
    public static void loadColorIdMap(Resources.Theme theme, int resId) {
        TypedArray ta = theme.obtainStyledAttributes(resId, R.styleable.IRCColors);
        colorIdMap = new int[R.styleable.IRCColors.length];
        for (int i = 0; i < colorIdMap.length; i++) {
            int c = COLOR_DEFAULT;
            try {
                int j = i;
                TypedValue tv;
                while ((tv = ta.peekValue(j)) != null && tv.type == TypedValue.TYPE_ATTRIBUTE)
                    j = Arrays.binarySearch(R.styleable.IRCColors, tv.data);
                c = ta.getColor(j, COLOR_FAULT);
            } catch (UnsupportedOperationException e)
                e.printStackTrace();

            if ( ( c & 0xff000000 ) == 0 )
                c = COLOR_INVISIBLE;
            else
                c |= COLOR_ARGB_FLAG;
            colorIdMap[i] = c;
        }
        ta.recycle();
        invalidateColorMaps();
    }

    public static int getColorById(Context context, int colorId) {
        if (colorIdMap == null)
            loadColorIdMap(context.getTheme(), R.style.AppTheme_IRCColors);
        if (colorId < 0 || colorId >= colorIdMap.length)
            throw new InvalidColorIdException(colorId);
        return colorIdMap[colorId];
    }

    /*==========================================================================
     *
     *  Map "ColorCode" values to native ARGB Color integers.
     *
     *  If the low-order bit of the tag is 1, then the entire code is taken
     *  as an ARGB value (with the tag used as the alpha value); otherwise the
     *  other bits of the tag select:
     *    * 00  a code ID, as above
     *    * 02  an mIRC color code (000000 to 000063)
     *    * 04  an ANSI color code (000000 to 0000ff)
     *    * 06  invisible (completely transparent)
     *    * 08  default (which differs for foreground and background)
     *  Other values will raise an InvalidColorCodeException, so that the entire
     *  IRC line will be rendered without any interpretation of control codes.
     *
     *  We maintain two color maps: mIRC colors, and ANSI colors.
     *
     *  The mIRC colors are divided into 16 configurable colors and 73 fixed colors.
     *  The ANSI colors are similarly divided into 16 configurable colors, a 6×6×6
     *  color-cube, and a 24-step gray-scale. In principle the color cube and grayscale
     *  are computable, based on a selected γ, but currently they're simply fixed, based
     *  on the color palette used by XTerm.
     */

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

    private static int[] MIRC_COLOR_MAP = null;
    private static int[] ANSI_COLOR_MAP = null;

    private static int[] fillColorMapTemplate(Context context, int[] map_template) {
        int[] newmap = new int[map_template.length];
        int i;
        for (i=0;i<map_template.length;++i) {
            int v = map_template[i];
            try
                if ((v & COLOR_ARGB_FLAG) != 0)
                    newmap[i] = v | 0xff000000;
                else if ((v & 0xff000000) == 0)
                    newmap[i] = getColorById(context, v);
                else
                    newmap[i] = 0x80ff7777; /* semi-transparent red as a warning */
            catch (Exception e) {}
        }
        return newmap;
    }

    private static void invalidateColorMaps() {
        MIRC_COLOR_MAP = null;
        ANSI_COLOR_MAP = null;
    }

    /* This should not be called for a "default" color */
    private static int codeToArgb(Context context, int colorCode) {
        int v = colorCode & 0xffffff;
        if ((colorCode & COLOR_ARGB_FLAG) != 0)
            return v | 0xff000000; /* fully opaque */
        switch (colorCode >> 24) {  /* >>24 ignores v */
            case COLOR_SPACE_MIRC >> 24:
                if (MIRC_COLOR_MAP == null)
                    MIRC_COLOR_MAP = fillColorMapTemplate(context, MIRC_COLOR_MAP_TEMPLATE);
                if (v >= MIRC_COLOR_MAP.length)
                    throw new InvalidColorCodeException(colorCode);
                return MIRC_COLOR_MAP[v];
            case COLOR_SPACE_ANSI >> 24:
                if (ANSI_COLOR_MAP == null)
                    ANSI_COLOR_MAP = fillColorMapTemplate(context, ANSI_COLOR_MAP_TEMPLATE);
                if (v >= ANSI_COLOR_MAP.length)
                    throw new InvalidColorCodeException(colorCode);
                return ANSI_COLOR_MAP[v];
            case COLOR_SPACE_ID >> 24:
                return getColorById(context, v);
            case COLOR_INVISIBLE >> 24:
                return 0x00000000; // ARGB with A set to 0
            case COLOR_DEFAULT >> 24: /* never call this function with a "default" color */
                throw new InvalidColorCodeException(colorCode);
            default:
                throw new InvalidColorCodeException(colorCode);
        }
    }

    public static int getIrcColor(Context context, int colorCode) {
        return codeToArgb(context, colorCode | COLOR_SPACE_MIRC);
    }

    private static int bestMatch(int [] colorMap, int color) {
        int r = Color.red(color),
            g = Color.green(color),
            b = Color.blue(color);
        int best = -1;
        int bestDiff = 0x300; // any number larger than 3×255
        for (int i = 0; i < colorMap.length; i++) {
            int c = colorMap[i];
            int diff = Math.abs(Color.red(c)   - r)
                     + Math.abs(Color.green(c) - g)
                     + Math.abs(Color.blue(c)  - b);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return best;
    }

    /* Reverse this process to get a code in the given space */
    private static int argbToCode(Context context, int color, int space) {
        switch (space >> 24) {
            case COLOR_SPACE_MIRC >> 24:
                if (MIRC_COLOR_MAP == null)
                    MIRC_COLOR_MAP = fillColorMapTemplate(context, MIRC_COLOR_MAP_TEMPLATE);
                return bestMatch(MIRC_COLOR_MAP, color) | COLOR_SPACE_MIRC;
            case COLOR_SPACE_ANSI >> 24:
                if (ANSI_COLOR_MAP == null)
                    ANSI_COLOR_MAP = fillColorMapTemplate(context, ANSI_COLOR_MAP_TEMPLATE);
                return bestMatch(ANSI_COLOR_MAP, color) | COLOR_SPACE_ANSI;
        }
        return color | COLOR_ARGB_FLAG;
    }

    /*================================================================================
     *
     *  Various "preferred colors"
     */

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

    private static int[] NICK_COLORS = new int[] {
         3 | COLOR_SPACE_MIRC,
         4 | COLOR_SPACE_MIRC,
         7 | COLOR_SPACE_MIRC,
         8 | COLOR_SPACE_MIRC,
         9 | COLOR_SPACE_MIRC,
        10 | COLOR_SPACE_MIRC,
        11 | COLOR_SPACE_MIRC,
        12 | COLOR_SPACE_MIRC,
        13 | COLOR_SPACE_MIRC,
    };

    public static int getNickColor(Context context, String nick) {
        int sum = 0;
        for (int i = 0; i < nick.length(); i++)
            sum += nick.charAt(i);
        return codeToArgb(context, NICK_COLORS[sum % NICK_COLORS.length]);
    }

    public static CharSequence getFormattedString(Context context, String string) {
        try {
            ColoredTextBuilder builder = new ColoredTextBuilder();
            appendFormattedString(context, builder, string);
            return builder.getSpannable();
        } catch (Exception e)
            return new SpannableString(string);
    }

    private static bool isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static class InvalidEscapeSequence extends RuntimeException {
        public boolean rewind() { return true; }
        InvalidEscapeSequence() { super(); rewind = true; }
    };
    private static class AbortEscapeSequence extends InvalidEscapeSequence {
        public boolean rewind() { return false; }
        AbortEscapeSequence() { super(); }
    };

    public static void appendFormattedString(Context context, ColoredTextBuilder builder,
                                             String string) {
        int fg = COLOR_DEFAULT,
            bg = COLOR_DEFAULT;
        boolean ansi_hidden = false,
                ansi_inverse = false,
                bold = false,
                italic = false,
                underline = false,
                strikeout = false;
        SpannableStringBuilder spannable = builder.getSpannable();
        int len = string.length();
        for (int i = 0; i < len; ) {
            /* Previous effective fg & bg, as modified by previous ansi_hidden
             * and ansi_inverse */
            int oeFg = fg,
                oeBg = bg;
            if (ansi_inverse) {
                oeFg = bg;
                oeBg = fg;
            }
            if (ansi_hidden)
                oeFg = oeBg;
            boolean oBold = bold,
                    oItalic = italic,
                    oStrikeout = strikeout,
                    oUnderline = underline;
            char c = string.charAt(i);
            i++;
            switch (c) {
                case 0x02: { // ^B, bold
                    bold = !bold;
                } break;

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
                } break;

                case 0x05: { // ^E, strikethrough
                    strikeout = !strikeout;
                } break;

                case '\n': { // ^J, newline, \n
                    spannable.append('\n');
                    fg = bg = COLOR_DEFAULT;
                    bold = italic = underline = false;
                } break;

                case 0x0F: { // ^O, reset
                    fg = bg = COLOR_DEFAULT;
                    ansi_hidden = ansi_inverse =
                    bold = italic = strikeout = underline = false;
                } break;

                case 0x16: { // ^W, swap bg and fg
                    int t = fg;
                            fg = bg;
                                 bg = t;
                    break;
                }
                case 0x1B: { // ^[, ESC, \e
                    int oi = i;
                    try {
                        if (i+1 >= len || string.charAt(i) != '[')
                            /* ESC not followed by [ */
                            throw new InvalidEscapeSequence();
                        /* We've seen an ANSI control sequence introducer (CSI): \e[ */
                        i++;
                        /* in C we could write the following loop as just:
                         * i += strspn(string+i, " !\"#$%&'()*+,-./0123456789:;<=>?"); */
                        boolean usable = true;
                        while (i < len && (c = string.charAt(i++)) >= 0x20 && c <= 0x3f)
                            if (!isAsciiDigit(c) && c != ';' && c != ':')
                                usable = false;
                        if (c < 0x40 || c > 0x7f) {
                            /* We've seen a sequence starting with CSI but
                             * stopped at an invalid byte, so treat it as any
                             * other invalid sequence. */
                            throw new InvalidEscapeSequence();
                        }
                        /* We've seen a syntactically valid ANSI escape
                         * sequence */
                        if (c != 'm' || !usable) {
                            /* We've seen a valid but unknown sequence
                             * comprising CSI [\x20-\x3f]* [\x4-\x7f]
                             * which we can simply ignore. */
                            continue;
                        }

                        /* We've seen a valid Attribute Setting sequence,
                         * starting with CSI and ending with 'm' with only
                         * digits, colons and semicolons in between.
                         *
                         * Interpret this as a series of parameters are
                         * separated by semicolons, which in turn may be
                         * subdivided into subparameters separated by colons.
                         *
                         * Because colons and semicolons are sometimes treated
                         * interchangeably, we treat parameters and
                         * sub-parameters as a single flat array, but remember
                         * whether or not each one was preceded by a colon. */
                        int estSize = (i-oi) / 3 + 3;
                        ArrayList<int> params = new ArrayList<>(estSize);
                        ArrayList<boolean> colons = new ArrayList<>(estSize);
                        {
                        colons.add(false);    /* first param is not preceded by a colon */
                        int x = 0;
                        for (int j=oi+1 ; j<i-1; ++j)
                            if ((c = string.charAt(j)) == ';' || c == ':') {
                                params.add(x);
                                colons.add(a == ':');
                                x = 0;
                            } else {
                                x *= 10;
                                x += c - '0';
                            }
                        // (empty parameter list "\e[m" is correctly treated as "\e[0m")
                        params.add(x);
                        }
                        for (int j = 0 ; j<params.length(); j++) {
                            if (colons.get(j))
                                throw new InvalidEscapeSequence();
                            int p = params.get(j);
                            switch (p) {
                                case 0: {
                                    /* Reset everything */
                                    fg = bg = COLOR_DEFAULT;
                                    ansi_hidden = ansi_inverse =
                                    bold = italic = strikeout = underline = false;
                                }
                                case  1: bold         = true;  break;
                                /* 2 - dim not implemented */
                                case 22: bold         = false; break;
                                case  3: italic       = true;  break;
                                case 23: italic       = false; break;
                                case  4: underline    = true;  break;
                                case 24: underline    = false; break;
                                /* 5/25 - blink not implemented */
                                /* 6/26 - bold not implemented */
                                case  7: ansi_inverse = true;  break;
                                case 27: ansi_inverse = false; break;
                                case  8: ansi_hidden  = true;  break;
                                case 28: ansi_hidden  = false; break;
                                case  9: strikeout    = true;  break;
                                case 29: strikeout    = false; break;

                                case 30: case 31: case 32: case 33: case 34: case 35: case 36: case 37: {
                                    fg = p-30 | COLOR_SPACE_ANSI;
                                    break;
                                }
                                case 90: case 91: case 92: case 93: case 94: case 95: case 96: case 97: {
                                    /* bright foreground */
                                    fg = p-90  | 8 | COLOR_SPACE_ANSI;
                                } break;

                                case 39: fg = COLOR_DEFAULT; break;    // foreground default

                                case 40: case 41: case 42: case 43: case 44: case 45: case 46: case 47: {
                                    /* standard background */
                                    bg = p-40 | COLOR_SPACE_ANSI;
                                } break;

                                case 100: case 101: case 102: case 103: case 104: case 105: case 106: case 107: {
                                    /* bright background */
                                    bg = p-100 | 8 | COLOR_SPACE_ANSI;
                                } break;

                                case 49: bg = COLOR_DEFAULT; break;    // background default

                                case 38: case 48: {
                                    ++j;
                                    if (j >= params.length) throw new AbortEscapeSequence();
                                    int newColor = COLOR_DEFAULT;
                                    boolean colon = colons.get(j);
                                    switch (params.get(j)) {
                                        case 2: {
                                            j += colon ? 4 : 3;
                                            if (j >= params.length) throw new AbortEscapeSequence();
                                            /* direct RGB specification
                                             *  ESC [ ... 38/48 ; 2 ; R ; G ; B ... m OR
                                             *  ESC [ ... 38/48 : 2 : : R : G : B ... m */
                                            newColor = Color.rgb(params.get(j-2),
                                                                 params.get(j-1),
                                                                 params.get(j))
                                                       | COLOR_ARGB_FLAG;
                                        } break;

                                        case 5: {
                                            j++;
                                            if (j >= params.length) throw new AbortEscapeSequence();
                                            if (j < params.length) {
                                                /* ANSI color lookup
                                                 *  ESC [ ... 38/48 ; 5 ; LOOKUP ... m OR
                                                 *  ESC [ ... 38/48 : 5 : LOOKUP ... m
                                                 */
                                                newColor = params.get(j) | COLOR_SPACE_ANSI;
                                            }
                                        } break;

                                        default: {
                                            if (!colon)
                                                throw new AbortEscapeSequence();
                                            /* resume once past all colon-separated elements */
                                            for (;j+1 < params.length && colons.get(j+1); ++j) {}
                                        } break;
                                    }
                                    /* ANSI color with unknown colorspace */
                                    switch (p) {
                                        case 38: fg = newColor; break;
                                        case 48: bg = newColor; break;
                                    }
                                } break;

                            }
                            /* continue processing next attribute within the
                             * same CSI ... 'm' sequence */
                        }
                    }
                    catch (InvalidEscapeSequence e)
                        if (e.rewind()) {
                            /* We've encountered a broken or invalid escape
                             * sequence, so
                             *  (a) display a "printable ESC symbol" U+241B, and */
                            spannable.append('␛');
                            /*  (b) resume parsing immediately following the ESC character. */
                            i = oi;
                            continue;
                        }
                    /* We've seen a valid attribute-setting escape sequence, so
                     * resume parsing from the the char after the 'm' */
                    break;
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

            /* The mIRC 'invert' code simply swaps the current foreground
             * and background colors.
             * In contrast, ANSI inverse is a flag that is either set or
             * reset; when it changes, the foreground & background colors
             * do swap, but that's not all: attempting to set the
             * foreground color while ansi_inverse is 'on' will actually
             * set the background color, and vice versa.
             *
             * Compute current effective fg & bg as modified by the ansi_hidden
             * and ansi_inverse flags */
            int eFg = fg,
                eBg = bg;
            if (ansi_inverse) {
                int t = eBg;
                        eBg = eFg;
                              eFg = t;
            }
            if (ansi_hidden)
                eFg = eBg;

            /* Skip if no changes */
            if (   eFg       == oeFg
                && eBg       == oeBg
                && bold      == oBold
                && italic    == oItalic
                && strikeout == oStrikeout
                && underline == oUnderline )
                continue;

            if (eFg == COLOR_DEFAULT && eBg == COLOR_DEFAULT
             && ! bold && ! italic && ! underline) {
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
            if (eBg != oeBg) {
                /* Use this if spans have to nest properly */
                if (false && oeFg != COLOR_DEFAULT) {
                    builder.endSpans(ForegroundColorSpan.class);
                    oeFg = COLOR_DEFAULT;
                }
                /* end nesting enforcement */
                if (oeBg != COLOR_DEFAULT)
                    builder.endSpans(BackgroundColorSpan.class);
                if (eBg != COLOR_DEFAULT)
                    builder.setSpan(new BackgroundColorSpan(codeToArgb(context, eBg)));
            }
            if (eFg != oeFg) {
                if (oeFg != COLOR_DEFAULT)
                    builder.endSpans(ForegroundColorSpan.class);
                if (eFg != COLOR_DEFAULT)
                    builder.setSpan(new ForegroundColorSpan(codeToArgb(context, eFg)));
            }
        }
    }

    /* Currently we only output mIRC codes.
     *  TODO: 1) add option to send ANSI attribute codes & basic color codes;
     *        2) add option to send ANSI 24-bit color codes.
     */
    public static String convertSpannableToIRCString(Context context, Spannable spannable) {
        int fg = COLOR_DEFAULT,
            bg = COLOR_DEFAULT;
        boolean bold = false,
                italic = false,
                underline = false,
                strikeout = false;
        StringBuilder ret = new StringBuilder(spannable.length());
        int nextSpan;
        for (int i = 0; i < spannable.length(); i = nextSpan) {
            nextSpan = spannable.nextSpanTransition(i, spannable.length(), Object.class);
            /* These intentionally do NOT inherit from the "current" settings */
            int nFg = COLOR_DEFAULT,
                nBg = COLOR_DEFAULT;
            boolean nBold = false,
                    nItalic = false,
                    nUnderline = false,
                    nStrikeout = false;
            for (Object span : spannable.getSpans(i, nextSpan, Object.class)) {
                int flags = spannable.getSpanFlags(span);
                if ((flags & Spannable.SPAN_COMPOSING) != 0)
                    continue;
                if (span instanceof ForegroundColorSpan)
                    nFg = argbToCode(context, ((ForegroundColorSpan) span).getForegroundColor(), COLOR_SPACE_MIRC);
                else if (span instanceof BackgroundColorSpan)
                    nBg = argbToCode(context, ((BackgroundColorSpan) span).getBackgroundColor(), COLOR_SPACE_MIRC);
                else if (span instanceof StyleSpan) {
                    int style = ((StyleSpan) span).getStyle();
                    /* make use of sensible bitwise numbering */
                    nBold   = (style & Typeface.BOLD  ) != 0;
                    nItalic = (style & Typeface.ITALIC) != 0;
                } else if (span instanceof UnderlineSpan)
                    nUnderline = true;
                else if (span instanceof StrikethroughSpan)
                    nStrikeout = true;
            }
            if ( !( nBold || nItalic || nUnderline || nFg != COLOR_DEFAULT || nBg != COLOR_DEFAULT )
               && (  bold ||  italic ||  underline ||  fg != COLOR_DEFAULT ||  bg != COLOR_DEFAULT )) {
                ret.append((char) 0x0F);
                fg = COLOR_DEFAULT;
                bg = COLOR_DEFAULT;
                bold = false;
                italic = false;
                underline = false;
            }
            if (nFg != fg || nBg != bg) {
                /* Check whether the char following this sequence could be
                 * miscontrued as part of the sequence itself.
                 *
                 * In particular, digits and commas.
                 * For the sake of other (possibly broken) IRC clients, and
                 * even though it's not technically necessary, treat a
                 * following comma as a "digit", so that we generate a full
                 * color code sequence, */
                boolean followedByCChar = (isAsciiDigit(spannable.chatAt(i))
                                           || ',' == spannable.chatAt(i))
                                         && nBold      == bold
                                         && nItalic    == italic
                                         && nUnderline == underline
                                         && nStrikeout == strikeout;
                int xBg = nBg == COLOR_DEFAULT
                       || nBg == COLOR_INVISIBLE ? 99
                                                 : nBg % 100;
                int xFg = nFg == COLOR_DEFAULT   ? 99 :
                          nBg == COLOR_INVISIBLE ? xBg :
                                                   nFg % 100;
                ret.append((char) 0x03);
                /* omit «99,99» color pair if we're sure there's a non-digit following */
                if (xBg != 99 || xBg != 99 || followedByCChar) {
                    ret.append(xFg);
                    ret.append(',');
                    /* Pad to 2 digits unless we're sure there's a non-digit following */
                    if (xBg < 10 && followedByCChar)
                        ret.append('0');
                    ret.append(xBg);
                }
                fg = nFg;
                bg = nBg;
            }
            if (nBold != bold) {
                ret.append((char) 0x02);
                bold = !bold;
            }
            if (nItalic != italic) {
                ret.append((char) 0x1D);
                italic = !italic;
            }
            if (nUnderline != underline) {
                ret.append((char) 0x1F);
                underline = !underline;
            }
            if (nStrikeout != strikeout) {
                ret.append((char) 0x05);
                strikeout = !strikeout;
            }

            ret.append(spannable, i, nextSpan);
        }
        return ret.toString();
    }

}
