package io.mrarm.irc.newui.group;

import android.content.Context;

import androidx.databinding.ObservableList;

import io.mrarm.irc.newui.group.view.GroupView;
import io.mrarm.irc.view.CircleIconView;

public interface Group {

    MasterGroup getParent();

    void setParent(MasterGroup group);


    String getName(Context ctx);

    CircleIconView.CustomizationInfo getIconCustomization(Context ctx);


    ObservableList<ServerChannelPair> getChannels();

    void setGroupView(GroupView groupView);

}
