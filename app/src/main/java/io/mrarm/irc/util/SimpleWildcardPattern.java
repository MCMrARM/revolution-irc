package io.mrarm.irc.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SimpleWildcardPattern {

    private static String globToRegexp(String str) {
        if (str == null)
            return null;
        StringBuilder ret = new StringBuilder();
        ret.append('^');
        int pi = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '*' || c == '?') {
                if (i > pi)
                    ret.append(Pattern.quote(str.substring(pi, i)));
                if (c == '*')
                    ret.append(".*");
                else
                    ret.append(".");
                pi = i + 1;
            }
        }
        if (str.length() > pi)
            ret.append(Pattern.quote(str.substring(pi)));
        ret.append('$');
        return ret.toString();
    }

    public static Pattern pattCompile(String str) throws PatternSyntaxException {
        int l1 = str.length() - 1;
        int j = (l1 >= 1)? 1 : 0;
        boolean isRe = false;

        if (j == 1) {
            char c0 = str.charAt(0);
            char c1 = str.charAt(l1);

            if ( (c0 == '/') && (c1 == '/') ) {
                isRe = true;
            } else {
                if ( !((c0 == '?') && (c1 == '?')) )
                    j = 0;
            }
        }
        if (j == 1)
            str = str.substring(1, l1);
        if ( !isRe )
            str = globToRegexp(str);
        return Pattern.compile(str); // tbd: add flags?
    }
}
