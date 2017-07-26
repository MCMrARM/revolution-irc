package io.mrarm.irc.drawer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.SettingsActivity;

public class DrawerHelper implements ServerConnectionManager.ConnectionsListener, ServerConnectionInfo.InfoChangeListener, ServerConnectionInfo.ChannelListChangeListener {

    private Activity mActivity;
    private DrawerLayout mDrawerLayout;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private DrawerMenuListAdapter mAdapter;
    private DrawerMenuItem mSearchItem;
    private DrawerMenuItem mManageServersItem;
    private DrawerMenuItem mSettingsItem;

    public DrawerHelper(Activity activity) {
        mActivity = activity;
        mDrawerLayout = activity.findViewById(R.id.drawer_layout);
        mRecyclerView = activity.findViewById(R.id.nav_list);
        mLayoutManager = new LinearLayoutManager(activity);
        mRecyclerView.setLayoutManager(mLayoutManager);

        Resources r = activity.getResources();

        mAdapter = new DrawerMenuListAdapter(activity);

        mSearchItem = new DrawerMenuItem(r.getString(R.string.action_search), R.drawable.ic_search_white);
        mSearchItem.setOnClickListener((View view) -> {
            ChannelSearchDialog dialog = new ChannelSearchDialog(activity);
            dialog.show();
            mDrawerLayout.closeDrawers();
        });
        mAdapter.addTopMenuItem(mSearchItem);
        mManageServersItem = new DrawerMenuItem(r.getString(R.string.action_servers), R.drawable.ic_edit);
        mAdapter.addMenuItem(mManageServersItem);
        mSettingsItem = new DrawerMenuItem(r.getString(R.string.action_settings), R.drawable.ic_settings);
        mSettingsItem.setOnClickListener((View view) -> {
            activity.startActivity(new Intent(activity, SettingsActivity.class));
        });
        mAdapter.addMenuItem(mSettingsItem);

        mRecyclerView.setAdapter(mAdapter);
    }

    public void registerListeners() {
        ServerConnectionManager.getInstance(mActivity).addListener(this);
        ServerConnectionManager.getInstance(mActivity).addGlobalConnectionInfoListener(this);
        ServerConnectionManager.getInstance(mActivity).addGlobalChannelListListener(this);
    }

    public void unregisterListeners() {
        ServerConnectionManager.getInstance(mActivity).removeListener(this);
        ServerConnectionManager.getInstance(mActivity).removeGlobalConnectionInfoListener(this);
        ServerConnectionManager.getInstance(mActivity).removeGlobalChannelListListener(this);
    }

    public void setChannelClickListener(DrawerMenuListAdapter.ChannelClickListener listener) {
        mAdapter.setChannelClickListener(listener);
    }

    public DrawerMenuItem getManageServersItem() {
        return mManageServersItem;
    }

    public void setSelectedChannel(ServerConnectionInfo server, String channel) {
        mAdapter.setSelectedChannel(server, channel);
    }

    public void setSelectedMenuItem(DrawerMenuItem menuItem) {
        mAdapter.setSelectedMenuItem(menuItem);
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        mActivity.runOnUiThread(mAdapter::notifyServerListChanged);
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        mActivity.runOnUiThread(mAdapter::notifyServerListChanged);
    }

    @Override
    public void onConnectionInfoChanged(ServerConnectionInfo connection) {
        mActivity.runOnUiThread(() -> {
            mAdapter.notifyServerInfoChanged(connection);
        });
    }

    @Override
    public void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        mActivity.runOnUiThread(mAdapter::notifyServerListChanged);
    }
}
