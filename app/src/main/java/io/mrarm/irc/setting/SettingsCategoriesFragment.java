package io.mrarm.irc.setting;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;

public abstract class SettingsCategoriesFragment extends Fragment {

    public abstract List<Item> getItems();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.simple_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.items);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new Adapter(getItems()));
        return view;
    }

    public static class Item {

        private int mTextId;
        private int mIconId;
        private View.OnClickListener mOnClickListener;

        public Item(int textId, int iconId) {
            mTextId = textId;
            mIconId = iconId;
        }

        public Item(int textId, int iconId, Class<? extends Fragment> fragmentClass) {
            mTextId = textId;
            mIconId = iconId;
            mOnClickListener = (View v) -> {
                try {
                    ((SettingsActivity) v.getContext()).setFragment(fragmentClass.newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        }

        public Item(int textId, int iconId, View.OnClickListener listener) {
            mTextId = textId;
            mIconId = iconId;
            mOnClickListener = listener;
        }

        public int getTextId() {
            return mTextId;
        }

        public int getIconId() {
            return mIconId;
        }

    }

    private static class Adapter extends RecyclerView.Adapter<Adapter.ItemHolder> {

        private List<Item> mItems;

        public Adapter(List<Item> items) {
            mItems = items;
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_category_item, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemHolder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public static class ItemHolder extends RecyclerView.ViewHolder {

            private TextView mText;
            private ImageView mIcon;

            public ItemHolder(View itemView) {
                super(itemView);
                mText = itemView.findViewById(R.id.text);
                mIcon = itemView.findViewById(R.id.image);
                itemView.setOnClickListener((View v) -> {
                    ((Item) itemView.getTag()).mOnClickListener.onClick(v);
                });
            }

            public void bind(Item item) {
                itemView.setTag(item);
                mText.setText(item.getTextId());
                mIcon.setImageResource(item.getIconId());
            }

        }

    }

}
