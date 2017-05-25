package io.mrarm.irc.drawer;

import android.app.Activity;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;

public class DrawerHelper {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private DrawerMenuListAdapter mAdapter;

    public DrawerHelper(Activity activity) {
        mRecyclerView = (RecyclerView) activity.findViewById(R.id.nav_list);
        mLayoutManager = new LinearLayoutManager(activity);
        mRecyclerView.setLayoutManager(mLayoutManager);

        Resources r = activity.getResources();

        mAdapter = new DrawerMenuListAdapter(activity,
                ServerConnectionManager.getInstance().getConnections());

        for (ServerConnectionInfo connectionInfo : ServerConnectionManager.getInstance()
                .getConnections()) {
            connectionInfo.addOnChannelListChangeListener((ServerConnectionInfo connection,
                                                           List<String> newChannels) -> {
                activity.runOnUiThread(mAdapter::notifyServerListChanged);
            });
        }

        mAdapter.addMenuItem(new DrawerMenuItem(r.getString(R.string.action_servers),
                r.getDrawable(R.drawable.ic_edit)));
        mAdapter.addMenuItem(new DrawerMenuItem(r.getString(R.string.action_settings),
                r.getDrawable(R.drawable.ic_settings)));

        mRecyclerView.setAdapter(mAdapter);
    }

    public void setChannelClickListener(DrawerMenuListAdapter.ChannelClickListener listener) {
        mAdapter.setChannelClickListener(listener);
    }

    public void setSelectedChannel(ServerConnectionInfo server, String channel) {
        mAdapter.setSelectedChannel(server, channel);
    }

}
