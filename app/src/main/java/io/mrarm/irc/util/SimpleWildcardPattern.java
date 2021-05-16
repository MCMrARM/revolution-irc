package io.mrarm.irc.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SimpleWildcardPattern {

    private static String parseGlob(String str) {
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

    private static String strGlobRe(String str) {
        if (str.startsWith("/") && str.endsWith("/"))
            return str.substring(1, str.length() - 1);
        if (str.startsWith("?") && str.endsWith("?"))
            return parseGlob(str.substring(1, str.length() - 1));
        return parseGlob(str);
    }

    public static Pattern pattCompile(String str) throws PatternSyntaxException {
        return Pattern.compile(strGlobRe(str));
    }
}
