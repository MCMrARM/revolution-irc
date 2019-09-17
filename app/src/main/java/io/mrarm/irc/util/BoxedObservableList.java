package io.mrarm.irc.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ListChangeRegistry;
import androidx.databinding.ObservableList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A class that boxes an ObservableList and allows to quickly and easily swap the underlying list,
 * while still keeping the same object reference and maintaining the listeners.
 * @param <T>
 */
public class BoxedObservableList<T> implements ObservableList<T> {

    private ObservableList<T> mWrapped;
    private final ListChangeRegistry mCallbacks = new ListChangeRegistry();
    private int mCallbackCount = 0;
    private OnListChangedCallback<ObservableList<T>> mCallbackDispatcher = new CallbackDispatcher();

    public BoxedObservableList(ObservableList<T> wrapped) {
        mWrapped = wrapped;
    }

    public void set(ObservableList<T> wrapped) {
        if (mWrapped != null && mWrapped.size() > 0)
            mCallbacks.notifyRemoved(this, 0, mWrapped.size());
        mWrapped = wrapped;
        if (wrapped.size() > 0)
            mCallbacks.notifyInserted(this, 0, wrapped.size());
    }

    @Override
    public void addOnListChangedCallback(
            OnListChangedCallback<? extends ObservableList<T>> callback) {
        if (mCallbackCount++ == 0)
            mWrapped.addOnListChangedCallback(mCallbackDispatcher);
        mCallbacks.add(callback);
    }

    @Override
    public void removeOnListChangedCallback(
            OnListChangedCallback<? extends ObservableList<T>> callback) {
        mCallbacks.remove(callback);
        if (--mCallbackCount == 0)
            mWrapped.removeOnListChangedCallback(mCallbackDispatcher);
    }

    @Override
    public int size() {
        return mWrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return mWrapped.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return mWrapped.contains(o);
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return mWrapped.iterator();
    }

    @Nullable
    @Override
    public Object[] toArray() {
        return mWrapped.toArray();
    }

    @Override
    public <T1> T1[] toArray(@Nullable T1[] a) {
        return mWrapped.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return mWrapped.add(t);
    }

    @Override
    public boolean remove(@Nullable Object o) {
        return mWrapped.remove(o);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return mWrapped.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        return mWrapped.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        return mWrapped.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return mWrapped.removeAll(c);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return mWrapped.retainAll(c);
    }

    @Override
    public void clear() {
        mWrapped.clear();
    }

    @Override
    public T get(int index) {
        return mWrapped.get(index);
    }

    @Override
    public T set(int index, T element) {
        return mWrapped.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        mWrapped.add(index, element);
    }

    @Override
    public T remove(int index) {
        return mWrapped.remove(index);
    }

    @Override
    public int indexOf(@Nullable Object o) {
        return mWrapped.indexOf(o);
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
        return mWrapped.lastIndexOf(o);
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator() {
        return mWrapped.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return mWrapped.listIterator(index);
    }

    @NonNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return mWrapped.subList(fromIndex, toIndex);
    }

    private class CallbackDispatcher extends OnListChangedCallback<ObservableList<T>> {

        @Override
        public void onChanged(ObservableList sender) {
            mCallbacks.notifyChanged(BoxedObservableList.this);
        }

        @Override
        public void onItemRangeChanged(ObservableList sender, int positionStart, int itemCount) {
            mCallbacks.notifyChanged(BoxedObservableList.this, positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(ObservableList sender, int positionStart, int itemCount) {
            mCallbacks.notifyInserted(BoxedObservableList.this, positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(ObservableList sender, int fromPosition, int toPosition, int itemCount) {
            mCallbacks.notifyMoved(BoxedObservableList.this, fromPosition, toPosition, itemCount);
        }

        @Override
        public void onItemRangeRemoved(ObservableList sender, int positionStart, int itemCount) {
            mCallbacks.notifyRemoved(BoxedObservableList.this, positionStart, itemCount);
        }

    }

}
