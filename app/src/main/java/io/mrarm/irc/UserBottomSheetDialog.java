package io.mrarm.irc;

import android.content.Context;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.dto.WhoisInfo;
import io.mrarm.irc.util.AdvancedDividerItemDecoration;
import io.mrarm.irc.util.StatusBarColorBottomSheetDialog;

public class UserBottomSheetDialog {

    private Context mContext;
    private StatusBarColorBottomSheetDialog mDialog;
    private RecyclerView mRecyclerView;
    private ItemAdapter mAdapter;

    private String mNick;
    private String mUser;
    private String mRealName;
    private boolean mAway;
    private List<Pair<String, String>> mEntries = new ArrayList<>();

    public UserBottomSheetDialog(Context context) {
        mContext = context;
    }

    public void requestData(String nick, ChatApi connection) {
        setUser(nick, null, null, false);
        connection.sendWhois(nick, (WhoisInfo info) -> {
            mRecyclerView.post(() -> {
                setData(info);
            });
        }, null);
    }

    public void setData(WhoisInfo info) {
        mEntries.clear();
        setUser(info.getNick(), info.getUser(), info.getRealName(), (info.getAwayMessage() != null));
        if (info.getAwayMessage() != null)
            addEntry(R.string.user_away, info.getAwayMessage());
        addEntry(R.string.user_hostname, info.getHost());
        if (info.getServer() != null)
            addEntry(R.string.user_server, mContext.getString(R.string.user_server_format, info.getServer(), info.getServerInfo()));
        if (info.getChannels() != null) {
            StringBuilder b = new StringBuilder();
            for (WhoisInfo.ChannelWithNickPrefixes channel : info.getChannels()) {
                if (b.length() > 0)
                    b.append(mContext.getString(R.string.text_comma));
                if (channel.getPrefixes() != null)
                    b.append(channel.getPrefixes());
                b.append(channel.getChannel());
            }
            addEntry(R.string.user_channels, b.toString());
        }
        if (info.getIdleSeconds() > 0)
            addEntry(R.string.user_idle, mContext.getResources().getQuantityString(R.plurals.time_seconds, info.getIdleSeconds(), info.getIdleSeconds()));
        if (info.getLoggedInAsAccount() != null)
            addEntry(R.string.user_account, info.getLoggedInAsAccount());
        if (info.isOperator())
            addEntry(R.string.user_server_op, mContext.getString(R.string.user_server_op_desc));
        if (info.isConnectionSecure())
            addEntry(R.string.user_secure, mContext.getString(R.string.user_secure_desc));
    }

    public void setUser(String nick, String user, String realName, boolean away) {
        mNick = nick;
        mUser = user;
        mRealName = realName;
        mAway = away;
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
        updateDialogStatusBarColor();
    }

    public void addEntry(int titleId, String value) {
        addEntry(mContext.getString(titleId), value);
    }

    public void addEntry(String title, String value) {
        mEntries.add(new Pair<>(title, value));
        if (mAdapter != null)
            mAdapter.notifyItemInserted(mEntries.size() - 1 + 1);
    }

    private void create() {
        mRecyclerView = new RecyclerView(mContext);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.addItemDecoration(new MyItemDecorator(mContext));
        mAdapter = new ItemAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mDialog = new StatusBarColorBottomSheetDialog(mContext);
        mDialog.setContentView(mRecyclerView);
        mDialog.getWindow().getDecorView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mRecyclerView.setMinimumHeight(bottom-top);
            }
        });
        updateDialogStatusBarColor();
    }

    private void updateDialogStatusBarColor() {
        if (mDialog == null)
            return;
        if (mAway)
            mDialog.setStatusBarColor(mContext.getResources().getColor(R.color.userAwayColorPrimaryDark));
        else
            mDialog.setStatusBarColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
    }

    public void show() {
        if (mDialog == null)
            create();
        mDialog.show();
    }

    private class ItemAdapter extends RecyclerView.Adapter {

        public static final int ITEM_HEADER = 0;
        public static final int ITEM_ENTRY = 1;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_bottom_user_header, parent, false);
                return new HeaderHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.dialog_bottom_user_entry, parent, false);
                return new EntryHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0)
                ((HeaderHolder) holder).bind();
            else
                ((EntryHolder) holder).bind(mEntries.get(position - 1));
        }

        @Override
        public int getItemCount() {
            return mEntries.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return ITEM_HEADER;
            return ITEM_ENTRY;
        }

        private class HeaderHolder extends RecyclerView.ViewHolder {
            private View mContainer;
            private TextView mName;
            private TextView mNick;
            private TextView mUser;

            public HeaderHolder(View itemView) {
                super(itemView);
                mContainer = itemView;
                mName = (TextView) itemView.findViewById(R.id.name);
                mNick = (TextView) itemView.findViewById(R.id.nick);
                mUser = (TextView) itemView.findViewById(R.id.user);
            }

            public void bind() {
                if (mAway) {
                    mName.setText(mContext.getString(R.string.user_title_away, mRealName));
                    mContainer.setBackgroundResource(R.color.userAwayColorPrimary);
                } else {
                    mName.setText(UserBottomSheetDialog.this.mRealName);
                    mContainer.setBackgroundResource(R.color.colorPrimary);
                }
                mNick.setText(UserBottomSheetDialog.this.mNick);
                mUser.setText(UserBottomSheetDialog.this.mUser);
            }
        }

        private class EntryHolder extends RecyclerView.ViewHolder {
            private TextView mTitle;
            private TextView mValue;

            public EntryHolder(View itemView) {
                super(itemView);
                mTitle = (TextView) itemView.findViewById(R.id.title);
                mValue = (TextView) itemView.findViewById(R.id.value);
            }

            public void bind(Pair<String, String> entry) {
                mTitle.setText(entry.first);
                mValue.setText(entry.second);
            }
        }

    }

    private static final class MyItemDecorator extends AdvancedDividerItemDecoration {

        public MyItemDecorator(Context context) {
            super(context);
        }

        @Override
        public boolean hasDivider(RecyclerView parent, View view) {
            return parent.getChildAdapterPosition(view) != 0;
        }

    }

}
