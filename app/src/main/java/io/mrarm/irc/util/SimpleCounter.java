package io.mrarm.irc.util;

public class SimpleCounter {

    private int i = 0;

    public SimpleCounter(int i) {
        this.i = i;
    }

    public int next() {
        return i++;
    }

}
