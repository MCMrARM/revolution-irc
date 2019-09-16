package io.mrarm.irc.newui.group;

import androidx.databinding.ObservableList;

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


    public void onChannelListReset() {
        getDefaultGroup().getChannels().clear();
    }

    public void onChannelJoined(String channel) {
        getDefaultGroup().getChannels().add(new ServerChannelPair(mUUID, channel));
    }

    public void onChannelLeft(String channel) {
        ObservableList<ServerChannelPair> l = getDefaultGroup().getChannels();
        for (int i = 0; i < l.size(); i++) {
            ServerChannelPair p = l.get(i);
            if (p.getServer().equals(this.mUUID) && p.getChannel().equals(channel)) {
                l.remove(i);
                break;
            }
        }
    }

}
