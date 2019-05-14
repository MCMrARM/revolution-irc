package io.mrarm.irc.newui.group;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

public class SubGroup {

    @SerializedName("uuid")
    UUID mUUID;
    @SerializedName("name")
    private String mName;
    @SerializedName("owner")
    UUID mOwnerUUID;
    private transient ServerGroupData mOwner;
    MasterGroup mParent;
    transient final ObservableList<String> mCurrentChannels = new ObservableArrayList<>();

    public MasterGroup getParent() {
        return mParent;
    }

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

    public ObservableList<String> getCurrentChannels() {
        return mCurrentChannels;
    }
}
