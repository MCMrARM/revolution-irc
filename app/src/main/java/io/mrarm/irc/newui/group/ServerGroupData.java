package io.mrarm.irc.newui.group;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.config.ServerConfigData;

public class ServerGroupData {

    private GroupManager mGroupManager;
    private UUID mUUID;
    private DefaultServerGroup mDefaultGroup;

    public ServerGroupData(GroupManager mgr, UUID uuid) {
        mGroupManager = mgr;
        mUUID = uuid;
    }

    public DefaultServerGroup createDefaultGroup() {
        mDefaultGroup = new DefaultServerGroup(this);
        return mDefaultGroup;
    }

    public String getName() {
        ServerConfigData data = mGroupManager.mConfigManager.findServer(mUUID);
        return data != null ? data.name : null;
    }

    public DefaultServerGroup getDefaultGroup() {
        return mDefaultGroup;
    }


    public void onChannelListReset(List<String> channels) {
        List<ServerChannelPair> sch = new ArrayList<>();
        for (String c : channels)
            sch.add(new ServerChannelPair(mUUID, c));
        getDefaultGroup().getGroupView().setChannels(sch);
    }

    public void onChannelJoined(String channel) {
        getDefaultGroup().getGroupView().addChannel(new ServerChannelPair(mUUID, channel));
    }

    public void onChannelLeft(String channel) {
        getDefaultGroup().getGroupView().removeChannel(new ServerChannelPair(mUUID, channel));
    }

}
