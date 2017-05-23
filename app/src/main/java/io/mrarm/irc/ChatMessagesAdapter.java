package io.mrarm.irc;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageList;
import io.mrarm.chatlib.dto.NickChangeMessageInfo;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE = 0;

    private MessageList mMessages;

    public ChatMessagesAdapter(MessageList messages) {
        setMessages(messages);
    }

    public void setMessages(MessageList messages) {
        this.mMessages = messages;
        notifyDataSetChanged();
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
            ((MessageHolder) holder).bind(mMessages.getMessages().get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.getMessages().size();
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_MESSAGE;
    }

    public static class MessageHolder extends RecyclerView.ViewHolder {

        private TextView mText;
        private static SimpleDateFormat messageTimeFormat = new SimpleDateFormat("[HH:mm] ",
                Locale.getDefault());

        public MessageHolder(View v) {
            super(v);
            mText = (TextView) v.findViewById(R.id.chat_message);
        }

        private void appendTimestamp(ColoredTextBuilder builder, MessageInfo message) {
            builder.append(messageTimeFormat.format(message.getDate()),
                    new ForegroundColorSpan(0xFF424242));
        }

        public void bind(MessageInfo message) {
            String senderNick = message.getSender().getNick();
            int nickColor = IRCColorUtils.getNickColor(mText.getContext(), senderNick);
            switch (message.getType()) {
                case NORMAL: {
                    ColoredTextBuilder builder = new ColoredTextBuilder();
                    appendTimestamp(builder, message);
                    builder.append(message.getSender().getNick() + ":", new ForegroundColorSpan(nickColor));
                    builder.append(" ");
                    builder.append(message.getMessage());
                    mText.setText(builder.getSpannable());
                    break;
                }
                case JOIN: {
                    ColoredTextBuilder builder = new ColoredTextBuilder();
                    appendTimestamp(builder, message);
                    builder.appendWithFlags("* ", Spanned.SPAN_EXCLUSIVE_INCLUSIVE, new ForegroundColorSpan(0xFF616161), new StyleSpan(Typeface.ITALIC));
                    builder.append(message.getSender().getNick(), new ForegroundColorSpan(nickColor));
                    builder.append(" has joined", new ForegroundColorSpan(0xFF616161));
                    mText.setText(builder.getSpannable());
                    break;
                }
                case NICK_CHANGE: {
                    String newNick = ((NickChangeMessageInfo) message).getNewNick();
                    int newNickColor = IRCColorUtils.getNickColor(mText.getContext(), newNick);

                    ColoredTextBuilder builder = new ColoredTextBuilder();
                    appendTimestamp(builder, message);
                    builder.appendWithFlags("* ", Spanned.SPAN_EXCLUSIVE_INCLUSIVE, new ForegroundColorSpan(0xFF616161), new StyleSpan(Typeface.ITALIC));
                    builder.append(message.getSender().getNick(), new ForegroundColorSpan(nickColor));
                    builder.append(" is now known as ", new ForegroundColorSpan(0xFF616161));
                    builder.append(newNick, new ForegroundColorSpan(newNickColor));
                    mText.setText(builder.getSpannable());
                    break;
                }
            }
        }

    }

}
