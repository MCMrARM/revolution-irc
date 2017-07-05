package io.mrarm.irc.util;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class ListAdapterWrapper implements ListAdapter {

    private ListAdapter mWrapped;

    public ListAdapterWrapper(ListAdapter wrapped) {
        mWrapped = wrapped;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mWrapped.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        return mWrapped.isEnabled(position);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mWrapped.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mWrapped.unregisterDataSetObserver(observer);
    }

    @Override
    public int getCount() {
        return mWrapped.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mWrapped.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mWrapped.getItemId(position);
    }

    @Override
    public boolean hasStableIds() {
        return mWrapped.hasStableIds();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return mWrapped.getView(position, convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return mWrapped.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return mWrapped.getViewTypeCount();
    }

    @Override
    public boolean isEmpty() {
        return mWrapped.isEmpty();
    }

}
