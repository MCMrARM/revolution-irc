package io.mrarm.irc.chat;

import android.app.Dialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.dialog.UserBottomSheetDialog;

public class ChannelInfoAdapter extends RecyclerView.Adapter {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TOPIC = 1;
    public static final int TYPE_MEMBER = 2;

    private ServerConnectionInfo mConnection;
    private String mTopic;
    private List<NickWithPrefix> mMembers;

    public ChannelInfoAdapter() {
    }

    public void setData(ServerConnectionInfo connection, String topic,
                        List<NickWithPrefix> members) {
        mConnection = connection;
        mTopic = topic;
        mMembers = members;
        notifyDataSetChanged();
    }

    public List<NickWithPrefix> getMembers() {
        return mMembers;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_HEADER) {
            // TODO:
            return null;
        } else if (viewType == TYPE_TOPIC) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_topic, viewGroup, false);
            return new TextHolder(view);
        } else { // TYPE_MEMBER
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_member, viewGroup, false);
            return new MemberHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int type = holder.getItemViewType();
        if (type == TYPE_TOPIC)
            ((TextHolder) holder).bind(mTopic);
        else if (type == TYPE_MEMBER)
            ((MemberHolder) holder).bind(mConnection, mMembers.get(position - 1));
    }

    @Override
    public int getItemCount() {
        return 1 + (mMembers != null ? mMembers.size() : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_TOPIC;
        return TYPE_MEMBER;
    }

    public static class TextHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        public TextHolder(View view) {
            super(view);
            textView = (TextView) view;
        }

        public void bind(String title) {
            textView.setText(title);
        }

    }

    public static class MemberHolder extends RecyclerView.ViewHolder {

        private ServerConnectionInfo mConnection;
        private TextView mText;

        public MemberHolder(View v) {
            super(v);
            mText = v.findViewById(R.id.chat_member);
            v.setOnClickListener((View view) -> {
                UserBottomSheetDialog dialog = new UserBottomSheetDialog(view.getContext());
                dialog.setConnection(mConnection);
                dialog.requestData((String) mText.getTag(), mConnection.getApiInstance());
                Dialog d = dialog.show();
                if (view.getContext() instanceof MainActivity)
                    ((MainActivity) view.getContext()).setFragmentDialog(d);
            });
        }

        public void bind(ServerConnectionInfo connection, NickWithPrefix nickWithPrefix) {
            mConnection = connection;
            bindText(mText, nickWithPrefix);
            mText.setTag(nickWithPrefix.getNick());
        }

        public static void bindText(TextView text, NickWithPrefix nickWithPrefix) {
            char prefix = ' ';
            if (nickWithPrefix.getNickPrefixes() != null &&
                    nickWithPrefix.getNickPrefixes().length() > 0)
                prefix = nickWithPrefix.getNickPrefixes().get(0);
            if (prefix == '~')
                text.setTextColor(text.getContext().getResources().getColor(R.color.memberOwner));
            else if (prefix == '&')
                text.setTextColor(text.getContext().getResources().getColor(R.color.memberAdmin));
            else if (prefix == '@')
                text.setTextColor(text.getContext().getResources().getColor(R.color.memberOp));
            else if (prefix == '%')
                text.setTextColor(text.getContext().getResources().getColor(R.color.memberHalfOp));
            else if (prefix == '+')
                text.setTextColor(text.getContext().getResources().getColor(R.color.memberVoice));
            else
                text.setTextColor(text.getContext().getResources().getColor(R.color.memberNormal));
            if (prefix != ' ')
                text.setText(prefix + nickWithPrefix.getNick());
            else
                text.setText(nickWithPrefix.getNick());
        }

    }

}
