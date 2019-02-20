package io.mrarm.irc.util;

import java.util.List;

public class ListUtils {

    /**
     * Finds the index of the first element in the list that is greater or equal to the specified
     * value.
     * If the element is not found, this function will return the list size.
     * @param list the list
     * @param value the value to compare to
     * @param <T> the list type
     * @return the index of the first list element matching the criteria
     */
    public static <T extends Comparable<T>> int lowerBound(List<T> list, T value) {
        int a = 0, b = list.size();
        while (b - a > 1) {
            int m = (a + b) / 2;
            if (list.get(m).compareTo(value) >= 0) {
                b = m;
            } else {
                a = m;
            }
        }
        return b;
    }

}
