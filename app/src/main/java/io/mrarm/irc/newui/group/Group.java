package io.mrarm.irc.newui.group;

import android.content.Context;

import androidx.databinding.ObservableList;

import io.mrarm.irc.view.ServerIconView;

public interface Group {

    MasterGroup getParent();

    void setParent(MasterGroup group);


    String getName(Context ctx);

    ServerIconView.CustomizationInfo getIconCustomization(Context ctx);

    ObservableList<ServerChannelPair> getChannels();

}
