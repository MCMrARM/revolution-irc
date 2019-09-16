package io.mrarm.irc.newui.group.v2;

import androidx.databinding.ObservableList;

public interface Group {

    MasterGroup getParent();

    void setParent(MasterGroup group);


    String getName();

    ObservableList<ServerChannelPair> getChannels();

}
