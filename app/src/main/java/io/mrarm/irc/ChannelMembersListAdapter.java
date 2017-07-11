package io.mrarm.irc;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.dto.NickWithPrefix;

public class ChannelMembersListAdapter extends BaseAdapter implements Filterable {

    private List<NickWithPrefix> mMembers;
    private List<NickWithPrefix> mFilteredMembers;
    private MyFilter mFilter;

    public ChannelMembersListAdapter(List<NickWithPrefix> members) {
        mMembers = members;
        mFilteredMembers = mMembers;
    }

    public void setMembers(List<NickWithPrefix> members) {
        mMembers = members;
    }

    @Override
    public int getCount() {
        return mFilteredMembers == null ? 0 : mFilteredMembers.size();
    }

    @Override
    public Object getItem(int position) {
        return mFilteredMembers.get(position).getNick();
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
            convertView.setTag(new ChannelMembersAdapter.MemberHolder(convertView));
        }
        ChannelMembersAdapter.MemberHolder holder = (ChannelMembersAdapter.MemberHolder) convertView.getTag();
        holder.bind(mFilteredMembers.get(position));
        return convertView;
    }

    private class MyFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults ret = new FilterResults();
            if (constraint == null || constraint.length() == 0 ||
                    (constraint.length() == 1 && constraint.charAt(0) == '@')) {
                ret.values = mMembers;
                ret.count = mMembers.size();
            } else {
                String str = constraint.toString();
                if (str.charAt(0) == '@')
                    str = str.substring(1);
                List<NickWithPrefix> list = new ArrayList<>();
                for (NickWithPrefix member : mMembers) {
                    if (member.getNick().startsWith(str))
                        list.add(member);
                }
                ret.values = list;
                ret.count = list.size();
            }
            return ret;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredMembers = (List<NickWithPrefix>) results.values;
            notifyDataSetChanged();
        }

    }

}