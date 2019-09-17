package io.mrarm.irc.newui.group;

import android.content.Context;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

public class DefaultServerGroup extends BaseGroup {

    private ServerGroupData mServerData;
    private final ObservableList<ServerChannelPair> channels = new ObservableArrayList<>();

    DefaultServerGroup(ServerGroupData data) {
        mServerData = data;
    }


    @Override
    public String getName(Context ctx) {
        return mServerData.getName();
    }

    @Override
    public ObservableList<ServerChannelPair> getChannels() {
        return channels;
    }

}
