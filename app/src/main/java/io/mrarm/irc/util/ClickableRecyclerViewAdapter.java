package io.mrarm.irc.util;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class ClickableRecyclerViewAdapter<VH extends ClickableRecyclerViewAdapter.ViewHolder<IT>, IT>
        extends SimpleRecyclerViewAdapter<VH, IT> {

    private ItemClickListener<IT> mItemClickListener;

    public ClickableRecyclerViewAdapter() {
    }

    public ClickableRecyclerViewAdapter(ViewHolderFactory<VH> viewHolderFactory, int viewResId,
                                        List<IT> items) {
        super(viewHolderFactory, viewResId, items);
    }

    public void setItemClickListener(ItemClickListener<IT> listener) {
        mItemClickListener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        VH ret = super.onCreateViewHolder(parent, viewType);
        ret.setClickListener(this);
        return ret;
    }

    public static class ViewHolder<IT> extends SimpleRecyclerViewAdapter.ViewHolder<IT> {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        void setClickListener(ClickableRecyclerViewAdapter<?, IT> adapter) {
            itemView.setOnClickListener((View v) -> {
                if (adapter.mItemClickListener != null)
                    adapter.mItemClickListener.onItemClick(getAdapterPosition(), adapter.getItems()
                            .get(getAdapterPosition()));
            });
        }

    }

    public interface ItemClickListener<IT> {

        void onItemClick(int index, IT value);

    }

}
