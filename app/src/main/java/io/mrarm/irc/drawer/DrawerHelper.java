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

    public DrawerHelper(Activity activity) {
        mRecyclerView = (RecyclerView) activity.findViewById(R.id.nav_list);
        mLayoutManager = new LinearLayoutManager(activity);
        mRecyclerView.setLayoutManager(mLayoutManager);

        Resources r = activity.getResources();

        DrawerMenuListAdapter adapter = new DrawerMenuListAdapter(ServerConnectionManager
                .getInstance().getConnections());

        for (ServerConnectionInfo connectionInfo : ServerConnectionManager.getInstance()
                .getConnections()) {
            connectionInfo.addOnChannelListChangeListener((ServerConnectionInfo connection,
                                                           List<String> newChannels) -> {
                activity.runOnUiThread(adapter::notifyServerListChanged);
            });
        }

        adapter.addMenuItem(new DrawerMenuItem(r.getString(R.string.action_servers),
                r.getDrawable(R.drawable.ic_edit)));
        adapter.addMenuItem(new DrawerMenuItem(r.getString(R.string.action_settings),
                r.getDrawable(R.drawable.ic_settings)));

        mRecyclerView.setAdapter(adapter);
    }

}
