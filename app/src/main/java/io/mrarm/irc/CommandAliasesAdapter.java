package io.mrarm.irc;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CommandAliasesAdapter extends RecyclerView.Adapter<CommandAliasesAdapter.ItemHolder> {

    private CommandAliasManager mManager;

    public CommandAliasesAdapter(Context context) {
        mManager = CommandAliasManager.getInstance(context);
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        return new ItemHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.settings_command_alias_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ItemHolder itemHolder, int pos) {
        itemHolder.bind(mManager.getUserAliases().get(pos).name);
    }

    @Override
    public int getItemCount() {
        return mManager.getUserAliases().size();
    }

    public static class ItemHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public ItemHolder(View view) {
            super(view);
            mText = (TextView) view.findViewById(R.id.text);
        }

        public void bind(String text) {
            mText.setText(text);
        }

    }

}
