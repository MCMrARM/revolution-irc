package io.mrarm.irc.newui.group;

import android.content.Context;

import androidx.databinding.ObservableList;

import io.mrarm.irc.newui.group.view.AlphabeticalView;
import io.mrarm.irc.newui.group.view.GroupView;
import io.mrarm.irc.util.BoxedObservableList;
import io.mrarm.irc.view.ServerIconView;

abstract class BaseGroup implements Group {

    private MasterGroup mParent;
    private GroupView mGroupView;
    private final BoxedObservableList<ServerChannelPair> mChannels =
            new BoxedObservableList<>(null);

    @Override
    public ServerIconView.CustomizationInfo getIconCustomization(Context ctx) {
        return null;
    }

    @Override
    public MasterGroup getParent() {
        return mParent;
    }

    @Override
    public void setParent(MasterGroup parent) {
        this.mParent = parent;
    }

    @Override
    public ObservableList<ServerChannelPair> getChannels() {
        getGroupView();
        return mChannels;
    }

    @Override
    public void setGroupView(GroupView groupView) {
        if (mGroupView != null)
            groupView.setChannels(mGroupView.getChannels());
        mGroupView = groupView;
        mChannels.set(groupView.getChannels());
    }

    protected GroupView getGroupView() {
        if (mGroupView == null)
            setGroupView(new AlphabeticalView()); // Always make sure we create a GroupView
        return mGroupView;
    }

}
