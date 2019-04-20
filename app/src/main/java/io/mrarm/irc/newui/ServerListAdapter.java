package io.mrarm.irc.newui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.util.RecyclerViewElevationDecoration;

public class ServerListAdapter extends RecyclerView.Adapter implements
        ServerActiveListData.Listener, ServerInactiveListData.Listener,
        RecyclerViewElevationDecoration.ItemElevationCallback {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ACTIVE_SERVER = 1;
    private static final int TYPE_INACTIVE_SERVER = 2;

    private final RecyclerViewElevationDecoration mDecoration;
    private final ServerActiveListData mActiveData;
    private final ServerInactiveListData mInactiveData;
    private boolean mActiveHeaderVisible;
    private boolean mInactiveHeaderVisible;

    public ServerListAdapter(Context context, ServerActiveListData activeData,
                             ServerInactiveListData inactiveData) {
        mActiveData = activeData;
        mInactiveData = inactiveData;
        mActiveData.setListener(this);
        mInactiveData.setListener(this);
        mDecoration = new RecyclerViewElevationDecoration(context, this);
        updateActiveListHeaderVisibility();
        updateInactiveListHeaderVisibility();
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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ACTIVE_SERVER || viewType == TYPE_INACTIVE_SERVER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_server_list_channel, parent, false);
            return new ActiveServerItem(view);
        } else if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_server_list_header, parent, false);
            return new HeaderHolder(v);
        }
        throw new IllegalStateException("Bad viewType");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = holder.getItemViewType();
        if (type == TYPE_HEADER) {
            ((HeaderHolder) holder).bind(position == 0 ? R.string.server_list_header_active
                    : R.string.server_list_header_inactive);
        } else if (type == TYPE_ACTIVE_SERVER) {
            ((ActiveServerItem) holder).bind(mActiveData.get(position - getActiveListStart()));
        } else if (type == TYPE_INACTIVE_SERVER) {
            ((ActiveServerItem) holder).bind(mInactiveData.get(position - getInactiveListStart()));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mActiveHeaderVisible)
            return TYPE_HEADER;
        if (position == getActiveListStart() + mActiveData.size() && mActiveHeaderVisible)
            return TYPE_HEADER;
        return position >= getInactiveListStart() ? TYPE_INACTIVE_SERVER : TYPE_ACTIVE_SERVER;
    }

    @Override
    public boolean isItemElevated(int index) {
        if (index == 0 && mActiveHeaderVisible)
            return false;
        if (index == getActiveListStart() + mActiveData.size() && mInactiveHeaderVisible)
            return false;
        return true;
    }

    @Override
    public int getItemCount() {
        return getInactiveListStart() + mInactiveData.size();
    }

    public int getActiveListStart() {
        return mActiveHeaderVisible ? 1 : 0;
    }

    public void updateActiveListHeaderVisibility() {
        boolean newVisibility = mActiveData.size() > 0;
        if (newVisibility == mActiveHeaderVisible)
            return;
        mActiveHeaderVisible = newVisibility;
        if (mActiveHeaderVisible)
            notifyItemInserted(0);
        else
            notifyItemRemoved(0);
    }

    public int getInactiveListStart() {
        return getActiveListStart() + mActiveData.size() + (mInactiveHeaderVisible ? 1 : 0);
    }

    public void updateInactiveListHeaderVisibility() {
        boolean newVisibility = mInactiveData.size() > 0;
        if (newVisibility == mInactiveHeaderVisible)
            return;
        mInactiveHeaderVisible = newVisibility;
        int index = getActiveListStart() + mActiveData.size();
        if (mInactiveHeaderVisible)
            notifyItemInserted(index);
        else
            notifyItemRemoved(index);
    }


    @Override
    public void onActiveConnectionAdded(int index) {
        updateActiveListHeaderVisibility();
        notifyItemInserted(getActiveListStart() + index);
    }

    @Override
    public void onActiveConnectionRemoved(int index) {
        notifyItemRemoved(getActiveListStart() + index);
        updateActiveListHeaderVisibility();
    }

    @Override
    public void onServerAdded(int index) {

    }

    @Override
    public void onServerRemoved(int index) {

    }

    @Override
    public void onServerUpdated(int index) {

    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {

        private TextView mTextView;

        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.title);
        }

        public void bind(int nameRes) {
            mTextView.setText(nameRes);
        }

    }


    public static class ActiveServerItem extends RecyclerView.ViewHolder {

        private TextView mName;

        public ActiveServerItem(@NonNull View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            itemView.findViewById(R.id.topic).setVisibility(View.GONE);
        }

        public void bind(ServerConnectionInfo conn) {
            mName.setText(conn.getName());
        }

        public void bind(ServerConfigData conn) {
            mName.setText(conn.name);
        }

    }

}
