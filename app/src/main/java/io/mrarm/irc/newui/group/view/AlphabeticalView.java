package io.mrarm.irc.newui.group.view;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.mrarm.irc.newui.group.ServerChannelPair;
import io.mrarm.irc.util.ListUtils;

public class AlphabeticalView implements GroupView {

    private final ObservableList<ServerChannelPair> channels = new ObservableArrayList<>();

    @Override
    public ObservableList<ServerChannelPair> getChannels() {
        return channels;
    }

    @Override
    public void setChannels(List<ServerChannelPair> servers) {
        channels.clear();
        List<ServerChannelPair> tmp = new ArrayList<>(servers);
        Collections.sort(tmp, SimpleAlphabeticalComparator.INSTANCE);
        channels.addAll(tmp);
    }

    @Override
    public void addChannel(ServerChannelPair item) {
        int i = ListUtils.lowerBound(channels, item, SimpleAlphabeticalComparator.INSTANCE);
        channels.add(i, item);
    }

    @Override
    public void removeChannel(ServerChannelPair item) {
        channels.remove(item);
    }

    private static class SimpleAlphabeticalComparator implements Comparator<ServerChannelPair> {

        private static final SimpleAlphabeticalComparator INSTANCE =
                new SimpleAlphabeticalComparator();

        @Override
        public int compare(ServerChannelPair o1, ServerChannelPair o2) {
            return o1.getChannel().compareTo(o2.getChannel());
        }

    }


}
