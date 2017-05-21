package io.mrarm.irc;

import java.util.List;

public class ServerConnectionInfo {

    private String mName;
    private List<String> mChannels;
    private boolean mExpandedInDrawer = true;

    public ServerConnectionInfo(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public List<String> getChannels() {
        return mChannels;
    }

    public void setChannels(List<String> channels) {
        mChannels = channels;
    }

    public boolean isExpandedInDrawer() {
        return mExpandedInDrawer;
    }

    public void setExpandedInDrawer(boolean expanded) {
        mExpandedInDrawer = expanded;
    }

}
