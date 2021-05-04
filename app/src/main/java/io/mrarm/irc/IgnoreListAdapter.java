package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.StyledAttributesHelper;

public class IgnoreListAdapter extends RecyclerView.Adapter<IgnoreListAdapter.ItemHolder> {

    private ServerConfigData mServer;
    private int mTextColorSecondary;
    private int mTextColorNick;
    private int mTextColorUser;
    private int mTextColorHost;

    public IgnoreListAdapter(Context context, ServerConfigData server) {
        mTextColorSecondary = StyledAttributesHelper.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);
        mTextColorNick = context.getResources().getColor(R.color.ignoreEntryNick);
        mTextColorUser = context.getResources().getColor(R.color.ignoreEntryUser);
        mTextColorHost = context.getResources().getColor(R.color.ignoreEntryHost);
        mServer = server;
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.simple_list_item, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        holder.bind(mServer.ignoreList.get(position));
    }

    @Override
    public int getItemCount() {
        return mServer.ignoreList == null ? 0 : mServer.ignoreList.size();
    }

    public class ItemHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public ItemHolder(View itemView) {
            super(itemView);
            mText = (TextView) itemView;
            mText.setOnClickListener((View v) -> {
                startEdit();
            });
            mText.setOnLongClickListener((View v) -> {
                Context context = v.getContext();
                MenuBottomSheetDialog dialog = new MenuBottomSheetDialog(context);
                dialog.addItem(R.string.action_edit, R.drawable.ic_edit, (MenuBottomSheetDialog.Item item) -> {
                    startEdit();
                    return true;
                });
                dialog.addItem(R.string.action_delete, R.drawable.ic_delete, (MenuBottomSheetDialog.Item item) -> {
                    mServer.ignoreList.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());
                    try {
                        ServerConfigManager.getInstance(context).saveServer(mServer);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(context, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
                dialog.show();
                return true;
            });
        }

        private void startEdit() {
            Intent intent = new Intent(mText.getContext(), EditIgnoreEntryActivity.class);
            intent.putExtra(EditIgnoreEntryActivity.ARG_SERVER_UUID, mServer.uuid.toString());
            intent.putExtra(EditIgnoreEntryActivity.ARG_ENTRY_INDEX, getAdapterPosition());
            mText.getContext().startActivity(intent);
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
                builder.append(entry.user, new ForegroundColorSpan(mTextColorUser));

            builder.append("@");
            if (entry.host == null || entry.host.equals("*"))
                builder.append("*", new ForegroundColorSpan(mTextColorSecondary));
            else
                builder.append(entry.host, new ForegroundColorSpan(mTextColorHost));

            if (entry.comment != null) {
                builder.append(" ");
                builder.append(entry.comment, new ForegroundColorSpan(mTextColorSecondary));
            }

            if (entry.mesg != null) {
                if( builder.getSpannable().length() != 0 ) builder.append(" " );
                int len = entry.mesg.length();
                if( len < 10 ) {
                    builder.append( entry.mesg );
                } else {
                    builder.append(entry.mesg.substring(0, 10));
                    builder.append(" ...");
                }
            }
            mText.setText(builder.getSpannable());
        }

    }

}
