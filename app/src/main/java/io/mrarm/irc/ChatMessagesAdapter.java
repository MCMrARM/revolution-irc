package io.mrarm.irc;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.NickChangeMessageInfo;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.LongPressSelectTouchListener;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements LongPressSelectTouchListener.Listener {

    private static final int TYPE_MESSAGE = 0;

    private ChatMessagesFragment mFragment;
    private List<MessageInfo> mMessages;
    private LongPressSelectTouchListener mSelectListener;
    private Set<Integer> mSelectedItems = new TreeSet<>();

    private Drawable mItemBackground;
    private Drawable mSelectedItemBackground;

    public ChatMessagesAdapter(ChatMessagesFragment fragment, List<MessageInfo> messages) {
        mFragment = fragment;
        TypedArray ta = fragment.getContext().obtainStyledAttributes(new int[] { R.attr.selectableItemBackground, R.attr.colorControlHighlight });
        mItemBackground = ta.getDrawable(0);
        int color = ta.getColor(1, 0);
        //color = ColorUtils.setAlphaComponent(color, Color.alpha(color) / 2);
        mSelectedItemBackground = new ColorDrawable(color);
        ta.recycle();

        setMessages(messages);
    }

    public void setMessages(List<MessageInfo> messages) {
        this.mMessages = messages;
        notifyDataSetChanged();
    }

    public void addMessagesToTop(List<MessageInfo> messages) {
        mMessages.addAll(0, messages);
        notifyItemRangeInserted(0, messages.size());
    }

    public boolean hasMessages() {
        return mMessages != null && mMessages.size() > 0;
    }

    public void setSelectListener(LongPressSelectTouchListener selectListener) {
        mSelectListener = selectListener;
        selectListener.setListener(this);
    }

    public Set<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public void clearSelection(RecyclerView recyclerView) {
        for (int item : mSelectedItems) {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(item);
            if (viewHolder == null)
                continue;
            ((MessageHolder) viewHolder).setSelected(false, false);
        }
        mSelectedItems.clear();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_MESSAGE) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_message, viewGroup, false);
            return new MessageHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        if (viewType == TYPE_MESSAGE) {
            ((MessageHolder) holder).bind(mMessages.get(position), mSelectedItems.contains(position) || mSelectListener.isElementHightlighted(position));
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_MESSAGE;
    }

    @Override
    public void onElementSelected(RecyclerView recyclerView, int adapterPos) {
        mSelectedItems.add(adapterPos);
        onElementHighlighted(recyclerView, adapterPos, true);
    }

    @Override
    public void onElementHighlighted(RecyclerView recyclerView, int adapterPos, boolean highlight) {
        MessageHolder holder = ((MessageHolder) recyclerView.findViewHolderForAdapterPosition(adapterPos));
        if (holder != null)
            holder.setSelected(highlight || mSelectedItems.contains(adapterPos), false);
    }

    public class MessageHolder extends RecyclerView.ViewHolder {

        private TextView mText;
        private boolean mSelected = false;

        public MessageHolder(View v) {
            super(v);
            mText = (TextView) v.findViewById(R.id.chat_message);
            v.setOnClickListener((View view) -> {
                if (mSelectedItems.size() > 0)
                    setSelected(!isSelected(), true);
            });
            v.setOnLongClickListener((View view) -> {
                setSelected(true, true);
                mSelectListener.startSelectMode(getAdapterPosition());
                mFragment.showMessagesActionMenu();
                return true;
            });
            mText.setBackgroundDrawable(mItemBackground);
        }

        public boolean isSelected() {
            return mSelected;
        }

        public void setSelected(boolean selected, boolean updateAdapter) {
            if (mSelected == selected)
                return;
            mSelected = selected;
            if (updateAdapter) {
                if (selected)
                    mSelectedItems.add(getAdapterPosition());
                else
                    mSelectedItems.remove(getAdapterPosition());
            }
            mText.setBackgroundDrawable(selected ? mSelectedItemBackground : mItemBackground);
        }

        public void bind(MessageInfo message, boolean selected) {
            setSelected(selected, false);
            mText.setText(buildMessage(mText.getContext(), message));
        }

    }

    private static SimpleDateFormat messageTimeFormat = new SimpleDateFormat("[HH:mm] ",
            Locale.getDefault());

    public static void appendTimestamp(Context context, ColoredTextBuilder builder, Date date) {
        builder.append(messageTimeFormat.format(date), new ForegroundColorSpan(context.getResources().getColor(R.color.messageTimestamp)));
    }

    public static CharSequence buildDisconnectWarning(Context context, Date date) {
        ColoredTextBuilder builder = new ColoredTextBuilder();
        appendTimestamp(context, builder, date);
        builder.append("Disconnected", new ForegroundColorSpan(context.getResources().getColor(R.color.messageDisconnected)));
        return builder.getSpannable();
    }

    public static CharSequence buildMessage(Context context, MessageInfo message) {
        String senderNick = message.getSender() == null ? null : message.getSender().getNick();
        int nickColor = senderNick == null ? 0 : IRCColorUtils.getNickColor(context, senderNick);
        switch (message.getType()) {
            case NORMAL: {
                ColoredTextBuilder builder = new ColoredTextBuilder();
                appendTimestamp(context, builder, message.getDate());
                builder.append(message.getSender().getNick() + ":", new ForegroundColorSpan(nickColor));
                builder.append(" ");
                IRCColorUtils.appendFormattedString(context, builder, message.getMessage());
                return builder.getSpannable();
            }
            case ME: {
                ColoredTextBuilder builder = new ColoredTextBuilder();
                appendTimestamp(context, builder, message.getDate());
                builder.setSpan(new StyleSpan(Typeface.ITALIC));
                builder.append("* ", new ForegroundColorSpan(IRCColorUtils.getStatusTextColor(context)));
                builder.append(message.getSender().getNick(), new ForegroundColorSpan(nickColor));
                builder.append(" ");
                IRCColorUtils.appendFormattedString(context, builder, message.getMessage());
                return builder.getSpannable();
            }
            case JOIN: {
                ColoredTextBuilder builder = new ColoredTextBuilder();
                appendTimestamp(context, builder, message.getDate());
                builder.appendWithFlags("* ", Spanned.SPAN_EXCLUSIVE_INCLUSIVE, new ForegroundColorSpan(IRCColorUtils.getStatusTextColor(context)), new StyleSpan(Typeface.ITALIC));
                builder.append(message.getSender().getNick(), new ForegroundColorSpan(nickColor));
                builder.append(" has joined", new ForegroundColorSpan(IRCColorUtils.getStatusTextColor(context))); // TODO: Translate this
                return builder.getSpannable();
            }
            case NICK_CHANGE: {
                String newNick = ((NickChangeMessageInfo) message).getNewNick();
                int newNickColor = IRCColorUtils.getNickColor(context, newNick);

                ColoredTextBuilder builder = new ColoredTextBuilder();
                appendTimestamp(context, builder, message.getDate());
                builder.appendWithFlags("* ", Spanned.SPAN_EXCLUSIVE_INCLUSIVE, new ForegroundColorSpan(IRCColorUtils.getStatusTextColor(context)), new StyleSpan(Typeface.ITALIC));
                builder.append(message.getSender().getNick(), new ForegroundColorSpan(nickColor));
                builder.append(" is now known as ", new ForegroundColorSpan(IRCColorUtils.getStatusTextColor(context))); // TODO: Translate this
                builder.append(newNick, new ForegroundColorSpan(newNickColor));
                return builder.getSpannable();
            }
            case DISCONNECT_WARNING:
                return buildDisconnectWarning(context, message.getDate());
        }
        return null;
    }

}
