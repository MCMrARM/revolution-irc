package io.mrarm.irc.util.collection;

import androidx.recyclerview.widget.RecyclerView;

public class ObservableListAdapterHelper<T> implements ObservableList.Listener<T> {

    private final RecyclerView.Adapter mAdapter;
    private int mAdapterOffset;
    private final ObservableList<T> mList;
    private boolean mBound = false;

    public ObservableListAdapterHelper(RecyclerView.Adapter adapter, int adapterOffset,
                                       ObservableList<T> list) {
        mAdapter = adapter;
        mAdapterOffset = adapterOffset;
        mList = list;
        bind();
    }

    public void bind() {
        if (mBound)
            return;
        mList.addListener(this);
        mBound = true;
    }

    public void unbind() {
        if (!mBound)
            return;
        mList.removeListener(this);
        mBound = false;
    }

    public void setOffset(int offset) {
        mAdapterOffset = offset;
    }

    public T get(int index) {
        return mList.get(index);
    }

    public int size()  {
        return mList.size();
    }


    @Override
    public void onItemAdded(int index, T value) {
        mAdapter.notifyItemInserted(index);
    }

    @Override
    public void onItemRemoved(int index, T value) {
        mAdapter.notifyItemRemoved(index);
    }

    @Override
    public void onItemChanged(int index, T oldValue, T newValue) {
        mAdapter.notifyItemChanged(index);
    }

    @Override
    public void onItemRangeAdded(int startIndex, int endIndexExclusive) {
        mAdapter.notifyItemRangeInserted(startIndex, endIndexExclusive - startIndex);
    }

    @Override
    public void onItemRangeRemoved(int startIndex, int endIndexExclusive) {
        mAdapter.notifyItemRangeRemoved(startIndex, endIndexExclusive - startIndex);
    }

}
