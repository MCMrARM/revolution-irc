package io.mrarm.irc;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private HeaderHelper mHeader;

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
            addEntry(R.string.user_idle, formatTime(info.getIdleSeconds()));
        if (info.getLoggedInAsAccount() != null)
            addEntry(R.string.user_account, info.getLoggedInAsAccount());
        if (info.isOperator())
            addEntry(R.string.user_server_op, mContext.getString(R.string.user_server_op_desc));
        if (info.isConnectionSecure())
            addEntry(R.string.user_secure, mContext.getString(R.string.user_secure_desc));
        mAdapter.notifyDataSetChanged();
    }

    private String formatTime(int seconds) {
        if (seconds >= TimeUnit.DAYS.toSeconds(2L)) {
            int days = (int) TimeUnit.SECONDS.toDays(seconds);
            return mContext.getResources().getQuantityString(R.plurals.time_days, days, days);
        }
        if (seconds >= TimeUnit.HOURS.toSeconds(2L)) {
            int days = (int) TimeUnit.SECONDS.toHours(seconds);
            return mContext.getResources().getQuantityString(R.plurals.time_hours, days, days);
        }
        if (seconds >= TimeUnit.MINUTES.toSeconds(2L)) {
            int days = (int) TimeUnit.SECONDS.toMinutes(seconds);
            return mContext.getResources().getQuantityString(R.plurals.time_minutes, days, days);
        }
        return mContext.getResources().getQuantityString(R.plurals.time_seconds, seconds, seconds);
    }

    public void setUser(String nick, String user, String realName, boolean away) {
        mNick = nick;
        mUser = user;
        mRealName = realName;
        mAway = away;
        updateDialogStatusBarColor();
        if (mHeader != null)
            mHeader.bind();
    }

    public void addEntry(int titleId, String value) {
        addEntry(mContext.getString(titleId), value);
    }

    public void addEntry(String title, String value) {
        mEntries.add(new Pair<>(title, value));
        if (mAdapter != null)
            mAdapter.notifyItemInserted(mEntries.size() - 1);
    }

    private void create() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_bottom_user, null);

        mHeader = new HeaderHelper(view.findViewById(R.id.header));
        mHeader.bind();

        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.addItemDecoration(new AdvancedDividerItemDecoration(mContext));
        mRecyclerView.setPadding(0, mHeader.mMaxHeight, 0, 0);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                View v = mRecyclerView.getChildAt(0);
                if (mRecyclerView.getChildAdapterPosition(v) != 0) {
                    mHeader.setScrollY(mHeader.mMaxHeight - mHeader.mMinHeight);
                    return;
                }
                mHeader.setScrollY(mRecyclerView.getPaddingTop() - v.getTop());
            }
        });

        mAdapter = new ItemAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mDialog = new StatusBarColorBottomSheetDialog(mContext);
        mDialog.setContentView(view);
        mDialog.getWindow().getDecorView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                view.setMinimumHeight(bottom - top);
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

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dialog_bottom_user_entry, parent, false);
            return new EntryHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((EntryHolder) holder).bind(mEntries.get(position));
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
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

    private class HeaderHelper {
        private View mContainer;
        private TextView mName;
        private TextView mNick;
        private TextView mUser;
        private int mBottomMargin;
        private int mNameBottomMargin;
        private int mTargetNameBottomMargin;
        private int mMaxHeight;
        private int mMinHeight;
        private int mElevation;

        public HeaderHelper(View itemView) {
            mContainer = itemView;
            mName = (TextView) itemView.findViewById(R.id.name);
            mNick = (TextView) itemView.findViewById(R.id.nick);
            mUser = (TextView) itemView.findViewById(R.id.user);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mNick.getLayoutParams();
            mBottomMargin = params.bottomMargin;
            params = (RelativeLayout.LayoutParams) mName.getLayoutParams();
            mNameBottomMargin = params.bottomMargin;
            mTargetNameBottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, mContext.getResources().getDisplayMetrics());
            params = (RelativeLayout.LayoutParams) mContainer.getLayoutParams();
            mMaxHeight = params.height;

            TypedArray ta = mContext.obtainStyledAttributes(new int[] { R.attr.actionBarSize });
            mMinHeight = ta.getDimensionPixelSize(0, 0);
            ta.recycle();

            mElevation = mContext.getResources().getDimensionPixelSize(R.dimen.abc_action_bar_elevation_material);
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

        public void setScrollY(int y) {
            y = Math.min(y, mMaxHeight - mMinHeight);
            ViewCompat.setElevation(mContainer, y == (mMaxHeight - mMinHeight) ? mElevation : 0);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mNick.getLayoutParams();
            params.bottomMargin = mBottomMargin - y;
            mNick.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) mUser.getLayoutParams();
            params.bottomMargin = mBottomMargin - y;
            mUser.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) mName.getLayoutParams();
            params.bottomMargin = Math.max(mNameBottomMargin - y, mTargetNameBottomMargin);
            mName.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) mContainer.getLayoutParams();
            params.height = mMaxHeight - y;
            mContainer.setLayoutParams(params);
        }
    }

}
