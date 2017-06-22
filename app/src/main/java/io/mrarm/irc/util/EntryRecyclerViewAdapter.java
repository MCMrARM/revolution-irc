package io.mrarm.irc.util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class EntryRecyclerViewAdapter extends RecyclerView.Adapter<EntryRecyclerViewAdapter.EntryHolder> {

    private static List<Class<? extends EntryHolder>> sKnownViewHolders = new ArrayList<>();
    private static List<Integer> sKnownViewHolderLayouts = new ArrayList<>();

    public static int registerViewHolder(Class<? extends EntryHolder> c, int layoutId) {
        sKnownViewHolders.add(c);
        sKnownViewHolderLayouts.add(layoutId);
        return sKnownViewHolders.size() - 1;
    }

    public static abstract class Entry {

        private EntryRecyclerViewAdapter mOwner;
        private int mIndex;

        public abstract int getViewHolder();

        void assignIndex(EntryRecyclerViewAdapter owner, int index) {
            mOwner = owner;
            mIndex = index;
        }

        protected void onUpdated() {
            mOwner.notifyItemChanged(mIndex);
        }

    }

    public static abstract class EntryHolder<T extends Entry> extends RecyclerView.ViewHolder {

        public EntryHolder(View itemView) {
            super(itemView);
        }

        public abstract void bind(T entry);

    }


    protected List<Entry> mEntries = new ArrayList<>();

    public void add(Entry entry) {
        entry.assignIndex(this, mEntries.size());
        mEntries.add(entry);
    }

    @Override
    public EntryHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(sKnownViewHolderLayouts.get(type), viewGroup, false);
        try {
            try {
                return sKnownViewHolders.get(type).getDeclaredConstructor(View.class).newInstance(view);
            } catch (NoSuchMethodException e2) {
                for (Constructor constructor : sKnownViewHolders.get(type).getDeclaredConstructors()) {
                    Class[] p = constructor.getParameterTypes();
                    if (p.length == 2 && p[0].equals(View.class) && EntryRecyclerViewAdapter.class.isAssignableFrom(p[1]))
                        return (EntryHolder) constructor.newInstance(view, this);
                }
                throw new NoSuchMethodException();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(EntryHolder viewHolder, int i) {
        viewHolder.bind(mEntries.get(i));
    }

    @Override
    public int getItemCount() {
        return mEntries.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mEntries.get(position).getViewHolder();
    }
}
