package io.mrarm.irc.util.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ObservableList<T> implements List<T> {

    private final List<T> mBacking;
    private final List<Listener<T>> mListeners = new ArrayList<>();

    public ObservableList(List<T> backing) {
        mBacking = backing;
    }

    public void addListener(Listener<T> listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener<T> listener) {
        mListeners.remove(listener);
    }

    @Override
    public int size() {
        return mBacking.size();
    }

    @Override
    public boolean isEmpty() {
        return mBacking.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return mBacking.contains(o);
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return mBacking.iterator(); // TODO: Handle Iterator.remove?
    }

    @Nullable
    @Override
    public Object[] toArray() {
        return mBacking.toArray();
    }

    @Override
    public <T1> T1[] toArray(@Nullable T1[] a) {
        return mBacking.toArray(a);
    }

    @Override
    public boolean add(T t) {
        boolean ret = mBacking.add(t);
        if (ret) {
            for (Listener<T> l : mListeners)
                l.onItemAdded(mBacking.size() - 1, t);
        }
        return ret;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        @SuppressWarnings("SuspiciousMethodCalls")
        int iof = indexOf(o);
        if (iof == -1)
            return false;
        remove(iof); // will call the listener
        return true;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return mBacking.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        int si = size();
        boolean b = mBacking.addAll(c);
        if (!b)
            return false;
        int tsi = size();
        for (Listener<T> l : mListeners)
            l.onItemRangeAdded(si, tsi);
        return true;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        boolean b = mBacking.addAll(index, c);
        if (!b)
            return false;
        for (Listener<T> l : mListeners)
            l.onItemRangeAdded(index, index + c.size());
        return true;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        int s = size();
        mBacking.clear();
        for (Listener<T> l : mListeners)
            l.onItemRangeRemoved(0, s);
    }

    @Override
    public T get(int index) {
        return mBacking.get(index);
    }

    @Override
    public T set(int index, T element) {
        T old = mBacking.set(index, element);
        for (Listener<T> l : mListeners)
            l.onItemChanged(index, old, element);
        return old;
    }

    @Override
    public void add(int index, T element) {
        mBacking.add(index, element);
        for (Listener<T> l : mListeners)
            l.onItemAdded(index, element);
    }

    @Override
    public T remove(int index) {
        T element = mBacking.remove(index);
        for (Listener<T> l : mListeners)
            l.onItemRemoved(index, element);
        return element;
    }

    @Override
    public int indexOf(@Nullable Object o) {
        return mBacking.indexOf(o);
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
        return mBacking.lastIndexOf(o);
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator() {
        return mBacking.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return mBacking.listIterator(index);
    }

    @NonNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return mBacking.subList(fromIndex, toIndex);
    }

    public interface Listener<T> {

        void onItemAdded(int index, T value);

        void onItemRemoved(int index, T value);

        void onItemChanged(int index, T oldValue, T newValue);

        void onItemRangeAdded(int startIndex, int endIndexExclusive);

        void onItemRangeRemoved(int startIndex, int endIndexExclusive);

    }

}
