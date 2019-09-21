package io.mrarm.irc.newui.group;

import android.content.Context;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.UUID;

import io.mrarm.irc.R;
import io.mrarm.irc.view.CircleIconView;
import io.mrarm.observabletransform.BindableObservableBoolean;
import io.mrarm.observabletransform.ObservableLists;

public class MasterGroup {

    private final ObservableList<Group> mGroups = new ObservableArrayList<>();
    private UUID mUUID;
    private String mName;
    private CircleIconView.CustomizationInfo mIconCustomization;
    private BindableObservableBoolean mContainsDefaultGroup;

    public MasterGroup(UUID uuid) {
        mUUID = uuid;
        mContainsDefaultGroup = ObservableLists.containsMatching(mGroups,
                (v) -> v instanceof DefaultInsertBeforeGroup);
        mContainsDefaultGroup.bind();
    }

    public UUID getUUID() {
        return mUUID;
    }

    public ObservableList<Group> getGroups() {
        return mGroups;
    }

    public void add(Group g) {
        if (g.getParent() != null)
            throw new RuntimeException("Group already has a parent");
        mGroups.add(g);
        g.setParent(this);
    }

    public String getName(Context ctx) {
        if (mName != null)
            return mName;
        if (mGroups.size() == 1)
            return mGroups.get(0).getName(ctx);
        if (mContainsDefaultGroup.get())
            return ctx.getString(R.string.value_default);
        return null;
    }

    public void setCustomName(String name) {
        mName = name;
    }

    public CircleIconView.CustomizationInfo getIconCustomization(Context ctx) {
        if (mIconCustomization != null)
            return mIconCustomization;
        if (mGroups.size() == 1)
            return mGroups.get(0).getIconCustomization(ctx);
        return null;
    }

    public void setIconCustomization(CircleIconView.CustomizationInfo customization) {
        mIconCustomization = customization;
    }

}
