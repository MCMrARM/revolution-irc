package io.mrarm.irc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.util.AdvancedDividerItemDecoration;

public class CommandAliasesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_USER_ITEM = 2;

    private CommandAliasManager mManager;
    private RecyclerView mRecyclerView;
    private int mDefaultItemTextColor;

    public CommandAliasesAdapter(Context context) {
        mManager = CommandAliasManager.getInstance(context);

        TypedArray ta = context.getTheme().obtainStyledAttributes(R.style.AppTheme,
                new int[] { android.R.attr.textColorSecondary });
        mDefaultItemTextColor = ta.getColor(0, Color.BLACK);
        ta.recycle();
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
        View view = inflater.inflate(R.layout.settings_command_alias_item, viewGroup, false);
        if (type == TYPE_USER_ITEM)
            return new UserItemHolder(view, this);
        return new DefaultItemHolder(view, mDefaultItemTextColor);
    }

    @Override
    public int getItemViewType(int position) {
        int userAliases = mManager.getUserAliases().size();
        if (position == 0 || (userAliases > 0 && position == userAliases + 1))
            return TYPE_HEADER;
        if (userAliases > 0 && position < userAliases + 1)
            return TYPE_USER_ITEM;
        return TYPE_ITEM;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder itemHolder, int pos) {
        int type = itemHolder.getItemViewType();
        int userAliases = mManager.getUserAliases().size();
        if (type == TYPE_HEADER) {
            ((HeaderHolder) itemHolder).bind(pos > 0 || userAliases == 0 ? R.string.value_default : R.string.value_custom);
        } else {
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

    public static class DefaultItemHolder extends ItemHolder {

        public DefaultItemHolder(View view, int color) {
            super(view);
            mText.setTextColor(color);
        }

    }

    public static class UserItemHolder extends ItemHolder {

        public UserItemHolder(View view, CommandAliasesAdapter adapter) {
            super(view);
            view.setOnClickListener((View v) -> {
                startEditActivity();
            });
            view.setOnLongClickListener((View v) -> {
                Context ctx = v.getContext();
                new AlertDialog.Builder(ctx)
                        .setTitle(mText.getText())
                        .setItems(new CharSequence[] {
                                ctx.getString(R.string.action_edit),
                                ctx.getString(R.string.action_delete)
                        }, (DialogInterface di, int which) -> {
                            if (which == 0) {
                                startEditActivity();
                            } else if (which == 1) {
                                new AlertDialog.Builder(ctx)
                                        .setTitle(R.string.action_delete_confirm_title)
                                        .setMessage(ctx.getString(R.string.action_delete_confirm_body, mText.getText()))
                                        .setPositiveButton(R.string.action_delete, (DialogInterface di2, int which2) -> {
                                            adapter.mManager.getUserAliases().remove(getAdapterPosition() - 1);
                                            adapter.mManager.saveUserSettings();
                                            adapter.notifyItemRemoved(getAdapterPosition());
                                            if (adapter.mManager.getUserAliases().size() == 0)
                                                adapter.notifyItemRemoved(0); // header
                                        })
                                        .setNegativeButton(R.string.action_cancel, null)
                                        .show();
                            }
                        })
                        .show();
                return true;
            });
        }

        private void startEditActivity() {
            Intent intent = new Intent(mText.getContext(), EditCommandAliasActivity.class);
            intent.putExtra(EditCommandAliasActivity.ARG_ALIAS_NAME, mText.getText().toString());
            mText.getContext().startActivity(intent);
        }

        @Override
        public void bind(CommandAliasManager.CommandAlias alias) {
            super.bind(alias);
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
