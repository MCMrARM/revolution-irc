package io.mrarm.irc.newui.group.v2;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

public class MasterGroup {

    private final ObservableList<Group> groups = new ObservableArrayList<>();

    public ObservableList<Group> getGroups() {
        return groups;
    }

    public void add(Group g) {
        if (g.getParent() != null)
            throw new RuntimeException("Group already has a parent");
        groups.add(g);
        g.setParent(this);
    }

}
