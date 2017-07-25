package io.mrarm.irc.chat;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import io.mrarm.chatlib.dto.HostInfoMessageInfo;
import io.mrarm.chatlib.dto.StatusMessageInfo;
import io.mrarm.chatlib.dto.StatusMessageList;
import io.mrarm.irc.R;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.MessageBuilder;

public class ServerStatusMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_EXPANDABLE_MESSAGE = 1;

    private StatusMessageList mMessages;
    private Set<StatusMessageInfo> mExpandedMessages;
    private Typeface mTypeface;
    private int mFontSize;

    public ServerStatusMessagesAdapter(StatusMessageList messages) {
        setMessages(messages);
    }

    public void setMessageFont(Typeface typeface, int textSize) {
        mTypeface = typeface;
        mFontSize = textSize;
    }

    public void setMessages(StatusMessageList messages) {
        this.mMessages = messages;
        mExpandedMessages = new HashSet<>();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_MESSAGE) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_message, viewGroup, false);
            return new MessageHolder(view);
        } else if (viewType == TYPE_EXPANDABLE_MESSAGE) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_expandable_message, viewGroup, false);
            return new ExpandableMessageHolder(view, this);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        if (viewType == TYPE_MESSAGE) {
            ((MessageHolder) holder).bind(mMessages.getMessages().get(position));
        } else if (viewType == TYPE_EXPANDABLE_MESSAGE) {
            ((ExpandableMessageHolder) holder).bind(position,
                    mMessages.getMessages().get(position));
        }
    }

    private void toggleExpandItem(int position) {
        StatusMessageInfo info = mMessages.getMessages().get(position);
        if (mExpandedMessages.contains(info))
            mExpandedMessages.remove(info);
        else
            mExpandedMessages.add(info);
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return mMessages.getMessages().size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mMessages.getMessages().get(position).getType() == StatusMessageInfo.MessageType.MOTD)
            return TYPE_EXPANDABLE_MESSAGE;
        return TYPE_MESSAGE;
    }

    public class MessageHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public MessageHolder(View v) {
            super(v);
            mText = (TextView) v.findViewById(R.id.chat_message);
            mText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        public void bind(StatusMessageInfo message) {
            if (mTypeface != null)
                mText.setTypeface(mTypeface);
            if (mFontSize != -1)
                mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);

            Context context = mText.getContext();
            if (message.getType() == StatusMessageInfo.MessageType.DISCONNECT_WARNING) {
                mText.setText(MessageBuilder.getInstance(context).buildDisconnectWarning(message.getDate()));
                return;
            }
            CharSequence text;
            if (message.getType() == StatusMessageInfo.MessageType.HOST_INFO) {
                HostInfoMessageInfo hostInfo = (HostInfoMessageInfo) message;
                SpannableString str = new SpannableString(context.getString(
                        R.string.message_host_info, hostInfo.getServerName(), hostInfo.getVersion(),
                        hostInfo.getUserModes(), hostInfo.getChannelModes()));
                str.setSpan(new ForegroundColorSpan(IRCColorUtils.getStatusTextColor(context)),
                        0, str.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                text = str;
            } else {
                text = IRCColorUtils.getFormattedString(context, message.getMessage());
            }
            mText.setText(MessageBuilder.getInstance(context).buildStatusMessage(message, text));
        }

    }

    public class ExpandableMessageHolder extends RecyclerView.ViewHolder {

        private ServerStatusMessagesAdapter mAdapter;
        private TextView mText;
        private TextView mExpandedText;
        private ImageView mExpandIcon;
        private int mPosition;

        public ExpandableMessageHolder(View v, ServerStatusMessagesAdapter adapter) {
            super(v);
            mAdapter = adapter;
            mText = (TextView) v.findViewById(R.id.chat_message);
            mExpandedText = (TextView) v.findViewById(R.id.chat_expanded_message);
            mExpandIcon = (ImageView) v.findViewById(R.id.expand_icon);
            //setExpanded(true);

            mExpandedText.setTypeface(Typeface.MONOSPACE);

            v.setOnClickListener((View view) -> {
                mAdapter.toggleExpandItem(mPosition);
            });
        }

        public void bind(int pos, StatusMessageInfo message) {
            if (mTypeface != null)
                mText.setTypeface(mTypeface);
            if (mFontSize != -1) {
                mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);
                mExpandedText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);
            }

            this.mPosition = pos;

            SpannableString str = new SpannableString(
                    mText.getContext().getString(R.string.message_motd));
            str.setSpan(new ForegroundColorSpan(
                    mText.getContext().getResources().getColor(R.color.motdColor)),
                    0, str.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            mText.setText(MessageBuilder.getInstance(mText.getContext()).buildStatusMessage(message, str));

            boolean expanded = mAdapter.mExpandedMessages.contains(message);

            mExpandedText.setVisibility(expanded ? View.VISIBLE : View.GONE);
            mExpandIcon.setRotation(expanded ? 180.f : 0.f);

            if (!expanded)
                return;
            ColoredTextBuilder builder2 = new ColoredTextBuilder();
            IRCColorUtils.appendFormattedString(mText.getContext(), builder2, message.getMessage());
            mExpandedText.setText(builder2.getSpannable());
        }

    }

}
