package io.mrarm.irc.chat;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class ChatPagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private ServerConnectionInfo connectionInfo;
    private List<String> channels;
    private Map<String, Long> channelIds = new HashMap<>();
    private long nextChannelId = 1;

    public ChatPagerAdapter(Context context, FragmentManager fm, ServerConnectionInfo connectionInfo) {
        super(fm);
        this.context = context;
        this.connectionInfo = connectionInfo;
        updateChannelList();
    }

    public void updateChannelList() {
        channels = connectionInfo.getChannels();
        Iterator<Map.Entry<String, Long>> it = channelIds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (!channels.contains(entry.getKey()))
                it.remove();
        }
        for (String channel : channels) {
            if (!channelIds.containsKey(channel))
                channelIds.put(channel, nextChannelId++);
        }
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0)
            return ChatMessagesFragment.newStatusInstance(connectionInfo);
        return ChatMessagesFragment.newInstance(connectionInfo,
                connectionInfo.getChannels().get(position - 1));
    }

    @Override
    public int getItemPosition(Object object) {
        if (object instanceof ChatMessagesFragment) {
            ChatMessagesFragment fragment = (ChatMessagesFragment) object;
            if (fragment.isServerStatus())
                return POSITION_UNCHANGED;
            int iof = channels.indexOf(fragment.getChannelName());
            if (iof == -1)
                return POSITION_NONE;
            return iof + 1;
        }
        return super.getItemPosition(object);
    }

    @Override
    public long getItemId(int position) {
        if (position == 0)
            return 0;
        return channelIds.get(getChannel(position));
    }

    @Override
    public int getCount() {
        if (channels == null)
            return 1;
        return channels.size() + 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0)
            return context.getString(R.string.tab_server);
        return channels.get(position - 1);
    }

    public String getChannel(int position) {
        if (position == 0)
            return null;
        return channels.get(position - 1);
    }

    public int findChannel(String channel) {
        return channels.indexOf(channel) + 1;
    }

}
