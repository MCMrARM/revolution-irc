package io.mrarm.irc.chat;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import androidx.recyclerview.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import io.mrarm.chatlib.dto.StatusMessageInfo;
import io.mrarm.chatlib.dto.StatusMessageList;
import io.mrarm.chatlib.dto.WhoisStatusMessageInfo;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.dialog.UserBottomSheetDialog;
import io.mrarm.irc.util.AlignToPointSpan;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.MessageBuilder;

public class ServerStatusMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_EXPANDABLE_MESSAGE = 1;

    private ServerConnectionInfo mConnection;
    private StatusMessageList mMessages;
    private Set<StatusMessageInfo> mExpandedMessages;
    private Typeface mTypeface;
    private int mFontSize;

    public ServerStatusMessagesAdapter(ServerConnectionInfo connection,
                                       StatusMessageList messages) {
        mConnection = connection;
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
        StatusMessageInfo.MessageType type = mMessages.getMessages().get(position).getType();
        if (type == StatusMessageInfo.MessageType.MOTD ||
                type == StatusMessageInfo.MessageType.WHOIS)
            return TYPE_EXPANDABLE_MESSAGE;
        return TYPE_MESSAGE;
    }

    public class MessageHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public MessageHolder(View v) {
            super(v);
            mText = v.findViewById(R.id.chat_message);
            mText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        public void bind(StatusMessageInfo message) {
            if (mTypeface != null)
                mText.setTypeface(mTypeface);
            if (mFontSize != -1)
                mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);

            Context context = mText.getContext();
            if (message.getType() == StatusMessageInfo.MessageType.DISCONNECT_WARNING) {
                mText.setText(AlignToPointSpan.apply(mText, MessageBuilder.getInstance(context)
                        .buildDisconnectWarning(message.getDate())));
                return;
            }
            mText.setText(AlignToPointSpan.apply(mText,
                    MessageBuilder.getInstance(context).buildStatusMessage(message)));
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
            mText = v.findViewById(R.id.chat_message);
            mExpandedText = v.findViewById(R.id.chat_expanded_message);
            mExpandIcon = v.findViewById(R.id.expand_icon);
            //setExpanded(true);

            mExpandedText.setTypeface(Typeface.MONOSPACE);

            v.setOnClickListener((View view) -> {
                StatusMessageInfo msg = mMessages.getMessages().get(mPosition);
                if (msg instanceof WhoisStatusMessageInfo) {
                    UserBottomSheetDialog dialog = new UserBottomSheetDialog(view.getContext());
                    dialog.setConnection(mConnection);
                    dialog.setData(((WhoisStatusMessageInfo) msg).getWhoisInfo());
                    Dialog d = dialog.show();
                    if (view.getContext() instanceof MainActivity)
                        ((MainActivity) view.getContext()).setFragmentDialog(d);
                } else {
                    mAdapter.toggleExpandItem(mPosition);
                }
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

            mText.setText(AlignToPointSpan.apply(mText,
                    MessageBuilder.getInstance(mText.getContext()).buildStatusMessage(message)));

            boolean expanded = mAdapter.mExpandedMessages.contains(message);

            mExpandedText.setVisibility(expanded ? View.VISIBLE : View.GONE);
            mExpandIcon.setRotation(expanded ? 180.f : 0.f);

            if (!expanded)
                return;
            mExpandedText.setText(IRCColorUtils.getFormattedString(mText.getContext(),
                    message.getMessage()));
        }

    }

}
