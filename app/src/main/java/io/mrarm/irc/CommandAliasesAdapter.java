package io.mrarm.irc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;
import io.mrarm.irc.util.AdvancedDividerItemDecoration;
import io.mrarm.irc.util.StyledAttributesHelper;

public class CommandAliasesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_USER_ITEM = 2;

    private CommandAliasManager mManager;
    private RecyclerView mRecyclerView;
    private int mSecondaryItemTextColor;

    public CommandAliasesAdapter(Context context) {
        mManager = CommandAliasManager.getInstance(context);

        mSecondaryItemTextColor = StyledAttributesHelper.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);
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
            return new UserItemHolder(view, mSecondaryItemTextColor, this);
        return new ItemHolder(view, mSecondaryItemTextColor);
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
        private int mSecondaryColor;
        protected CommandAliasManager.CommandAlias mAlias;

        public ItemHolder(View view, int secondaryColor) {
            super(view);
            mText = view.findViewById(R.id.text);
            mSecondaryColor = secondaryColor;
            view.setOnClickListener((View v) -> {
                startEditActivity();
            });
        }

        public void bind(CommandAliasManager.CommandAlias alias) {
            if (alias.syntax == null)
                return;
            SpannableString str = new SpannableString(alias.name + " " + alias.syntax);
            str.setSpan(new ForegroundColorSpan(mSecondaryColor), alias.name.length(), str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mText.setText(str);
            mAlias = alias;
        }

        protected void startEditActivity() {
            Intent intent = new Intent(mText.getContext(), EditCommandAliasActivity.class);
            intent.putExtra(EditCommandAliasActivity.ARG_ALIAS_NAME, mAlias.name);
            intent.putExtra(EditCommandAliasActivity.ARG_ALIAS_SYNTAX, mAlias.syntax);
            mText.getContext().startActivity(intent);
        }

    }

    public static class UserItemHolder extends ItemHolder {

        public UserItemHolder(View view, int secondaryColor, CommandAliasesAdapter adapter) {
            super(view, secondaryColor);
            view.setOnLongClickListener((View v) -> {
                Context ctx = v.getContext();
                MenuBottomSheetDialog dialog = new MenuBottomSheetDialog(ctx);
                dialog.addItem(R.string.action_edit, R.drawable.ic_edit, (MenuBottomSheetDialog.Item item) -> {
                    startEditActivity();
                    return true;
                });
                dialog.addItem(R.string.action_delete, R.drawable.ic_delete, (MenuBottomSheetDialog.Item item) -> {
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
                    return true;
                });
                dialog.show();
                return true;
            });
        }

    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {

        protected TextView mText;

        public HeaderHolder(View view) {
            super(view);
            mText = view.findViewById(R.id.title);
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
