package io.mrarm.irc.newui.group;

import androidx.databinding.ObservableList;

public interface Group {

    MasterGroup getParent();

    void setParent(MasterGroup group);


    String getName();

    ObservableList<ServerChannelPair> getChannels();

}
