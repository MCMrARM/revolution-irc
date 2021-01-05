package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.ChatApi;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.util.AdvancedDividerItemDecoration;

public class IRCLinkActivity extends ThemedActivity {

    private static final int REQUEST_ADD_SERVER = 100;

    private String mHostName;
    private String mChannelName;
    private ServerConnectionInfo mSelectedConnection;
    private OpenTaskChannelListListener mOpenTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || getIntent().getAction() == null ||
                !getIntent().getAction().equals(Intent.ACTION_VIEW) ||
                getIntent().getData() == null) {
            finish();
            return;
        }

        RecyclerView recyclerView = new RecyclerView(this);
        setContentView(recyclerView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String str = getIntent().getData().toString();
        int iof = str.indexOf(":/");
        if (iof == -1) {
            finish();
            return;
        }
        iof += 2;
        if (str.length() > iof && str.charAt(iof) == '/')
            iof++;
        str = str.substring(iof);
        iof = str.indexOf('/');
        if (iof == -1 || iof == str.length() - 1) {
            Intent intent = new Intent(this, EditServerActivity.class);
            intent.putExtra(EditServerActivity.ARG_NAME, str);
            intent.putExtra(EditServerActivity.ARG_ADDRESS, str);
            startActivity(intent);

            finish();
            return;
        }
        mHostName = str.substring(0, iof);
        mChannelName = prefixChannelIfNeeded(str.substring(iof + 1));

        LinkServerListAdapter adapter = new LinkServerListAdapter(
                this, mHostName, mChannelName);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(adapter.createItemDecoration(this));
    }

    private static String prefixChannelIfNeeded(String name) {
        if (name == null || name.length() == 0)
            return "";
        char c = name.charAt(0);
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
            return "#" + name;
        return name;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ADD_SERVER && data != null && data.getAction() != null &&
                data.getAction().equals(EditServerActivity.RESULT_ACTION)) {
            ServerConnectionManager mgr = ServerConnectionManager.getInstance(this);

            String uuid = data.getStringExtra(EditServerActivity.ARG_SERVER_UUID);
            openServer(ServerConfigManager.getInstance(this).findServer(
                    UUID.fromString(uuid)));
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenTask != null) {
            mSelectedConnection.removeOnChannelListChangeListener(mOpenTask);
        }
    }

    public void openServer(ServerConfigData server) {
        if (mSelectedConnection != null)
            throw new RuntimeException();

        ArrayList<String> channels = new ArrayList<>();
        channels.add(mChannelName);

        ServerConnectionManager mgr = ServerConnectionManager.getInstance(this);
        if (!mgr.hasConnection(server.uuid))
            mgr.tryCreateConnection(server, this);
        ServerConnectionInfo connection = mgr.getConnection(server.uuid);
        if (connection == null)
            return;
        mSelectedConnection = connection;
        ChatApi api = connection.getApiInstance();
        if (api == null)
            return;
        if (connection.hasChannel(mChannelName)) {
            startActivity(MainActivity.getLaunchIntent(this, connection, mChannelName));
            finish();
            return;
        }
        setContentView(R.layout.dialog_please_wait);
        api.joinChannels(channels, (Void vv) -> {
            if (connection.hasChannel(mChannelName)) {
                startActivity(MainActivity.getLaunchIntent(this, connection, mChannelName));
                finish();
                return;
            }
            mOpenTask = new OpenTaskChannelListListener(mChannelName);
            connection.addOnChannelListChangeListener(mOpenTask);
        }, (Exception e) -> finish());
    }

    private class OpenTaskChannelListListener
            implements ServerConnectionInfo.ChannelListChangeListener {

        private String mChannel;

        public OpenTaskChannelListListener(String channel) {
            mChannel = channel;
        }

        @Override
        public void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
            if (newChannels.contains(mChannel)) {
                startActivity(MainActivity.getLaunchIntent(IRCLinkActivity.this,
                        connection, mChannel));
                connection.removeOnChannelListChangeListener(this);
                mOpenTask = null;
                finish();
            }
        }

    }

    private static class LinkServerListAdapter extends RecyclerView.Adapter {

        private static final int TYPE_TEXT = 0;
        private static final int TYPE_HEADER = 1;
        private static final int TYPE_SERVER_ITEM = 2;
        private static final int TYPE_ACTION_ITEM = 3;

        private IRCLinkActivity mContext;
        private String mHostName;
        private String mChannelName;
        private List<ServerConfigData> mActiveServers;
        private List<ServerConfigData> mInactiveServers;

        public LinkServerListAdapter(IRCLinkActivity context, String hostName, String channelName) {
            mContext = context;
            mHostName = hostName;
            mChannelName = channelName;
            reloadServerList(true);
        }

        /**
         * Returns the main domain for the specified hostname.
         * Example: test.freenode.net => freenode.net
         */
        private String getHostnameDomain(String hname) {
            if (hname == null || hname.isEmpty())
                return hname;
            int iof = hname.lastIndexOf('.');
            if (iof <= 0)
                return hname;
            iof = hname.lastIndexOf('.', iof - 1);
            if (iof <= 0)
                return hname;
            return hname.substring(iof + 1);
        }

        private void reloadServerList(boolean filter) {
            mActiveServers = new ArrayList<>();
            mInactiveServers = new ArrayList<>();
            ServerConnectionManager connectionManager =
                    ServerConnectionManager.getInstance(mContext);
            for (ServerConfigData server : ServerConfigManager.getInstance(mContext).getServers()) {
                if (filter) {
                    if (!getHostnameDomain(server.address).equals(getHostnameDomain(mHostName)))
                        continue;
                }
                if (connectionManager .hasConnection(server.uuid))
                    mActiveServers.add(server);
                else
                    mInactiveServers.add(server);
            }
            notifyDataSetChanged();
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
                return new ServerHolder(view);
            } else if (viewType == TYPE_ACTION_ITEM) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_list_item_with_icon, parent, false);
                return new ActionHolder(view);
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
                ((ServerHolder) holder).bind(mActiveServers.get(position - getActiveListStart()));
            else if (hasInactiveHeader() && position == getInactiveHeaderIndex())
                ((TextHolder) holder).bind(R.string.server_list_header_inactive);
            else if (position >= getInactiveListStart() &&
                    position < getInactiveListStart() + mInactiveServers.size())
                ((ServerHolder) holder).bind(mInactiveServers.get(position - getInactiveListStart()));
            else if (position == getExtraActionsHeaderIndex())
                ((TextHolder) holder).bind(R.string.notification_header_options);
            else if (position == getExtraActionsStart())
                ((ActionHolder) holder).bind(ActionHolder.ACTION_ADD);
            else if (position == getExtraActionsStart() + 1)
                ((ActionHolder) holder).bind(ActionHolder.ACTION_SHOW_ALL);
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

        private static class TextHolder extends RecyclerView.ViewHolder {

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

        private final class ServerHolder extends TextHolder implements View.OnClickListener {

            private static final int ACTION_ADD = 1;
            private static final int ACTION_SHOW_ALL = 2;

            ServerConfigData mServer;

            public ServerHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
            }

            public void bind(ServerConfigData server) {
                mServer = server;
                super.bind(server.name);
            }


            @Override
            public void onClick(View v) {
                mContext.openServer(mServer);
            }

        }

        private static class TextIconHolder extends RecyclerView.ViewHolder {

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

        private final class ActionHolder extends TextIconHolder implements View.OnClickListener {

            private static final int ACTION_ADD = 1;
            private static final int ACTION_SHOW_ALL = 2;

            private int mActionType;

            public ActionHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
            }

            public void bind(int actionType) {
                mActionType = actionType;
                if (actionType == ACTION_ADD)
                    super.bind(R.string.add_server, R.drawable.ic_add_white);
                else if (actionType == ACTION_SHOW_ALL)
                    super.bind(R.string.irc_link_show_all, R.drawable.ic_sort_white);
            }


            @Override
            public void onClick(View v) {
                if (mActionType == ACTION_ADD) {
                    ArrayList<String> channels = new ArrayList<>();
                    channels.add(mChannelName);

                    Intent intent = new Intent(v.getContext(), EditServerActivity.class);
                    intent.putExtra(EditServerActivity.ARG_NAME, mHostName);
                    intent.putExtra(EditServerActivity.ARG_ADDRESS, mHostName);
                    intent.putExtra(EditServerActivity.ARG_AUTOJOIN_CHANNELS, channels);
                    mContext.startActivityForResult(intent, REQUEST_ADD_SERVER);
                }
                if (mActionType == ACTION_SHOW_ALL) {
                    reloadServerList(false);
                }
            }

        }

    }

}
