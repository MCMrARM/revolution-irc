package io.mrarm.irc.util;

import java.util.regex.Pattern;

public class SimpleWildcardPattern {

    public static Pattern compile(String str) {
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
        return Pattern.compile(ret.toString());
    }

}
