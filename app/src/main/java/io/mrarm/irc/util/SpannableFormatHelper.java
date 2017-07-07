package io.mrarm.irc.util;

import android.content.Context;
import android.text.SpannableStringBuilder;

public class SpannableFormatHelper {

    public static CharSequence format(CharSequence seq, Object... args) {
        int argI = 0;
        SpannableStringBuilder builder = new SpannableStringBuilder(seq);
        for (int i = 0; i < builder.length() - 1; i++) {
            if (builder.charAt(i) == '%') {
                int c = builder.charAt(++i);
                CharSequence replacement = null;
                switch (c) {
                    case 's':
                        replacement = (CharSequence) args[argI++];
                        break;
                    case '%':
                        replacement = "%";
                        break;
                }
                if (replacement != null) {
                    builder.replace(i - 1, i + 1, replacement);
                    i += replacement.length() - 2;
                }
            }
        }
        return builder;
    }

    public static CharSequence getText(Context context, int resId, Object... args) {
        return format(context.getText(resId), args);
    }

}
