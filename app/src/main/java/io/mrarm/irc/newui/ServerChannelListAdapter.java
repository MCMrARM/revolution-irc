package io.mrarm.irc.newui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.newui.ServerChannelListData.ChannelEntry;
import io.mrarm.irc.newui.ServerChannelListData.Group;
import io.mrarm.irc.util.RecyclerViewElevationDecoration;

public class ServerChannelListAdapter extends RecyclerView.Adapter<ServerChannelListAdapter.Holder>
        implements ServerChannelListData.Listener,
        RecyclerViewElevationDecoration.ItemElevationCallback {

    /**
     * The count of items in a channel group before the channels start (accounting for stuff like
     * a header)
     */
    private static final int GROUP_ITEMS_BEFORE_CHANNELS = 1;

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_CHANNEL = 1;

    private final ServerChannelListData mChannelData;
    private final RecyclerViewElevationDecoration mDecoration;

    private CallbackInterface mInterface;

    public ServerChannelListAdapter(Context context, ServerChannelListData channelData) {
        mChannelData = channelData;
        channelData.addListener(this);
        mDecoration = new RecyclerViewElevationDecoration(context, this);
    }

    public void setCallbackInterface(CallbackInterface callbackInterface) {
        mInterface = callbackInterface;
    }

    private boolean areHeadersVisible() {
        return mChannelData.getGroups().size() > 1;
    }

    public int getGroupsStart() {
        return 0;
    }

    private int getGroupItemCount(Group g) {
        if (!areHeadersVisible())
            return g.size();
        return GROUP_ITEMS_BEFORE_CHANNELS + g.size();
    }

    private Group<ChannelEntry> findGroupAt(int index) {
        int i = getGroupsStart();
        for (Group<ChannelEntry> g : mChannelData.getGroups()) {
            i += getGroupItemCount(g);
            if (index < i)
                return g;
        }
        return null;
    }

    private int findGroupStartPosition(Group<ChannelEntry> findGroup) {
        int i = getGroupsStart();
        for (Group<ChannelEntry> g : mChannelData.getGroups()) {
            if (findGroup == g)
                return i;
            i += getGroupItemCount(g);
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        int cnt = getGroupsStart();
        for (Group<ChannelEntry> g : mChannelData.getGroups()) {
            cnt += getGroupItemCount(g);
        }
        return cnt;
    }

    @Override
    public int getItemViewType(int position) {
        Group<ChannelEntry> g = findGroupAt(position);
        int sPos = position - findGroupStartPosition(g);
        if (sPos == 0 && areHeadersVisible())
            return TYPE_HEADER;
        return TYPE_CHANNEL;
    }

    @Override
    public boolean isItemElevated(int position) {
        Group<ChannelEntry> g = findGroupAt(position);
        int sPos = position - findGroupStartPosition(g);
        return sPos != 0 || !areHeadersVisible();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.addItemDecoration(mDecoration);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.removeItemDecoration(mDecoration);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_server_list_header, parent, false);
            return new HeaderHolder(v);
        } else if (viewType == TYPE_CHANNEL) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_server_list_channel, parent, false);
            return new ChannelHolder(v);
        }
        throw new IllegalArgumentException("Invalid viewType");
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (holder instanceof HeaderHolder) {
            Group<ChannelEntry> g = findGroupAt(position);
            if (g != null)
                ((HeaderHolder) holder).bind(g);
            else
                ((HeaderHolder) holder).bindInactiveConnections();
        }
        if (holder instanceof ChannelHolder) {
            Group<ChannelEntry> g = findGroupAt(position);
            if (g != null) {
                int p = findGroupStartPosition(g);
                ((ChannelHolder) holder).bind(g.get(position - p -
                        (areHeadersVisible() ? GROUP_ITEMS_BEFORE_CHANNELS : 0)));
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        holder.unbind();
    }

    @Override
    public void onGroupAdded(Group<ChannelEntry> group) {
        int pos = findGroupStartPosition(group);
        notifyItemRangeInserted(pos, getGroupItemCount(group));
        if (mChannelData.getGroups().size() == 2)
            notifyItemInserted(getGroupsStart());
    }

    @Override
    public void onGroupRemoved(Group<ChannelEntry> group) {
        int pos = findGroupStartPosition(group);
        notifyItemRangeRemoved(pos, getGroupItemCount(group));
        if (mChannelData.getGroups().size() == 1)
            notifyItemRemoved(getGroupsStart());
    }

    @Override
    public void onChannelAdded(Group<ChannelEntry> group, int index) {
        int pos = findGroupStartPosition(group);
        notifyItemInserted(pos + GROUP_ITEMS_BEFORE_CHANNELS + index);
    }

    @Override
    public void onChannelRemoved(Group<ChannelEntry> group, int index) {
        int pos = findGroupStartPosition(group);
        notifyItemRemoved(pos + GROUP_ITEMS_BEFORE_CHANNELS + index);
    }

    @Override
    public void onChannelListReset(Group<ChannelEntry> group, int oldCount) {
        int pos = findGroupStartPosition(group);
        notifyItemRangeRemoved(pos + GROUP_ITEMS_BEFORE_CHANNELS, oldCount);
        notifyItemRangeInserted(pos + GROUP_ITEMS_BEFORE_CHANNELS, group.size());
    }

    public static class Holder extends RecyclerView.ViewHolder {

        public Holder(@NonNull View itemView) {
            super(itemView);
        }

        public void unbind() {
        }

    }

    public static class HeaderHolder extends Holder {

        private TextView mTextView;

        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.title);
        }

        public void bind(Group<ChannelEntry> g) {
            mTextView.setText(g.getName());
        }

        public void bindInactiveConnections() {
            mTextView.setText(R.string.server_list_header_inactive);
        }

    }

    public class ChannelHolder extends Holder
            implements ChannelEntry.Listener, View.OnClickListener {

        private ChannelEntry mEntry;
        private TextView mName;
        private TextView mTopic;

        public ChannelHolder(@NonNull View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mTopic = itemView.findViewById(R.id.topic);
            itemView.setOnClickListener(this);
        }

        public void bind(ChannelEntry e) {
            if (mEntry != null)
                throw new IllegalStateException("mEntry is not null");
            mEntry = e;
            e.bind(this);
            mName.setText(e.getName());
            onInfoChanged();
        }

        @Override
        public void unbind() {
            mEntry.unbind(this);
            mEntry = null;
        }

        @Override
        public void onInfoChanged() {
            mTopic.setVisibility(TextUtils.isEmpty(mEntry.getTopic()) ? View.GONE : View.VISIBLE);
            mTopic.setText(mEntry.getTopic());
        }

        @Override
        public void onClick(View v) {
            mInterface.onChatOpened(mEntry.getGroup().getConnection(), mEntry.getName());
        }
    }

    public interface CallbackInterface {

        void onChatOpened(ServerConnectionInfo server, String channel);

    }

}
