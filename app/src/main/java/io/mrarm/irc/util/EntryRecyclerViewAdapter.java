package io.mrarm.irc.util;

import androidx.recyclerview.widget.RecyclerView;
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
        private boolean mUpdatesDirectly;

        public abstract int getViewHolder();

        void assignIndex(EntryRecyclerViewAdapter owner, int index) {
            if (mOwner != null && mOwner != owner)
                throw new RuntimeException("Entry is already attached to an EntryRecyclerViewAdapter");
            mOwner = owner;
            mIndex = index;
        }

        public EntryRecyclerViewAdapter getOwner() {
            return mOwner;
        }

        public int getIndex() {
            return mIndex;
        }

        /**
         * Marks that this element has custom update logic and shouldn't use notifyItemChanged.
         * @param updatesDirectly whether the element uses custom update logic
         */
        protected void setUpdatesDirectly(boolean updatesDirectly) {
            mUpdatesDirectly = updatesDirectly;
        }

        protected void onUpdated(boolean doNotNotifyRV) {
            if (mOwner != null && mIndex != -1 && !mUpdatesDirectly && !doNotNotifyRV)
                mOwner.notifyItemChanged(mIndex);
        }

        protected final void onUpdated() {
            onUpdated(false);
        }

    }

    public static abstract class EntryHolder<T extends Entry> extends RecyclerView.ViewHolder {

        private T mEntry;

        public EntryHolder(View itemView) {
            super(itemView);
        }

        protected T getEntry() {
            return mEntry;
        }

        public abstract void bind(T entry);

        public void unbind() {
        }

    }


    protected List<Entry> mEntries = new ArrayList<>();

    public void add(Entry entry) {
        entry.assignIndex(this, mEntries.size());
        mEntries.add(entry);
        notifyItemInserted(mEntries.size() - 1);
    }

    public void add(int index, Entry entry) {
        entry.assignIndex(this, index);
        for (int i = index; i < mEntries.size(); i++)
            mEntries.get(i).mIndex++;
        mEntries.add(index, entry);
        notifyItemInserted(index);
    }

    public void remove(int index) {
        Entry entry = mEntries.get(index);
        entry.mIndex = -1;
        entry.mOwner = null;
        mEntries.remove(index);
        for (int i = index; i < mEntries.size(); i++)
            mEntries.get(i).mIndex--;
        notifyItemRemoved(index);
    }

    public List<Entry> getEntries() {
        return mEntries;
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
        viewHolder.mEntry = mEntries.get(i);
        viewHolder.bind(viewHolder.mEntry);
    }

    @Override
    public void onViewRecycled(EntryHolder viewHolder) {
        viewHolder.unbind();
        viewHolder.mEntry = null;
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
