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
import io.mrarm.irc.util.RecyclerViewElevationDecoration;

public class ServerListAdapter extends RecyclerView.Adapter<ServerListAdapter.Holder>
        implements ServerListChannelData.Listener,
        RecyclerViewElevationDecoration.ItemElevationCallback {

    /**
     * The count of items in a server group before the channels start (accounting for stuff like
     * a header)
     */
    private static final int SERVER_ITEMS_BEFORE_CHANNELS = 1;

    public static final int TYPE_SERVER_HEADER = 0;
    public static final int TYPE_CHANNEL = 1;

    private final ServerListChannelData mChannelData;
    private final RecyclerViewElevationDecoration mDecoration;

    private CallbackInterface mInterface;

    public ServerListAdapter(Context context, ServerListChannelData channelData) {
        mChannelData = channelData;
        channelData.addListener(this);
        mDecoration = new RecyclerViewElevationDecoration(context, this);
    }

    public void setCallbackInterface(CallbackInterface callbackInterface) {
        mInterface = callbackInterface;
    }

    private int getServerItemCount(ServerListChannelData.ServerGroup g) {
        return SERVER_ITEMS_BEFORE_CHANNELS + g.size();
    }

    private ServerListChannelData.ServerGroup findServerAt(int index) {
        int i = 0;
        for (ServerListChannelData.ServerGroup g : mChannelData.getServers()) {
            i += getServerItemCount(g);
            if (index < i)
                return g;
        }
        return null;
    }

    private int findServerStartPosition(ServerListChannelData.ServerGroup findGroup) {
        int i = 0;
        for (ServerListChannelData.ServerGroup g : mChannelData.getServers()) {
            if (findGroup == g)
                return i;
            i += getServerItemCount(g);
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        int cnt = 0;
        for (ServerListChannelData.ServerGroup g : mChannelData.getServers()) {
            cnt += getServerItemCount(g);
        }
        return cnt;
    }

    @Override
    public int getItemViewType(int position) {
        ServerListChannelData.ServerGroup g = findServerAt(position);
        int sPos = position - findServerStartPosition(g);
        if (sPos == 0)
            return TYPE_SERVER_HEADER;
        return TYPE_CHANNEL;
    }

    @Override
    public boolean isItemElevated(int position) {
        ServerListChannelData.ServerGroup g = findServerAt(position);
        int sPos = position - findServerStartPosition(g);
        return sPos != 0;
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
        if (viewType == TYPE_SERVER_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_server_list_header, parent, false);
            return new ServerHeaderHolder(v);
        } else if (viewType == TYPE_CHANNEL) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_server_list_channel, parent, false);
            return new ChannelHolder(v);
        }
        throw new IllegalArgumentException("Invalid viewType");
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (holder instanceof ServerHeaderHolder) {
            ServerListChannelData.ServerGroup g = findServerAt(position);
            ((ServerHeaderHolder) holder).bind(g);
        }
        if (holder instanceof ChannelHolder) {
            ServerListChannelData.ServerGroup g = findServerAt(position);
            if (g != null) {
                int p = findServerStartPosition(g);
                ((ChannelHolder) holder).bind(g.get(position - p - SERVER_ITEMS_BEFORE_CHANNELS));
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        holder.unbind();
    }

    @Override
    public void onServerAdded(ServerListChannelData.ServerGroup group) {
        int pos = findServerStartPosition(group);
        notifyItemRangeInserted(pos, getServerItemCount(group));
    }

    @Override
    public void onServerRemoved(ServerListChannelData.ServerGroup group) {
        int pos = findServerStartPosition(group);
        notifyItemRangeRemoved(pos, getServerItemCount(group));
    }

    @Override
    public void onChannelAdded(ServerListChannelData.ServerGroup group, int index) {
        int pos = findServerStartPosition(group);
        notifyItemInserted(pos + SERVER_ITEMS_BEFORE_CHANNELS + index);
    }

    @Override
    public void onChannelRemoved(ServerListChannelData.ServerGroup group, int index) {
        int pos = findServerStartPosition(group);
        notifyItemRemoved(pos + SERVER_ITEMS_BEFORE_CHANNELS + index);
    }

    @Override
    public void onChannelListReset(ServerListChannelData.ServerGroup group, int oldCount) {
        int pos = findServerStartPosition(group);
        notifyItemRangeRemoved(pos + SERVER_ITEMS_BEFORE_CHANNELS, oldCount);
        notifyItemRangeInserted(pos + SERVER_ITEMS_BEFORE_CHANNELS, group.size());
    }

    public static class Holder extends RecyclerView.ViewHolder {

        public Holder(@NonNull View itemView) {
            super(itemView);
        }

        public void unbind() {
        }

    }

    public static class ServerHeaderHolder extends Holder {

        private TextView mTextView;

        public ServerHeaderHolder(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.title);
        }

        public void bind(ServerListChannelData.ServerGroup g) {
            mTextView.setText(g.getConnection().getName());
        }

    }

    public class ChannelHolder extends Holder
            implements ServerListChannelData.ChannelEntry.Listener, View.OnClickListener {

        private ServerListChannelData.ChannelEntry mEntry;
        private TextView mName;
        private TextView mTopic;

        public ChannelHolder(@NonNull View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mTopic = itemView.findViewById(R.id.topic);
            itemView.setOnClickListener(this);
        }

        public void bind(ServerListChannelData.ChannelEntry e) {
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
