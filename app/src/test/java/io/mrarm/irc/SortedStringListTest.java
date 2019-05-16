package io.mrarm.irc;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.mrarm.irc.util.SortedStringList;

import static org.junit.Assert.assertEquals;

public class SortedStringListTest {

    private void validate(List<String> str) {
        List<String> copy = new ArrayList<>(str);
        Collections.sort(copy);
        assertEquals(str.size(), copy.size());
        for (int i = 0; i < copy.size(); i++)
            assertEquals(str.get(i), copy.get(i));
    }

    @Test
    public void basicTest() {
        List<String> test = new ArrayList<>();
        test.add("a");
        test.add("b");
        test.add("e");
        SortedStringList.add(test, "d");
        validate(test);
        SortedStringList.add(test, "f");
        validate(test);
        SortedStringList.add(test, "c");
        validate(test);
        SortedStringList.add(test, "a");
        validate(test);
        SortedStringList.add(test, "g");
        SortedStringList.add(test, "e");
        validate(test);
        assertEquals(0, SortedStringList.lowerBound(test, "a"));
        assertEquals(2, SortedStringList.upperBound(test, "a"));
        assertEquals(2, SortedStringList.lowerBound(test, "b"));
        assertEquals(5, SortedStringList.lowerBound(test, "e"));
        assertEquals(7, SortedStringList.upperBound(test, "e"));
    }

    @Test
    public void randomAddTest() {
        Random r = new Random(1337);
        for (int i = 0; i < 1000; i++) {
            List<String> test = new ArrayList<>();
            for (int j = 0; j < 500; j++)
                SortedStringList.add(test, String.valueOf(r.nextLong()));
            validate(test);
        }
    }

}
