package io.mrarm.irc.chat;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.config.CommandAliasManager;

public class CommandListSuggestionsAdapter extends BaseAdapter implements Filterable {

    private Context mContext;
    private List<CommandAliasManager.CommandAlias> mFilteredItems;
    private MyFilter mFilter;
    private int mSecondaryTextColor;

    public CommandListSuggestionsAdapter(Context context) {
        mContext = context;
        mFilteredItems = null;

        TypedArray ta = context.getTheme().obtainStyledAttributes(R.style.AppTheme,
                new int[] { android.R.attr.textColorSecondary });
        mSecondaryTextColor = ta.getColor(0, Color.BLACK);
        ta.recycle();
    }

    @Override
    public int getCount() {
        return mFilteredItems == null ? 0 : mFilteredItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mFilteredItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null)
            mFilter = new MyFilter();
        return mFilter;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.simple_list_item, parent, false);
        }
        CommandAliasManager.CommandAlias item = mFilteredItems.get(position);
        SpannableString str = new SpannableString("/" + item.name + " " + item.syntax);
        str.setSpan(new ForegroundColorSpan(mSecondaryTextColor), item.name.length() + 1, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) convertView).setText(str);
        return convertView;
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