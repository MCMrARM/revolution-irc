package io.mrarm.irc.newui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.util.SpannableStringHelper;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ChatListAdapter extends RecyclerView.Adapter {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mNotifyDataSetPosted = false;

    private final ChatListData mData;

    private int mTextColorPrimary;
    private int mTextColorSecondary;

    private CallbackInterface mInterface;

    public ChatListAdapter(Context ctx, ChatListData data) {
        mData = data;

        mTextColorPrimary =
                StyledAttributesHelper.getColor(ctx, android.R.attr.textColorPrimary, 0);
        mTextColorSecondary =
                StyledAttributesHelper.getColor(ctx, android.R.attr.textColorSecondary, 0);

        data.addListener(() -> {
            if (mNotifyDataSetPosted)
                return;
            mNotifyDataSetPosted = true;
            mHandler.post(() -> {
                notifyDataSetChanged();
                mNotifyDataSetPosted = false;
            });
        });
    }

    public void setCallbackInterface(CallbackInterface callbackInterface) {
        mInterface = callbackInterface;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_chat_list_entry, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ItemHolder) holder).bind(mData.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        ((ItemHolder) holder).unbind();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public final class ItemHolder extends RecyclerView.ViewHolder {

        private Runnable mUpdateCb = this::updateData;
        private ChatListData.Item mItem;

        private TextView mChannel;
        private TextView mMessageDate;
        private TextView mMessageText;
        private TextView mUnreadCounter;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            mChannel = itemView.findViewById(R.id.chat_name);
            mMessageDate = itemView.findViewById(R.id.chat_time);
            mMessageText = itemView.findViewById(R.id.chat_message);
            mUnreadCounter = itemView.findViewById(R.id.chat_counter);

            itemView.setOnClickListener((v) ->
                    mInterface.onChatOpened(mItem.getConnection(), mItem.getChannel()));
        }

        public void bind(ChatListData.Item item) {
            mItem = item;
            item.observe(mUpdateCb);

            mChannel.setText(SpannableStringHelper.format("%S%s%S (%s)",
                    new ForegroundColorSpan(mTextColorPrimary), item.getChannel(),
                    new ForegroundColorSpan(mTextColorSecondary), item.getConnectionName()));
            updateData();
        }

        public void unbind() {
            mItem.stopObserving(mUpdateCb);
            mItem = null;
        }

        private void updateData() {
            mMessageDate.setText(formatDate(mChannel.getContext(), mItem.getLastMessageDate()));
            mMessageText.setText(mItem.getLastMessageText());
            int cnt = mItem.getUnreadMessageCount();
            mUnreadCounter.setVisibility(cnt > 0 ? View.VISIBLE : View.GONE);
            if (cnt > 0)
                mUnreadCounter.setText(String.valueOf(cnt));
        }

    }

    private static String formatDate(Context context, Date date) {
        if (date == null)
            return null;
        if (System.currentTimeMillis() - date.getTime() < 1000 * 60 * 2) // newer than 2 min
            return context.getString(R.string.time_now);
        if (DateUtils.isToday(date.getTime()))
            return DateUtils.formatDateTime(context, date.getTime(), DateUtils.FORMAT_SHOW_TIME);
        return DateUtils.formatDateTime(context, date.getTime(), DateUtils.FORMAT_SHOW_DATE);

    }

    public interface CallbackInterface {

        void onChatOpened(ServerConnectionInfo server, String channel);

    }

}
