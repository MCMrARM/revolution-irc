package io.mrarm.irc.newui.group;

abstract class BaseGroup implements Group {

    private MasterGroup parent;

    @Override
    public MasterGroup getParent() {
        return parent;
    }

    @Override
    public void setParent(MasterGroup parent) {
        this.parent = parent;
    }

}
