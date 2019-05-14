package io.mrarm.irc.util;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a simple Deque-like list implementeed on two ArrayLists.
 * This container supports the following operations:
 * - addFirst(item), addLast(item)
 * - remove(index), removeFirst()
 * - get(index)
 * - size()
 * @param <T> item type
 */
public final class TwoWayList<T> {

    private final List<T> mBefore = new ArrayList<>();
    private final List<T> mAfter = new ArrayList<>();

    /**
     * Removes all elements.
     * The time complexity is O(1).
     */
    public void clear() {
        mBefore.clear();
        mAfter.clear();
    }

    /**
     * Adds a new element to the beginning of the list.
     * The time complexity is equal to that of ArrayList.add.
     * @param item the item to add
     */
    public void addFirst(T item) {
        mBefore.add(item);
    }

    /**
     * Adds a new element to the end of the list.
     * The time complexity is equal to that of ArrayList.add.
     * @param item the item to add
     */
    public void addLast(T item) {
        mAfter.add(item);
    }

    /**
     * Gets the element from the list.
     * The time complexity is O(1).
     * @param index the index of the item to get
     * @return the item
     * @throws IndexOutOfBoundsException if the index is not valid
     */
    public T get(int index) {
        if (index < mBefore.size())
            return mBefore.get(mBefore.size() - 1 - index);
        index -= mBefore.size();
        return mAfter.get(index);
    }

    /**
     * Gets the total count of items.
     * @return the total count of items
     */
    public int size() {
        return mBefore.size() + mAfter.size();
    }

    /**
     * Removes an item at the specified index.
     * The time complexity should be assumed to be O(n) for most cases (except for the first and
     * last element, see removeFirst).
     * @param index the index to remove
     * @throws IndexOutOfBoundsException if the index is not valid
     */
    public T remove(int index) {
        if (index < mBefore.size()) {
            return mBefore.remove(mBefore.size() - 1 - index);
        }
        index -= mBefore.size();
        return mAfter.remove(index);
    }

    /**
     * Removes the first element from the list.
     * If the element has been added using addFirst, the time complexity is defined to be O(1).
     * Otherwise the time complexity is O(n).
     * @throws IndexOutOfBoundsException if the list is empty
     */
    public T removeFirst() {
        return remove(0);
    }

}
