package io.mrarm.irc.newui.group;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class SubGroup {

    @SerializedName("uuid")
    private UUID mUUID;
    @SerializedName("name")
    private String mName;
    @SerializedName("owner")
    UUID mOwnerUUID;
    private transient ServerGroupData mOwner;

    public UUID getUUID() {
        return mUUID;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public ServerGroupData getOwner() {
        return mOwner;
    }

    public void setOwner(ServerGroupData owner) {
        if (mOwner != null) {
            mOwner.mOwnedSubGroup = null;
        }
        mOwner = owner;
        if (owner != null) {
            owner.mOwnedSubGroup = this;
        }
    }

}
