package io.mrarm.irc;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.style.ForegroundColorSpan;
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
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.IRCColorUtils;

public class ServerStatusMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_EXPANDABLE_MESSAGE = 1;

    private StatusMessageList mMessages;
    private Set<StatusMessageInfo> mExpandedMessages;
    private Typeface mTypeface;

    public ServerStatusMessagesAdapter(StatusMessageList messages) {
        setMessages(messages);
    }

    public void setMessageTypeface(Typeface typeface) {
        this.mTypeface = typeface;
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
            if (mTypeface != null)
                mText.setTypeface(mTypeface);
        }

        public void bind(StatusMessageInfo message) {
            Context context = mText.getContext();
            if (message.getType() == StatusMessageInfo.MessageType.DISCONNECT_WARNING) {
                mText.setText(ChatMessagesAdapter.buildDisconnectWarning(context, message.getDate()));
                return;
            }
            ColoredTextBuilder builder = new ColoredTextBuilder();
            ChatMessagesAdapter.appendTimestamp(context, builder, message.getDate());
            int statusColor = IRCColorUtils.getStatusTextColor(context);
            builder.append(message.getSender() + ": ", new ForegroundColorSpan(statusColor));
            if (message.getType() == StatusMessageInfo.MessageType.HOST_INFO) {
                HostInfoMessageInfo hostInfo = (HostInfoMessageInfo) message;
                builder.append("Server name is " + hostInfo.getServerName() + ", running " +
                        hostInfo.getVersion() + ". Supported user modes: " +
                        hostInfo.getUserModes() + ", supported channel modes: " +
                        hostInfo.getChannelModes(), new ForegroundColorSpan(statusColor));
            } else {
                IRCColorUtils.appendFormattedString(context, builder, message.getMessage());
            }
            mText.setText(builder.getSpannable());
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

            if (mTypeface != null)
                mText.setTypeface(mTypeface);
            mExpandedText.setTypeface(Typeface.MONOSPACE);

            v.setOnClickListener((View view) -> {
                mAdapter.toggleExpandItem(mPosition);
            });
        }

        public void bind(int pos, StatusMessageInfo message) {
            this.mPosition = pos;

            ColoredTextBuilder builder = new ColoredTextBuilder();
            ChatMessagesAdapter.appendTimestamp(mText.getContext(), builder, message.getDate());
            builder.append(message.getSender() + ": ", new ForegroundColorSpan(IRCColorUtils.getStatusTextColor(mText.getContext())));
            if (message.getType() == StatusMessageInfo.MessageType.MOTD)
                builder.append("Message of the Day", new ForegroundColorSpan(
                        mText.getContext().getResources().getColor(R.color.motdColor)));
            mText.setText(builder.getSpannable());

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
