/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mrarm.irc.util;

import androidx.annotation.NonNull;
import androidx.databinding.ListChangeRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A slightly modified version of {@link androidx.databinding.ObservableArrayList}, with a helper
 * which can be used with lists that are supposed to be reordered.
 */
@SuppressWarnings("ALL")
public class ExtendedObservableArrayList<T> extends ArrayList<T>
        implements ExtendedObservableList<T> {
    private transient ListChangeRegistry mListeners = new ListChangeRegistry();
    private transient final List<ExtendedListener> mExtendedListeners = new ArrayList<>();

    @Override
    public void addOnListChangedCallback(OnListChangedCallback listener) {
        if (mListeners == null) {
            mListeners = new ListChangeRegistry();
        }
        mListeners.add(listener);
    }

    @Override
    public void removeOnListChangedCallback(OnListChangedCallback listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }

    @Override
    public void addExtendedListener(ExtendedListener listener) {
        mExtendedListeners.add(listener);
    }

    @Override
    public void removeExtendedListener(ExtendedListener listener) {
        mExtendedListeners.remove(listener);
    }

    @Override
    public boolean add(T object) {
        super.add(object);
        notifyAdded(size() - 1, 1);
        return true;
    }

    @Override
    public void add(int index, T object) {
        super.add(index, object);
        notifyAdded(index, 1);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> collection) {
        int oldSize = size();
        boolean added = super.addAll(collection);
        if (added) {
            notifyAdded(oldSize, size() - oldSize);
        }
        return added;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> collection) {
        boolean added = super.addAll(index, collection);
        if (added) {
            notifyAdded(index, collection.size());
        }
        return added;
    }

    @Override
    public void clear() {
        int oldSize = size();
        if (oldSize != 0) {
            notifyRemove(0, oldSize);
        }
        super.clear();
        if (oldSize != 0) {
            notifyRemoved(0, oldSize);
        }
    }

    @Override
    public T remove(int index) {
        notifyRemove(index, 1);
        T val = super.remove(index);
        notifyRemoved(index, 1);
        return val;
    }

    @Override
    public boolean remove(Object object) {
        int index = indexOf(object);
        if (index >= 0) {
            remove(index);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public T set(int index, T object) {
        for (ExtendedListener l : mExtendedListeners) {
            l.onChanged(this, index, get(index), object);
        }
        T val = super.set(index, object);
        if (mListeners != null) {
            mListeners.notifyChanged(this, index, 1);
        }
        return val;
    }

    @Override
    public void move(int from, int to, int count) {
        if (count == 1) {
            T tmp = super.get(from);
            if (to > from) {
                for (int i = from; i < to; i++)
                    super.set(i, super.get(i + 1));
            } else if (to < from) {
                for (int i = from; i > to; i--)
                    super.set(i, super.get(i - 1));
            }
            super.set(to, tmp);
        } else {
            List<T> vtmp = super.subList(from, count);
            ArrayList<T> tmp = new ArrayList<>(vtmp);
            vtmp.clear();
            super.addAll(to, tmp);
        }
        notifyMoved(from, to, count);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        notifyRemove(fromIndex, toIndex - fromIndex);
        super.removeRange(fromIndex, toIndex);
        notifyRemoved(fromIndex, toIndex - fromIndex);
    }

    private void notifyAdded(int start, int count) {
        if (mListeners != null) {
            mListeners.notifyInserted(this, start, count);
        }
        for (ExtendedListener l : mExtendedListeners) {
            l.onAdded(this, size() - 1, 1);
        }
    }

    private void notifyRemove(int start, int count) {
        for (ExtendedListener l : mExtendedListeners) {
            l.onRemove(this, start, count);
        }
    }

    private void notifyRemoved(int start, int count) {
        if (mListeners != null) {
            mListeners.notifyRemoved(this, start, count);
        }
    }

    private void notifyMoved(int from, int to, int count) {
        if (mListeners != null) {
            mListeners.notifyMoved(this, from, to, count);
        }
        for (ExtendedListener l : mExtendedListeners) {
            l.onMoved(this, from, to, count);
        }
    }
}
