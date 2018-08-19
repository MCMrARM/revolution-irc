package io.mrarm.irc;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.util.AdvancedDividerItemDecoration;

public class IRCLinkActivity extends ThemedActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecyclerView recyclerView = new RecyclerView(this);
        setContentView(recyclerView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ServerConfigData> active = new ArrayList<>();
        List<ServerConfigData> inactive = new ArrayList<>();

        ServerConnectionManager connectionManager = ServerConnectionManager.getInstance(this);
        for (ServerConfigData server : ServerConfigManager.getInstance(this).getServers()) {
            if (connectionManager .hasConnection(server.uuid))
                active.add(server);
            else
                inactive.add(server);
        }
        LinkServerListAdapter adapter = new LinkServerListAdapter(
                "irc.mrarm.io", "#testing", active, inactive);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(adapter.createItemDecoration(this));
    }

    private static class LinkServerListAdapter extends RecyclerView.Adapter {

        private static final int TYPE_TEXT = 0;
        private static final int TYPE_HEADER = 1;
        private static final int TYPE_SERVER_ITEM = 2;
        private static final int TYPE_ACTION_ITEM = 3;

        private String mHostName;
        private String mChannelName;
        private List<ServerConfigData> mActiveServers;
        private List<ServerConfigData> mInactiveServers;

        public LinkServerListAdapter(String hostName, String channelName,
                                     List<ServerConfigData> active,
                                     List<ServerConfigData> inactive) {
            mHostName = hostName;
            mChannelName = channelName;
            mActiveServers = active;
            mInactiveServers = inactive;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_TEXT) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.irc_link_header_text_item, parent, false);
                return new TextHolder(view);
            } else if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.settings_list_header, parent, false);
                return new TextHolder(view);
            } else if (viewType == TYPE_SERVER_ITEM) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_list_item, parent, false);
                return new TextHolder(view);
            } else if (viewType == TYPE_ACTION_ITEM) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_list_item_with_icon, parent, false);
                return new TextIconHolder(view);
            }
            throw new IllegalArgumentException();
        }

        private boolean hasActiveHeader() {
            return mActiveServers.size() > 0;
        }

        private int getActiveHeaderIndex() {
            return 1;
        }

        private int getActiveListStart() {
            return hasActiveHeader() ? getActiveHeaderIndex() + 1 : getActiveHeaderIndex();
        }

        private boolean hasInactiveHeader() {
            return hasActiveHeader() && mInactiveServers.size() > 0;
        }

        private int getInactiveHeaderIndex() {
            return 1 + (hasActiveHeader() ? 1 : 0) + mActiveServers.size();
        }

        private int getInactiveListStart() {
            return hasInactiveHeader() ? getInactiveHeaderIndex() + 1 : getInactiveHeaderIndex();
        }

        private int getExtraActionsHeaderIndex() {
            return getInactiveListStart() + mInactiveServers.size();
        }

        private int getExtraActionsStart() {
            return getExtraActionsHeaderIndex() + 1;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position == 0)
                ((TextHolder) holder).bind(holder.itemView.getResources()
                        .getString(R.string.irc_link_pick_header, mHostName, mChannelName));
            else if (hasActiveHeader() && position == getActiveHeaderIndex())
                ((TextHolder) holder).bind(R.string.server_list_header_active);
            else if (position >= getActiveListStart() &&
                    position < getActiveListStart() + mActiveServers.size())
                ((TextHolder) holder).bind(mActiveServers.get(position - getActiveListStart())
                        .name);
            else if (hasInactiveHeader() && position == getInactiveHeaderIndex())
                ((TextHolder) holder).bind(R.string.server_list_header_inactive);
            else if (position >= getInactiveListStart() &&
                    position < getInactiveListStart() + mInactiveServers.size())
                ((TextHolder) holder).bind(mInactiveServers.get(position - getInactiveListStart())
                        .name);
            else if (position == getExtraActionsHeaderIndex())
                ((TextHolder) holder).bind(R.string.notification_header_options);
            else if (position == getExtraActionsStart() + 1)
                ((TextIconHolder) holder).bind(R.string.irc_link_show_all, R.drawable.ic_sort_white);
            else if (position == getExtraActionsStart())
                ((TextIconHolder) holder).bind(R.string.add_server, R.drawable.ic_add_white);
        }

        @Override
        public int getItemCount() {
            return 1 + (hasActiveHeader() ? 1 : 0) + mActiveServers.size() +
                    (hasInactiveHeader() ? 1 : 0) + mInactiveServers.size() + 3;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return TYPE_TEXT;
            if ((hasActiveHeader() && position == getActiveHeaderIndex()) ||
                    (hasInactiveHeader() && position == getInactiveHeaderIndex()) ||
                    position == getExtraActionsHeaderIndex())
                return TYPE_HEADER;
            if (position >= getExtraActionsStart())
                return TYPE_ACTION_ITEM;
            return TYPE_SERVER_ITEM;
        }

        public ItemDecoration createItemDecoration(Context context) {
            return new ItemDecoration(context);
        }

        public static class ItemDecoration extends AdvancedDividerItemDecoration {

            public ItemDecoration(Context context) {
                super(context);
            }

            @Override
            public boolean hasDivider(RecyclerView parent, View view) {
                RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
                int viewType = holder.getItemViewType();
                return viewType != TYPE_HEADER && viewType != TYPE_TEXT;
            }

        }

        private static final class TextHolder extends RecyclerView.ViewHolder {

            public TextHolder(View itemView) {
                super(itemView);
            }

            public void bind(int textResId) {
                ((TextView) itemView).setText(textResId);
            }

            public void bind(String text) {
                ((TextView) itemView).setText(text);
            }

        }

        private static final class TextIconHolder extends RecyclerView.ViewHolder {

            private TextView mTextView;
            private ImageView mImageView;

            public TextIconHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.text);
                mImageView = itemView.findViewById(R.id.icon);
            }

            public void bind(int textResId, int imageResId) {
                mTextView.setText(textResId);
                mImageView.setImageResource(imageResId);
            }

        }

    }

}
