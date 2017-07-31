package io.mrarm.irc.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;

public class MenuBottomSheetDialog extends BottomSheetDialog {

    private List<Object> mItems = new ArrayList<>();

    public MenuBottomSheetDialog(@NonNull Context context) {
        super(context);

        RecyclerView content = new RecyclerView(context);
        content.setLayoutManager(new LinearLayoutManager(context));
        content.setAdapter(new ItemAdapter());
        int verticalPadding = content.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_menu_vertical_margin);
        content.setPadding(0, verticalPadding, 0, verticalPadding);
        setContentView(content);
    }

    public void addItem(int stringId, int iconId, OnItemClickListener listener) {
        Item item = new Item();
        item.mText = getContext().getString(stringId);
        item.mIconId = iconId;
        item.mClickListener = listener;
        mItems.add(item);
    }

    public static class Item {

        CharSequence mText;
        int mIconId;
        OnItemClickListener mClickListener;

    }

    public interface OnItemClickListener {

        boolean onClick(Item item);

    }


    protected class ItemAdapter extends RecyclerView.Adapter {

        private static final int TYPE_ITEM = 0;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_ITEM) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_bottom_menu_item, parent, false);
                return new ItemHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object item = mItems.get(position);
            int type = holder.getItemViewType();
            if (type == TYPE_ITEM)
                ((ItemHolder) holder).bind((Item) item);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            Object item = mItems.get(position);
            if (item instanceof Item)
                return TYPE_ITEM;
            return -1;
        }

    }

    protected class ItemHolder extends RecyclerView.ViewHolder {

        private ImageView mIcon;
        private TextView mText;

        public ItemHolder(View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.icon);
            mText = itemView.findViewById(R.id.text);
            itemView.setOnClickListener((View v) -> {
                Item item = (Item) mItems.get(getAdapterPosition());
                if (item.mClickListener != null && item.mClickListener.onClick(item))
                    dismiss();
            });
        }

        public void bind(Item item) {
            mIcon.setImageResource(item.mIconId);
            mText.setText(item.mText);
        }

    }

}
