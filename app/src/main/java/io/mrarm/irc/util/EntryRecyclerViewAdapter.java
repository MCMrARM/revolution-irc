package io.mrarm.irc.util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    public static EntryHolder createRegisteredViewHolder(ViewGroup viewGroup, int id) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(sKnownViewHolderLayouts.get(id), viewGroup, false);
        try {
            return sKnownViewHolders.get(id).getDeclaredConstructor(View.class).newInstance(view);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static abstract class Entry {

        public abstract int getViewHolder();

    }

    public static abstract class EntryHolder<T extends Entry> extends RecyclerView.ViewHolder {

        public EntryHolder(View itemView) {
            super(itemView);
        }

        public abstract void bind(T entry);

    }


    private List<Entry> mEntries = new ArrayList<>();

    public void add(Entry entry) {
        mEntries.add(entry);
    }

    @Override
    public EntryHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        return createRegisteredViewHolder(viewGroup, type);
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

}
