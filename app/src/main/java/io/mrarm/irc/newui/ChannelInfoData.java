package io.mrarm.irc.newui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.mrarm.chatlib.ChannelInfoListener;
import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.util.UiThreadHelper;

/**
 * This class wraps around chatapi's interface for getting channel info.
 * It is useful because it provides a thread-safe way to access the channel info and listen to
 * specific events of use to the UI.
 * This class additionally manages sorting members by rank and name.
 *
 * TODO: Maybe merge this with GroupChannelListData.ChannelEntry
 */
public class ChannelInfoData implements ChannelInfoListener {

    private final ServerConnectionInfo mConnection;
    private final String mChannel;
    private List<NickWithPrefix> mMembers;
    private String mTopic;
    private final List<MemberListListener> mMemberListListeners = new ArrayList<>();
    private boolean mSortMembers = false;


    public ChannelInfoData(ServerConnectionInfo connection, String channel) {
        mConnection = connection;
        mChannel = channel;
    }

    public void load() {
        ChatApi api = mConnection.getApiInstance();
        if (api != null) {
            api.subscribeChannelInfo(mChannel, this, null, null);
            api.getChannelInfo(mChannel, (ci) -> {
                onMemberListChanged(ci.getMembers());
                onTopicChanged(ci.getTopic(), ci.getTopicSetBy(), ci.getTopicSetOn());
            }, null);
        }
    }

    public void unload() {
        ChatApi api = mConnection.getApiInstance();
        if (api != null) {
            api.unsubscribeChannelInfo(mChannel, this, null, null);
        }
    }

    public void setSortMembers(boolean value) {
        mSortMembers = value;
    }

    public void addMemberListListener(MemberListListener listener) {
        mMemberListListeners.add(listener);
    }

    public void removeMemberListListener(MemberListListener listener) {
        mMemberListListeners.remove(listener);
    }

    public List<NickWithPrefix> getMembers() {
        return mMembers;
    }

    public String getTopic() {
        return mTopic;
    }

    public static void sortMembers(ServerConnectionInfo connection, List<NickWithPrefix> list) {
        Collections.sort(list, (NickWithPrefix left, NickWithPrefix right) -> {
            if (left.getNickPrefixes() != null && right.getNickPrefixes() != null) {
                char leftPrefix = left.getNickPrefixes().get(0);
                char rightPrefix = right.getNickPrefixes().get(0);
                for (char c : ((ServerConnectionApi) connection.getApiInstance())
                        .getServerConnectionData().getSupportList().getSupportedNickPrefixes()) {
                    if (leftPrefix == c && rightPrefix != c)
                        return -1;
                    if (rightPrefix == c && leftPrefix != c)
                        return 1;
                }
            } else if (left.getNickPrefixes() != null || right.getNickPrefixes() != null)
                return left.getNickPrefixes() != null ? -1 : 1;
            return left.getNick().compareToIgnoreCase(right.getNick());
        });
    }


    @Override
    public void onMemberListChanged(List<NickWithPrefix> list) {
        if (mSortMembers) {
            list = new ArrayList<>(list);
            sortMembers(mConnection, list);
        }
        final List<NickWithPrefix> finalList = list;
        UiThreadHelper.runOnUiThread(() -> {
            mMembers = finalList;
            for (MemberListListener listener : mMemberListListeners)
                listener.onMemberListChanged(this);
        });
    }

    @Override
    public void onTopicChanged(String topic, MessageSenderInfo setBy, Date setOn) {
        UiThreadHelper.runOnUiThread(() -> mTopic = topic);
    }

    public interface MemberListListener {

        void onMemberListChanged(ChannelInfoData data);

    }

}