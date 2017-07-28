package io.mrarm.irc.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class ChatSuggestionsAdapter extends BaseAdapter implements Filterable {

    private ServerConnectionInfo mConnection;
    private List<NickWithPrefix> mMembers;
    private List<Object> mFilteredItems;
    private boolean mMembersEnabled = false;
    private boolean mChannelsEnabled = false;
    private MyFilter mFilter;

    public ChatSuggestionsAdapter(ServerConnectionInfo connection, List<NickWithPrefix> members) {
        mConnection = connection;
        mMembers = members;
        mFilteredItems = null;
    }

    public void setMembers(List<NickWithPrefix> members) {
        mMembers = members;
    }

    public void setEnabledSuggestions(boolean members, boolean channels) {
        synchronized (this) {
            mMembersEnabled = members;
            mChannelsEnabled = channels;
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

    @Override
    public int getCount() {
        return mFilteredItems == null ? 0 : mFilteredItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mFilteredItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null)
            mFilter = new MyFilter();
        return mFilter;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_member, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.chat_member);
        Object item = mFilteredItems.get(position);
        if (item instanceof NickWithPrefix)
            ChannelMembersAdapter.MemberHolder.bindText(textView, (NickWithPrefix) item);
        else
            textView.setText(item.toString());
        return convertView;
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
                for (String channel : mConnection.getChannels()) {
                    if (channel.regionMatches(true, 0, str, 0, str.length()))
                        list.add(channel);
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

}