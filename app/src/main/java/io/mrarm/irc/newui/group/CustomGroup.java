package io.mrarm.irc.newui.group;

import android.content.Context;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

public class CustomGroup extends BaseGroup {

    private String name;

    public CustomGroup(String name) {
        this.name = name;
    }

    @Override
    public String getName(Context ctx) {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
