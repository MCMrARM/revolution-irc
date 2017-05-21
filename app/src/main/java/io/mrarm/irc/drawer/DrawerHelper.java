package io.mrarm.irc.drawer;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class DrawerHelper {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    public DrawerHelper(Activity activity) {
        mRecyclerView = (RecyclerView) activity.findViewById(R.id.nav_list);
        mLayoutManager = new LinearLayoutManager(activity);
        mRecyclerView.setLayoutManager(mLayoutManager);

        ArrayList<ServerConnectionInfo> testList = new ArrayList<>();
        ServerConnectionInfo testInfo = new ServerConnectionInfo("Freenode");
        ArrayList<String> testChannelList = new ArrayList<>();
        testChannelList.add("#test-channel");
        testChannelList.add("#android");
        testInfo.setChannels(testChannelList);
        testList.add(testInfo);
        ServerConnectionInfo testInfo2 = new ServerConnectionInfo("Private IRC network");
        testInfo2.setChannels(testChannelList);
        testList.add(testInfo2);
        ServerConnectionInfo testInfo3 = new ServerConnectionInfo("Slack");
        testInfo3.setChannels(testChannelList);
        testList.add(testInfo3);
        DrawerMenuListAdapter adapter = new DrawerMenuListAdapter(testList);
        mRecyclerView.setAdapter(adapter);
    }

}
