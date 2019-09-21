package io.mrarm.irc.util;

public interface ExtendedObservableList<T> extends ReorderableObservableList<T> {

    void addExtendedListener(ExtendedListener<? extends ExtendedObservableList<T>, T> listener);

    void removeExtendedListener(ExtendedListener<? extends ExtendedObservableList<T>, T> listener);


    interface ExtendedListener<T extends ExtendedObservableList<V>, V> {

        void onRemove(T source, int posStart, int count);

        void onAdded(T source, int posStart, int count);

        void onChanged(T source, int index, V oldValue, V newValue);

        void onMoved(T source, int fromIndex, int toIndex, int count);

    }

    interface SimpleExtendedListener<T extends ExtendedObservableList<V>, V>
            extends ExtendedListener<T, V> {

        void onAdded(T source, int index, V value);

        void onRemove(T source, int index, V value);

        @Override
        default void onAdded(T source, int posStart, int count) {
            for (int i = 0; i < count; i++)
                onAdded(source, posStart + i, source.get(posStart + i));
        }

        @Override
        default void onRemove(T source, int posStart, int count) {
            for (int i = 0; i < count; i++)
                onRemove(source, posStart + i, source.get(posStart + i));
        }

        @Override
        default void onMoved(T source, int fromIndex, int toIndex, int count) {
            for (int i = 0; i < count; i++)
                onRemove(source, fromIndex + i, source.get(toIndex + i));
            for (int i = 0; i < count; i++)
                onAdded(source, toIndex + i, source.get(toIndex + i));
        }

        @Override
        default void onChanged(T source, int index, V oldValue, V newValue) {
            onRemove(source, index, oldValue);
            onAdded(source, index, newValue);
        }
    }

}
