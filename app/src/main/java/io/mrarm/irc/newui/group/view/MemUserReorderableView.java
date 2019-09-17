package io.mrarm.irc.newui.group.view;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mrarm.irc.newui.group.ServerChannelPair;
import io.mrarm.irc.util.ListUtils;

public class MemUserReorderableView implements GroupView {

    private final Map<ServerChannelPair, Integer> mOrderMap = new HashMap<>();
    private final ObservableList<ServerChannelPair> mUserView = new ObservableArrayList<>();
    private final List<Integer> mUserViewIndexes = new ArrayList<>();

    @Override
    public ObservableList<ServerChannelPair> getChannels() {
        return mUserView;
    }

    @Override
    public void setChannels(List<ServerChannelPair> servers) {
        mUserView.clear();
        mUserViewIndexes.clear();
        for (ServerChannelPair s : servers)
            addChannel(s);
    }

    public void addChannel(ServerChannelPair item) {
        Integer val = mOrderMap.get(item);
        if (val == null) {
            val = mOrderMap.size();
            mOrderMap.put(item, mOrderMap.size());
        }
        int i = ListUtils.lowerBound(mUserViewIndexes, val);
        mUserView.add(i, item);
        mUserViewIndexes.add(i, val);
    }

    @Override
    public void removeChannel(ServerChannelPair item) {
        Integer val = mOrderMap.get(item);
        if (val == null)
            return;
        int i = Collections.binarySearch(mUserViewIndexes, val);
        if (i == -1)
            return;
        mUserView.remove(i);
        mUserViewIndexes.remove(i);
    }

    public void setOrder(List<ServerChannelPair> items) {
        mOrderMap.clear();
        int i = 0;
        for (ServerChannelPair p : items)
            mOrderMap.put(p, i++);

        setChannels(new ArrayList<>(items));
    }

}
