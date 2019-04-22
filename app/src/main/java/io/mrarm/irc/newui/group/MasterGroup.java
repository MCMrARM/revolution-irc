package io.mrarm.irc.newui.group;

import com.google.gson.annotations.SerializedName;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MasterGroup {

    @SerializedName("uuid")
    UUID mUUID;
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

    public void addSubGroup(SubGroup sg) {
        if (sg.mUUID == null)
            throw new InvalidParameterException("The sub group must first be registered in " +
                    "the GroupManager");
        if (sg.mParent != null)
            throw new InvalidParameterException("The sub group already has a parent");
        sg.mParent = this;
        mSubGroups.add(sg);
    }

    public void removeSubGroup(SubGroup sg) {
        if (sg.mParent != this)
            throw new InvalidParameterException("Wrong sub group parent");
        mSubGroups.remove(sg);
        sg.mParent = null;
    }

}
