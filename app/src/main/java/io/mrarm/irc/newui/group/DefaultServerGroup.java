package io.mrarm.irc.newui.group;

import android.content.Context;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

public class DefaultServerGroup extends BaseGroup {

    private ServerGroupData mServerData;

    DefaultServerGroup(ServerGroupData data) {
        mServerData = data;
    }


    @Override
    public String getName(Context ctx) {
        return mServerData.getName();
    }

}
