package io.mrarm.irc.newui.group;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MasterGroup {

    @SerializedName("uuid")
    private UUID mUUID;
    @SerializedName("name")
    private String mName;
    @SerializedName("owner")
    UUID mOwnerUUID;
    private transient ServerGroupData mOwner;
    @SerializedName("subGroups")
    List<SubGroup> mSubGroups = new ArrayList<>();

    public UUID getUUID() {
        return mUUID;
    }

    public void setUUID(UUID uuid) {
        mUUID = uuid;
    }

    public String getName() {
        if (mName != null)
            return mName;
        if (mOwner != null)
            return mOwner.getName();
        return null;
    }

    public void setName(String name) {
        mName = name;
    }

    public ServerGroupData getOwner() {
        return mOwner;
    }

    public void setOwner(ServerGroupData owner) {
        if (mOwner != null) {
            mOwner.mOwnedMasterGroup = null;
        }
        mOwnerUUID = owner.mServerUUID;
        mOwner = owner;
        if (owner != null) {
            owner.mOwnedMasterGroup = this;
        }
    }

    public List<SubGroup> getSubGroups() {
        return Collections.unmodifiableList(mSubGroups);
    }
}
