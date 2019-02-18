package io.mrarm.irc.newui;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.irc.NotificationManager;
import io.mrarm.irc.R;
import io.mrarm.irc.util.AlignToPointSpan;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.UiThreadHelper;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.BaseHolder<?>>
        implements MessagesData.Listener {

    private final MessagesData mData;

    public MessagesAdapter(MessagesData data) {
        mData = data;
        mData.setListener(this);
    }


    @NonNull
    @Override
    public MessagesAdapter.BaseHolder<?> onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_message, parent, false);
        return new MessageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesAdapter.BaseHolder<?> holder, int position) {
        ((MessageHolder) holder).bind((MessagesData.MessageItem) mData.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull MessagesAdapter.BaseHolder<?> holder) {
        holder.unbind();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    @Override
    public void onReloaded() {
        UiThreadHelper.runOnUiThread(this::notifyDataSetChanged);
    }

    @Override
    public void onItemsAdded(int pos, int count) {
        UiThreadHelper.runOnUiThread(() -> {
            if (count > 1)
                notifyItemRangeInserted(pos, count);
            else
                notifyItemInserted(pos);
        });
    }

    public abstract static class BaseHolder<T extends MessagesData.Item>
            extends RecyclerView.ViewHolder {

        public BaseHolder(@NonNull View itemView) {
            super(itemView);
        }

        public abstract void bind(T item);

        public void unbind() {
        }

    }

    public class MessageHolder extends BaseHolder<MessagesData.MessageItem> {

        private TextView mText;

        public MessageHolder(@NonNull View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.chat_message);
            mText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        private CharSequence buildMessage(MessagesData.MessageItem item) {
            MessageBuilder builder = MessageBuilder.getInstance(mText.getContext());
            if (NotificationManager.getInstance().shouldMessageUseMentionFormatting(
                    mData.getConnection(), mData.getChannel(), item.getMessage()))
                return builder.buildMessageWithMention(item.getMessage());
            else
                return builder.buildMessage(item.getMessage());
        }

        @Override
        public void bind(MessagesData.MessageItem item) {
            mText.setText(AlignToPointSpan.apply(mText, buildMessage(item)));
        }

    }


}
