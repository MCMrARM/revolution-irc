package io.mrarm.irc.dialog;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class MenuBottomSheetDialog extends ProperHeightBottomSheetDialog {

    private List<Object> mItems = new ArrayList<>();

    public MenuBottomSheetDialog(@NonNull Context context) {
        super(context);

        RecyclerView content = new RecyclerView(context);
        content.setLayoutManager(new LinearLayoutManager(context));
        content.setAdapter(new ItemAdapter());
        int verticalPadding = content.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_menu_vertical_margin);
        content.setPadding(0, verticalPadding, 0, verticalPadding);
        setContentView(content);

        content.setBackgroundColor(StyledAttributesHelper.getColor(context, R.attr.colorBackgroundFloating, 0));
    }

    public void addHeader(CharSequence text) {
        HeaderItem item = new HeaderItem();
        item.mText = text;
        mItems.add(item);
    }

    public void addItem(int stringId, int iconId, OnItemClickListener listener) {
        addItem(getContext().getString(stringId), iconId, listener);
    }

    public void addItem(CharSequence text, int iconId, OnItemClickListener listener) {
        Item item = new Item();
        item.mText = text;
        item.mIconId = iconId;
        item.mClickListener = listener;
        mItems.add(item);
    }

    public static class Item {

        CharSequence mText;
        int mIconId;
        OnItemClickListener mClickListener;

    }

    public static class HeaderItem {

        CharSequence mText;

    }

    public interface OnItemClickListener {

        boolean onClick(Item item);

    }


    protected class ItemAdapter extends RecyclerView.Adapter {

        private static final int TYPE_ITEM = 0;
        private static final int TYPE_HEADER = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_ITEM) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_bottom_menu_item, parent, false);
                return new ItemHolder(view);
            }
            if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_bottom_menu_header, parent, false);
                return new HeaderItemHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object item = mItems.get(position);
            int type = holder.getItemViewType();
            if (type == TYPE_ITEM)
                ((ItemHolder) holder).bind((Item) item);
            if (type == TYPE_HEADER)
                ((HeaderItemHolder) holder).bind((HeaderItem) item);
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
            if (item instanceof HeaderItem)
                return TYPE_HEADER;
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

    protected class HeaderItemHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public HeaderItemHolder(View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.text);
        }

        public void bind(HeaderItem item) {
            mText.setText(item.mText);
        }

    }

}
