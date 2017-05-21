package io.mrarm.irc;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageList;

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

        public MessageHolder(View v) {
            super(v);
            mText = (TextView) v.findViewById(R.id.chat_message);
        }

        public void bind(MessageInfo message) {
            String senderNick = message.getSenderNick();
            SpannableString string = new SpannableString("<" + senderNick + "> " + message.getMessage());
            //string.setSpan(new StyleSpan(Typeface.BOLD), 0, senderNick.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(0xFF757575), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(0xFF757575), senderNick.length() + 1, senderNick.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(0xFF1976D2), 1, senderNick.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mText.setText(string);
        }

    }

}
