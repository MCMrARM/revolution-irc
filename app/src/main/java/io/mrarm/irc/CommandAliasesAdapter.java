package io.mrarm.irc;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.mrarm.irc.util.AdvancedDividerItemDecoration;

public class CommandAliasesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private CommandAliasManager mManager;
    private RecyclerView mRecyclerView;

    public CommandAliasesAdapter(Context context) {
        mManager = CommandAliasManager.getInstance(context);
    }

    public ItemDecoration createItemDecoration(Context context) {
        return new ItemDecoration(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        if (type == TYPE_HEADER)
            return new HeaderHolder(inflater.inflate(R.layout.settings_list_header,
                    viewGroup, false));
        return new ItemHolder(inflater.inflate(R.layout.settings_command_alias_item,
                viewGroup, false));
    }

    @Override
    public int getItemViewType(int position) {
        int userAliases = mManager.getUserAliases().size();
        if (position == 0 || (userAliases > 0 && position == userAliases + 1))
            return TYPE_HEADER;
        return TYPE_ITEM;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder itemHolder, int pos) {
        int type = itemHolder.getItemViewType();
        if (type == TYPE_HEADER) {
            ((HeaderHolder) itemHolder).bind(pos == 0 ? R.string.value_default : R.string.value_custom);
        } else {
            int userAliases = mManager.getUserAliases().size();
            userAliases = (userAliases > 0 ? 1 + userAliases : 0);
            ((ItemHolder) itemHolder).bind(pos > userAliases
                    ? CommandAliasManager.getDefaultAliases().get(pos - 1 - userAliases)
                    : mManager.getUserAliases().get(pos - 1));
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        if (recyclerView == mRecyclerView)
            mRecyclerView = null;
    }

    @Override
    public int getItemCount() {
        int userAliases = mManager.getUserAliases().size();
        int defaultAliases = CommandAliasManager.getDefaultAliases().size();
        return (userAliases > 0 ? 1 + userAliases : 0) + 1 + defaultAliases;
    }

    public static class ItemHolder extends RecyclerView.ViewHolder {

        protected TextView mText;

        public ItemHolder(View view) {
            super(view);
            mText = (TextView) view.findViewById(R.id.text);
        }

        public void bind(CommandAliasManager.CommandAlias alias) {
            mText.setText(alias.name);
        }

    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {

        protected TextView mText;

        public HeaderHolder(View view) {
            super(view);
            mText = (TextView) view.findViewById(R.id.title);
        }

        public void bind(int textId) {
            mText.setText(textId);
        }

    }

    public class ItemDecoration extends AdvancedDividerItemDecoration {

        public ItemDecoration(Context context) {
            super(context);
        }

        @Override
        public boolean hasDivider(RecyclerView parent, View view) {
            return mRecyclerView.getChildViewHolder(view).getItemViewType() != TYPE_HEADER;
        }

    }

}
