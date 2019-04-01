package io.mrarm.irc.newui;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.chatlib.dto.MessageId;
import io.mrarm.irc.NotificationManager;
import io.mrarm.irc.R;
import io.mrarm.irc.chat.ChatSelectTouchListener;
import io.mrarm.irc.util.AlignToPointSpan;
import io.mrarm.irc.util.MessageBuilder;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.BaseHolder<?>>
        implements MessagesData.Listener, MessagesUnreadData.FirstUnreadMessageListener,
        ChatSelectTouchListener.AdapterInterface {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_MESSAGE_WITH_NEW_MESSAGES_MARKER = 1;
    public static final int TYPE_DAY_MARKER = 2;

    private final MessagesData mData;
    private MessageId mFirstUnreadMessageId;
    private long mStableIdOffset = 1000000000;
    private MessageLongPressListener mMessageLongPressListener;

    public MessagesAdapter(MessagesData data) {
        setHasStableIds(true);
        mData = data;
        mData.addListener(this);
    }

    public void setMessageLongPressListener(MessageLongPressListener listener) {
        mMessageLongPressListener = listener;
    }

    public void setUnreadData(MessagesUnreadData data) {
        data.setFirstUnreadMessageListener(this);
        mFirstUnreadMessageId = data.getFirstUnreadMessageId();
    }

    @Override
    public int getItemViewType(int position) {
        MessagesData.Item item = mData.get(position);
        if (item instanceof MessagesData.MessageItem) {
            if (((MessagesData.MessageItem) item).getMessageId().equals(mFirstUnreadMessageId))
                return TYPE_MESSAGE_WITH_NEW_MESSAGES_MARKER;
            return TYPE_MESSAGE;
        }
        if (item instanceof MessagesData.DayMarkerItem)
            return TYPE_DAY_MARKER;
        throw new RuntimeException("Invalid item in MessagesData");
    }

    @NonNull
    @Override
    public MessagesAdapter.BaseHolder<?> onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MESSAGE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_message, parent, false);
            return new MessageHolder(view);
        } else if (viewType == TYPE_MESSAGE_WITH_NEW_MESSAGES_MARKER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_new_messages_marker, parent, false);
            return new MessageHolder(view);
        } else if (viewType == TYPE_DAY_MARKER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_day_marker, parent, false);
            return new DayMarkerHolder(view);
        }
        throw new RuntimeException("Invalid viewType");
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesAdapter.BaseHolder<?> holder, int position) {
        if (holder instanceof MessageHolder)
            ((MessageHolder) holder).bind((MessagesData.MessageItem) mData.get(position));
        else if (holder instanceof DayMarkerHolder)
            ((DayMarkerHolder) holder).bind((MessagesData.DayMarkerItem) mData.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull MessagesAdapter.BaseHolder<?> holder) {
        holder.unbind();
    }

    @Override
    public long getItemId(int position) {
        return position + mStableIdOffset;
    }

    @Override
    public int getItemPosition(long id) {
        return (int) (id - mStableIdOffset);
    }

    @Override
    public CharSequence getTextAt(int position) {
        MessagesData.Item i = mData.get(position);
        if (i instanceof MessagesData.MessageItem)
            return MessageBuilder.getInstance(null)
                    .buildMessage(((MessagesData.MessageItem) i).getMessage());
        return null;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    @Override
    public void onReloaded() {
        notifyDataSetChanged();
    }

    @Override
    public void onItemsAdded(int pos, int count) {
        // NOTE: Currently items can be only added at the start or the end, so this is safe.
        if (pos == 0)
            mStableIdOffset -= count;
        if (count > 1)
            notifyItemRangeInserted(pos, count);
        else
            notifyItemInserted(pos);
    }

    @Override
    public void onItemsRemoved(int pos, int count) {
        if (pos == 0)
            mStableIdOffset += count;
        notifyItemRangeRemoved(pos, count);
    }

    @Override
    public void onFirstUnreadMesssageSet(MessageId m) {
        int oldI = mData.findMessageWithId(mFirstUnreadMessageId);
        mFirstUnreadMessageId = m;
        int newI = mData.findMessageWithId(m);
        if (oldI != -1)
            notifyItemChanged(oldI);
        if (newI != -1)
            notifyItemChanged(newI);
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
            mText.setOnLongClickListener((v) -> {
                if (mMessageLongPressListener != null) {
                    mMessageLongPressListener.onMessageLongPressed(getAdapterPosition());
                    return true;
                }
                return false;
            });
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

    public class DayMarkerHolder extends BaseHolder<MessagesData.DayMarkerItem> {

        private TextView mText;

        public DayMarkerHolder(View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.text);
        }

        public void bind(MessagesData.DayMarkerItem item) {
            mText.setText(item.getMessageText(mText.getContext()));
        }

    }

    public interface MessageLongPressListener {

        void onMessageLongPressed(int pos);

    }


}
