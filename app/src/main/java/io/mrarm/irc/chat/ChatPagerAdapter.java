package io.mrarm.irc.chat;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

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

    public ChatPagerAdapter(Context context, FragmentManager fm, ServerConnectionInfo connectionInfo, Bundle bundle) {
        super(fm);
        this.context = context;
        this.connectionInfo = connectionInfo;
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            updateChannelList();
    }

    public void onSaveInstanceState(Bundle outBundle) {
        String[] keys = new String[channelIds.size()];
        long[] values = new long[channelIds.size()];
        Iterator<Map.Entry<String, Long>> it = channelIds.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            keys[i] = e.getKey();
            values[i] = e.getValue();
            ++i;
        }
        outBundle.putStringArray("channel_ids_keys", keys);
        outBundle.putLongArray("channel_ids_values", values);
    }

    public void onRestoreInstanceState(Bundle bundle) {
        String[] keys = bundle.getStringArray("channel_ids_keys");
        long[] values = bundle.getLongArray("channel_ids_values");
        if (keys != null && values != null && keys.length == values.length) {
            for (int i = keys.length - 1; i >= 0; --i) {
                channelIds.put(keys[i], values[i]);
                nextChannelId = Math.max(nextChannelId, values[i] + 1);
            }
        }
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
        return ChatMessagesFragment.newInstance(connectionInfo, channels.get(position - 1));
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
