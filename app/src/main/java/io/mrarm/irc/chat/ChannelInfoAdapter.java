package io.mrarm.irc.chat;

import android.app.Dialog;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.dialog.UserBottomSheetDialog;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.LinkHelper;
import io.mrarm.irc.util.SpannableStringHelper;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ChannelInfoAdapter extends RecyclerView.Adapter {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TOPIC = 1;
    public static final int TYPE_MEMBER = 2;

    private ServerConnectionInfo mConnection;
    private String mTopic;
    private String mTopicSetBy;
    private Date mTopicSetOn;
    private List<NickWithPrefix> mMembers;

    public ChannelInfoAdapter() {
    }

    public void setData(ServerConnectionInfo connection, String topic, String topicSetBy,
                        Date topicSetOn, List<NickWithPrefix> members) {
        mConnection = connection;
        mTopic = topic;
        mTopicSetBy = topicSetBy;
        mTopicSetOn = topicSetOn;
        mMembers = members;
        notifyDataSetChanged();
    }

    public List<NickWithPrefix> getMembers() {
        return mMembers;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_info_header, viewGroup, false);
            return new TextHolder(view);
        } else if (viewType == TYPE_TOPIC) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_topic, viewGroup, false);
            return new TopicHolder(view);
        } else { // TYPE_MEMBER
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_member, viewGroup, false);
            return new MemberHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int type = holder.getItemViewType();
        if (type == TYPE_HEADER)
            ((TextHolder) holder).bind(position == 0 ? R.string.channel_topic
                    : R.string.channel_members);
        else if (type == TYPE_TOPIC)
            ((TopicHolder) holder).bind(mTopic, mTopicSetBy, mTopicSetOn);
        else if (type == TYPE_MEMBER)
            ((MemberHolder) holder).bind(mConnection, mMembers.get(position - 3));
    }

    @Override
    public int getItemCount() {
        return 3 + (mMembers != null ? mMembers.size() : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || position == 2)
            return TYPE_HEADER;
        if (position == 1)
            return TYPE_TOPIC;
        return TYPE_MEMBER;
    }

    public static class TextHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        public TextHolder(View view) {
            super(view);
            textView = (TextView) view;
        }

        public void bind(int title) {
            textView.setText(title);
        }

        public void bind(String title) {
            if (title != null) {
                textView.setText(title);
            } else {
                textView.setText(null);
            }
        }

    }

    public static class TopicHolder extends RecyclerView.ViewHolder {

        private TextView topicTextView;
        private TextView topicInfoTextView;
        private int textColorSecondary;

        public TopicHolder(View view) {
            super(view);
            topicTextView = view.findViewById(R.id.topic);
            topicInfoTextView = view.findViewById(R.id.topic_info);
            textColorSecondary = StyledAttributesHelper.getColor(topicTextView.getContext(),
                    android.R.attr.textColorSecondary, Color.BLACK);

            topicTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        public void bind(String topic, String topicSetBy, Date topicSetOn) {
            if (topic != null) {
                topicTextView.setText(LinkHelper.addLinks(IRCColorUtils.getFormattedString(
                        topicTextView.getContext(), topic)));
            } else {
                SpannableString noTopicColored = new SpannableString(topicTextView.getResources()
                        .getString(R.string.channel_topic_none));
                noTopicColored.setSpan(new ForegroundColorSpan(textColorSecondary), 0,
                        noTopicColored.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                topicTextView.setText(noTopicColored);
            }
            if (topicSetBy != null && topicSetOn != null) {
                SpannableString topicSetByColored = new SpannableString(topicSetBy);
                topicSetByColored.setSpan(new ForegroundColorSpan(IRCColorUtils.getNickColor(
                        topicInfoTextView.getContext(), topicSetBy)), 0, topicSetBy.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                String topicSetOnStr = DateUtils.formatDateTime(topicInfoTextView.getContext(),
                        topicSetOn.getTime(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);

                topicInfoTextView.setText(SpannableStringHelper.getText(
                        topicInfoTextView.getContext(), R.string.channel_topic_info,
                        topicSetByColored, topicSetOnStr));
                topicInfoTextView.setVisibility(View.VISIBLE);
            } else {
                topicInfoTextView.setText(null);
                topicInfoTextView.setVisibility(View.GONE);
            }
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
            int colorId = IRCColorUtils.COLOR_MEMBER_NORMAL;
            if (prefix == '~')
                colorId = IRCColorUtils.COLOR_MEMBER_OWNER;
            else if (prefix == '&')
                colorId = IRCColorUtils.COLOR_MEMBER_ADMIN;
            else if (prefix == '@')
                colorId = IRCColorUtils.COLOR_MEMBER_OP;
            else if (prefix == '%')
                colorId = IRCColorUtils.COLOR_MEMBER_HALF_OP;
            else if (prefix == '+')
                colorId = IRCColorUtils.COLOR_MEMBER_VOICE;
            text.setTextColor(IRCColorUtils.getColorById(text.getContext(), colorId));
            if (prefix != ' ')
                text.setText(prefix + nickWithPrefix.getNick());
            else
                text.setText(nickWithPrefix.getNick());
        }

    }

}
