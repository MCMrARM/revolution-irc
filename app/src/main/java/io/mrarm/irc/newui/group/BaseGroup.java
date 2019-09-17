package io.mrarm.irc.newui.group;

import android.content.Context;

import io.mrarm.irc.view.ServerIconView;

abstract class BaseGroup implements Group {

    private MasterGroup parent;

    @Override
    public ServerIconView.CustomizationInfo getIconCustomization(Context ctx) {
        return null;
    }

    @Override
    public MasterGroup getParent() {
        return parent;
    }

    @Override
    public void setParent(MasterGroup parent) {
        this.parent = parent;
    }

}
