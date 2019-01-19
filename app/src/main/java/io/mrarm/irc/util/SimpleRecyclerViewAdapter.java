package io.mrarm.irc.util;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class SimpleRecyclerViewAdapter<VH extends SimpleRecyclerViewAdapter.ViewHolder<IT>, IT>
        extends RecyclerView.Adapter<VH> {

    private ViewHolderFactory<VH> mViewHolderFactory;
    private List<IT> mItems;
    private int mViewResId;

    public SimpleRecyclerViewAdapter() {
    }

    public SimpleRecyclerViewAdapter(ViewHolderFactory<VH> viewHolderFactory, int viewResId,
                                     List<IT> items) {
        mViewHolderFactory = viewHolderFactory;
        mViewResId = viewResId;
        mItems = items;
    }

    public void setViewHolderFactory(ViewHolderFactory<VH> viewHolderFactory, int viewResId) {
        mViewHolderFactory = viewHolderFactory;
        mViewResId = viewResId;
    }

    public void setItems(List<IT> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    public List<IT> getItems() {
        return mItems;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(mViewResId, parent, false);
        return mViewHolderFactory.createViewHolder(view);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(mItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems != null ? mItems.size() : 0;
    }

    public static class ViewHolder<IT> extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void bind(IT item) {
            //
        }

    }

    public interface ViewHolderFactory<VH extends SimpleRecyclerViewAdapter.ViewHolder> {

        VH createViewHolder(View view);

    }

}
