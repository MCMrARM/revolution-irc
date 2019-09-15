package io.mrarm.irc.util;

import android.util.Log;

import androidx.databinding.ObservableList;

import io.mrarm.dataadapter.DataAdapter;
import io.mrarm.dataadapter.DataFragment;

/**
 * This might be called a minor hack, however the whole problem of
 */
public class ReorderableDataAdapter extends DataAdapter {

    private boolean inReorderTransaction = false;
    private int removedIndex = -1;
    private int addedIndex = -1;

    public <T> void move(ObservableList<T> src, int srcIndex,
                         ObservableList<T> dest, int destIndex) {
        beginReorderTransaction();
        T o = src.remove(srcIndex);
        dest.add(destIndex, o);
        endReorderTransaction();
    }

    public void beginReorderTransaction() {
        if (inReorderTransaction)
            throw new RuntimeException("beginReorderTransaction MUST NOT be called twice");
        inReorderTransaction = true;
        addedIndex = -1;
        removedIndex = -1;
    }

    public void endReorderTransaction() {
        if (!inReorderTransaction)
            return;
        inReorderTransaction = false;
        if (removedIndex != -1 && addedIndex != -1) {
            onItemRangeMoved(getSource(), removedIndex, addedIndex, 1);
            return;
        }
        Log.w("ReorderableDataAdapter", "Reorder transaction failed");
        if (removedIndex != -1)
            onItemRangeRemoved(getSource(), removedIndex, 1);
        if (addedIndex != -1)
            onItemRangeInserted(getSource(), addedIndex, 1);
    }

    @Override
    public void onItemRangeRemoved(DataFragment fragment, int index, int count) {
        if (inReorderTransaction) {
            if (count != 1 || removedIndex != -1) {
                // transaction failed
                endReorderTransaction();
            } else {
                removedIndex = index;
                return;
            }
        }
        super.onItemRangeRemoved(fragment, index, count);
    }

    @Override
    public void onItemRangeInserted(DataFragment fragment, int index, int count) {
        if (inReorderTransaction) {
            // if transaction is ok, set the added index, but complete the transaction either way
            if (count != 1 || removedIndex == -1 || addedIndex != -1) {
                // transaction failed
                endReorderTransaction();
            } else {
                addedIndex = index;
                endReorderTransaction();
                return;
            }
        }
        super.onItemRangeInserted(fragment, index, count);
    }

}
