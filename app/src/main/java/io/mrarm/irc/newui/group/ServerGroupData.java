package io.mrarm.irc.newui.group;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

import io.mrarm.irc.config.ServerConfigData;

public class ServerGroupData {

    @SerializedName("uuid")
    UUID mServerUUID;

    transient ServerConfigData mServer;
    transient MasterGroup mOwnedMasterGroup;
    transient SubGroup mOwnedSubGroup;

    UUID mDefaultSubGroupUUID;
    transient SubGroup mDefaultSubGroup;
    @SerializedName("defaultGroupCustom")
    boolean mDefaultSubGroupCustom;

    public ServerGroupData() {
    }

    public ServerGroupData(ServerConfigData server) {
        mServerUUID = server.uuid;
        mServer = server;
    }

    public String getName() {
        return mServer.name;
    }

    public void setDefaultSubGroup(SubGroup group) {
        mDefaultSubGroup = group;
        mDefaultSubGroupUUID = group != null ? group.getUUID() : null;
    }
}
