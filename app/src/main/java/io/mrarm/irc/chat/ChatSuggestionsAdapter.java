package io.mrarm.irc.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.dto.ModeList;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.util.SelectableRecyclerViewAdapter;

public class ChatSuggestionsAdapter extends SelectableRecyclerViewAdapter<ChatSuggestionsAdapter.ItemHolder> implements Filterable {

    private ServerConnectionInfo mConnection;
    private List<NickWithPrefix> mMembers;
    private List<Object> mFilteredItems;
    private boolean mMembersEnabled = false;
    private boolean mChannelsEnabled = false;
    private boolean mUsersEnabled = false;
    private MyFilter mFilter;
    private OnItemClickListener mClickListener;

    public ChatSuggestionsAdapter(Context context, ServerConnectionInfo connection, List<NickWithPrefix> members) {
        super(context);
        mConnection = connection;
        mMembers = members;
        mFilteredItems = null;
    }

    public void setClickListener(OnItemClickListener listener) {
        mClickListener = listener;
    }

    public void setMembers(List<NickWithPrefix> members) {
        mMembers = members;
    }

    public void setEnabledSuggestions(boolean members, boolean channels, boolean users) {
        synchronized (this) {
            mMembersEnabled = members;
            mChannelsEnabled = channels;
            mUsersEnabled = users;
        }
    }

    public boolean areMembersEnabled() {
        synchronized (this) {
            return mMembersEnabled;
        }
    }

    public boolean areChannelsEnabled() {
        synchronized (this) {
            return mChannelsEnabled;
        }
    }

    public boolean areUsersEnabled() {
        synchronized (this) {
            return mUsersEnabled;
        }
    }

    public Object getItem(int position) {
        return mFilteredItems.get(position);
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_member, parent, false);
        return new ItemHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.bind(mFilteredItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mFilteredItems == null ? 0 : mFilteredItems.size();
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null)
            mFilter = new MyFilter();
        return mFilter;
    }

    public class ItemHolder extends SelectableRecyclerViewAdapter.ViewHolder {

        private TextView mText;

        public ItemHolder(View view) {
            super(view);
            mText = view.findViewById(R.id.chat_member);
            view.setOnClickListener((View v) -> {
                mClickListener.onItemClick(v.getTag());
            });
        }

        public void bind(Object item) {
            itemView.setTag(item);
            if (item instanceof NickWithPrefix) {
                ChannelInfoAdapter.MemberHolder.bindText(mText, (NickWithPrefix) item);
            } else {
                mText.setText(item.toString());
                mText.setTextColor(mText.getContext().getResources().getColor(R.color.memberNormal));
            }
        }

    }

    private class MyFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults ret = new FilterResults();
            String str = constraint.toString().toLowerCase();
            String mstr = str;
            if (str.length() > 0 && str.charAt(0) == '@')
                mstr = str.substring(1);
            List<Object> list = new ArrayList<>();
            if (areMembersEnabled() && mMembers != null) {
                for (NickWithPrefix member : mMembers) {
                    if (member.getNick().regionMatches(true, 0, mstr, 0, mstr.length()))
                        list.add(member);
                }
            }
            if (areChannelsEnabled()) {
                ModeList channelTypes = ((ServerConnectionApi) mConnection.getApiInstance())
                        .getServerConnectionData().getSupportList().getSupportedChannelTypes();
                for (String channel : mConnection.getChannels()) {
                    if (channel.length() == 0 || !channelTypes.contains(channel.charAt(0)))
                        continue;
                    if (channel.regionMatches(true, 0, str, 0, str.length()))
                        list.add(channel);
                }
            }
            if (areUsersEnabled()) {
                try {
                    list.addAll(mConnection.getApiInstance().getUserInfoApi().findUsers(str, null, null).get());
                } catch (Exception ignored) {
                }
            }
            ret.values = list;
            ret.count = list.size();
            return ret;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredItems = (List<Object>) results.values;
            notifyDataSetChanged();
        }

    }

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

}