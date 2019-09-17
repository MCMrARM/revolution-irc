package io.mrarm.irc.newui.group.view;

import androidx.databinding.ObservableList;

import java.util.List;

import io.mrarm.irc.newui.group.ServerChannelPair;

/**
 * This class represents a way to sort channels inside of a group.
 */
public interface GroupView {

    ObservableList<ServerChannelPair> getChannels();

    void setChannels(List<ServerChannelPair> servers);

    void addChannel(ServerChannelPair item);

    void removeChannel(ServerChannelPair item);

}
