package io.mrarm.irc.util;

import java.util.List;

public class SortedStringList {

    public static int lowerBound(List<String> str, String value) {
        int a = 0, b = str.size(), c;
        while (a != b) {
            c = (a + b) / 2;
            if (str.get(c).compareTo(value) < 0)
                a = c + 1;
            else
                b = c;
        }
        return a;
    }

    public static int upperBound(List<String> str, String value) {
        int a = 0, b = str.size(), c;
        while (a != b) {
            c = (a + b) / 2;
            if (str.get(c).compareTo(value) <= 0)
                a = c + 1;
            else
                b = c;
        }
        return a;
    }

    public static int indexOf(List<String> str, String value) {
        if (str.isEmpty())
            return -1;
        int index = lowerBound(str, value);
        if (str.get(index).equals(value))
            return index;
        return -1;
    }

    public static void add(List<String> str, String value) {
        str.add(upperBound(str, value), value);
    }

    public static boolean remove(List<String> str, String value) {
        int iof = indexOf(str, value);
        if (iof == -1)
            return false;
        str.remove(iof);
        return true;
    }

}
