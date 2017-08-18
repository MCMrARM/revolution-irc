package io.mrarm.irc.chat;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.util.StyledAttributesHelper;

public class CommandListSuggestionsAdapter extends RecyclerView.Adapter<CommandListSuggestionsAdapter.ItemHolder> implements Filterable {

    private Context mContext;
    private List<CommandAliasManager.CommandAlias> mFilteredItems;
    private MyFilter mFilter;
    private int mSecondaryTextColor;
    private ChatSuggestionsAdapter.OnItemClickListener mClickListener;

    public CommandListSuggestionsAdapter(Context context) {
        mContext = context;
        mFilteredItems = null;

        mSecondaryTextColor = StyledAttributesHelper.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);
    }

    public void setClickListener(ChatSuggestionsAdapter.OnItemClickListener listener) {
        mClickListener = listener;
    }

    public CommandAliasManager.CommandAlias getItem(int position) {
        return mFilteredItems.get(position);
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.simple_list_item, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        holder.bind(mFilteredItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mFilteredItems != null ? mFilteredItems.size() : 0;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null)
            mFilter = new MyFilter();
        return mFilter;
    }

    public class ItemHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public ItemHolder(View view) {
            super(view);
            mText = (TextView) view;
            view.setOnClickListener((View v) -> {
                mClickListener.onItemClick(v.getTag());
            });
        }

        public void bind(CommandAliasManager.CommandAlias item) {
            itemView.setTag(item);
            SpannableString str = new SpannableString("/" + item.name + " " + item.syntax);
            str.setSpan(new ForegroundColorSpan(mSecondaryTextColor), item.name.length() + 1, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mText.setText(str);
        }

    }

    private class MyFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults ret = new FilterResults();
            String str = constraint.toString().toLowerCase();
            if (str.charAt(0) == '/')
                str = str.substring(1);
            List<CommandAliasManager.CommandAlias> list = new ArrayList<>();
            for (CommandAliasManager.CommandAlias alias : CommandAliasManager.getDefaultAliases()) {
                if (alias.name.regionMatches(true, 0, str, 0, str.length()))
                    list.add(alias);
            }
            for (CommandAliasManager.CommandAlias alias : CommandAliasManager.getInstance(mContext).getUserAliases()) {
                if (alias.name.regionMatches(true, 0, str, 0, str.length()))
                    list.add(alias);
            }
            Collections.sort(list, (CommandAliasManager.CommandAlias l, CommandAliasManager.CommandAlias r) -> l.name.compareTo(r.name));
            ret.values = list;
            ret.count = list.size();
            return ret;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredItems = (List<CommandAliasManager.CommandAlias>) results.values;
            notifyDataSetChanged();
        }

    }

}