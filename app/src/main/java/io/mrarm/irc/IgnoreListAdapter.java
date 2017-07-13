package io.mrarm.irc;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.mrarm.irc.util.ColoredTextBuilder;

public class IgnoreListAdapter extends RecyclerView.Adapter<IgnoreListAdapter.ItemHolder> {

    private List<ServerConfigData.IgnoreEntry> mEntries;
    private int mTextColorSecondary;
    private int mTextColorNick;
    private int mTextColorUser;
    private int mTextColorHost;

    public IgnoreListAdapter(Context context, List<ServerConfigData.IgnoreEntry> entries) {
        TypedArray ta = context.obtainStyledAttributes(R.style.AppTheme,
                new int[] { android.R.attr.textColorSecondary });
        mTextColorSecondary = ta.getColor(0, Color.BLACK);
        ta.recycle();
        mTextColorNick = context.getResources().getColor(R.color.ignoreEntryNick);
        mTextColorUser = context.getResources().getColor(R.color.ignoreEntryUser);
        mTextColorHost = context.getResources().getColor(R.color.ignoreEntryHost);
        mEntries = entries;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.simple_list_item, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        holder.bind(mEntries.get(position));
    }

    @Override
    public int getItemCount() {
        return mEntries == null ? 0 : mEntries.size();
    }

    public class ItemHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public ItemHolder(View itemView) {
            super(itemView);
            mText = (TextView) itemView;
        }

        public void bind(ServerConfigData.IgnoreEntry entry) {
            ColoredTextBuilder builder = new ColoredTextBuilder();
            if (entry.nick == null || entry.nick.equals("*"))
                builder.append("*", new ForegroundColorSpan(mTextColorSecondary));
            else
                builder.append(entry.nick, new ForegroundColorSpan(mTextColorNick));

            builder.append("!");
            if (entry.user == null || entry.user.equals("*"))
                builder.append("*", new ForegroundColorSpan(mTextColorSecondary));
            else
                builder.append(entry.nick, new ForegroundColorSpan(mTextColorUser));

            builder.append("@");
            if (entry.host == null || entry.host.equals("*"))
                builder.append("*", new ForegroundColorSpan(mTextColorSecondary));
            else
                builder.append(entry.host, new ForegroundColorSpan(mTextColorHost));
            mText.setText(builder.getSpannable());
        }

    }

}
