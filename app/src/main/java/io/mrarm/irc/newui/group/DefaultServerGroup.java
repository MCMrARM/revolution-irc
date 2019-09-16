package io.mrarm.irc.newui.group;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

public class DefaultServerGroup extends BaseGroup {

    private ServerGroupData mServerData;
    private final ObservableList<ServerChannelPair> channels = new ObservableArrayList<>();

    DefaultServerGroup(ServerGroupData data) {
        mServerData = data;
    }


    @Override
    public String getName() {
        return mServerData.getName();
    }

    @Override
    public ObservableList<ServerChannelPair> getChannels() {
        return channels;
    }

}
