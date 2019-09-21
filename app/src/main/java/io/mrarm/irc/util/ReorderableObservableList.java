package io.mrarm.irc.util;

import androidx.databinding.ObservableList;

public interface ReorderableObservableList<T> extends ObservableList<T> {

    void move(int from, int to, int count);

}
