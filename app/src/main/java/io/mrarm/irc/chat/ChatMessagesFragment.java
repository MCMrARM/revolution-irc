package io.mrarm.irc.chat;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.mrarm.chatlib.ChannelInfoListener;
import io.mrarm.chatlib.StatusMessageListener;
import io.mrarm.chatlib.dto.ChannelInfo;
import io.mrarm.chatlib.dto.MessageFilterOptions;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageList;
import io.mrarm.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.dto.StatusMessageInfo;
import io.mrarm.chatlib.dto.StatusMessageList;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.message.MessageListener;
import io.mrarm.irc.IRCChooserTargetService;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.util.LongPressSelectTouchListener;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.ScrollPosLinearLayoutManager;
import io.mrarm.irc.config.SettingsHelper;


public class ChatMessagesFragment extends Fragment implements StatusMessageListener,
        MessageListener, ChannelInfoListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "ChatMessagesFragment";

    private static final String ARG_SERVER_UUID = "server_uuid";
    private static final String ARG_DISPLAY_STATUS = "display_status";
    private static final String ARG_CHANNEL_NAME = "channel";

    private static final int LOAD_MORE_BEFORE_INDEX = 10;

    private static final MessageFilterOptions sFilterJoinParts;

    private List<NickWithPrefix> mMembers = null;

    private ServerConnectionInfo mConnection;
    private String mChannelName;
    private String mChannelTopic;
    private MessageSenderInfo mChannelTopicSetBy;
    private Date mChannelTopicSetOn;
    private RecyclerView mRecyclerView;
    private ScrollPosLinearLayoutManager mLayoutManager;
    private ChatMessagesAdapter mAdapter;
    private ServerStatusMessagesAdapter mStatusAdapter;
    private List<MessageInfo> mMessages;
    private List<StatusMessageInfo> mStatusMessages;
    private boolean mNeedsUnsubscribeChannelInfo = false;
    private boolean mNeedsUnsubscribeMessages = false;
    private boolean mNeedsUnsubscribeStatusMessages = false;
    private MessageListAfterIdentifier mLoadMoreIdentifier;
    private boolean mIsLoadingMore;
    private MessageFilterOptions mMessageFilterOptions;

    static {
        sFilterJoinParts = new MessageFilterOptions();
        sFilterJoinParts.excludeMessageTypes = new ArrayList<>();
        sFilterJoinParts.excludeMessageTypes.add(MessageInfo.MessageType.JOIN);
        sFilterJoinParts.excludeMessageTypes.add(MessageInfo.MessageType.PART);
        sFilterJoinParts.excludeMessageTypes.add(MessageInfo.MessageType.QUIT);
    }

    public ChatMessagesFragment() {
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getParentFragment() != null)
            updateParentCurrentChannel();
        if (!isVisibleToUser)
            hideMessagesActionMenu();
        if (mConnection != null && mChannelName != null) {
            mConnection.getNotificationManager().getChannelManager(mChannelName, true).setOpened(getContext(), isVisibleToUser);
        }
        if (mConnection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isVisibleToUser)
                IRCChooserTargetService.setChannel(mConnection.getUUID(), mChannelName);
            else
                IRCChooserTargetService.unsetChannel(mConnection.getUUID(), mChannelName);
        }
    }

    public static ChatMessagesFragment newInstance(ServerConnectionInfo server,
                                                   String channelName) {
        ChatMessagesFragment fragment = new ChatMessagesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, server.getUUID().toString());
        if (channelName != null)
            args.putString(ARG_CHANNEL_NAME, channelName);
        fragment.setArguments(args);
        return fragment;
    }

    public static ChatMessagesFragment newStatusInstance(ServerConnectionInfo server) {
        ChatMessagesFragment fragment = new ChatMessagesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, server.getUUID().toString());
        args.putBoolean(ARG_DISPLAY_STATUS, true);
        fragment.setArguments(args);
        return fragment;
    }

    private MessageFilterOptions getFilterOptions() {
        return mMessageFilterOptions;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UUID connectionUUID = UUID.fromString(getArguments().getString(ARG_SERVER_UUID));
        ServerConnectionInfo connectionInfo = ServerConnectionManager.getInstance(getContext())
                .getConnection(connectionUUID);
        mConnection = connectionInfo;
        mChannelName = getArguments().getString(ARG_CHANNEL_NAME);

        SettingsHelper settingsHelper = SettingsHelper.getInstance(getContext());
        if (mChannelName != null) {
            mAdapter = new ChatMessagesAdapter(this, new ArrayList<>());
            mAdapter.setMessageFont(settingsHelper.getChatFont(), settingsHelper.getChatFontSize());

            Log.i(TAG, "Request message list for: " + mChannelName);
            connectionInfo.getApiInstance().getChannelInfo(mChannelName,
                    (ChannelInfo channelInfo) -> {
                        Log.i(TAG, "Got channel info " + mChannelName);
                        mChannelTopic = channelInfo.getTopic();
                        mChannelTopicSetBy = channelInfo.getTopicSetBy();
                        mChannelTopicSetOn = channelInfo.getTopicSetOn();
                        onMemberListChanged(channelInfo.getMembers());
                    }, null);

            connectionInfo.getApiInstance().subscribeChannelInfo(mChannelName, this, null, null);
            mNeedsUnsubscribeChannelInfo = true;

            reloadMessages(settingsHelper);
        } else if (getArguments().getBoolean(ARG_DISPLAY_STATUS)) {
            mStatusAdapter = new ServerStatusMessagesAdapter(mConnection, new StatusMessageList(new ArrayList<>()));
            mStatusAdapter.setMessageFont(settingsHelper.getChatFont(), settingsHelper.getChatFontSize());

            Log.i(TAG, "Request status message list");
            connectionInfo.getApiInstance().getStatusMessages(100, null,
                    (StatusMessageList messages) -> {
                        Log.i(TAG, "Got server status message list: " +
                                messages.getMessages().size() + " messages");
                        mStatusMessages = messages.getMessages();
                        mNeedsUnsubscribeStatusMessages = true;
                        updateMessageList(() -> {
                            mStatusAdapter.setMessages(messages);
                            if (mRecyclerView != null)
                                mRecyclerView.scrollToPosition(mStatusAdapter.getItemCount() - 1);
                        });

                        connectionInfo.getApiInstance().subscribeStatusMessages(ChatMessagesFragment.this, null, null);
                    }, null);
        }

        settingsHelper.addPreferenceChangeListener(SettingsHelper.PREF_CHAT_FONT, this);
        settingsHelper.addPreferenceChangeListener(SettingsHelper.PREF_CHAT_FONT_SIZE, this);
        settingsHelper.addPreferenceChangeListener(SettingsHelper.PREF_CHAT_HIDE_JOIN_PART, this);
        // it's enough to only register to the last format preference, as all preferences are always rewritten
        settingsHelper.addPreferenceChangeListener(SettingsHelper.PREF_MESSAGE_FORMAT_EVENT_HOSTNAME, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.removePreferenceChangeListener(SettingsHelper.PREF_CHAT_FONT, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_CHAT_FONT_SIZE, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_CHAT_HIDE_JOIN_PART, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_MESSAGE_FORMAT_EVENT_HOSTNAME, this);

        if (mNeedsUnsubscribeChannelInfo)
            mConnection.getApiInstance().unsubscribeChannelInfo(getArguments().getString(ARG_CHANNEL_NAME), ChatMessagesFragment.this, null, null);
        if (mNeedsUnsubscribeMessages)
            mConnection.getApiInstance().getMessageStorageApi().unsubscribeChannelMessages(getArguments().getString(ARG_CHANNEL_NAME), ChatMessagesFragment.this, null, null);
        if (mNeedsUnsubscribeStatusMessages)
            mConnection.getApiInstance().unsubscribeStatusMessages(ChatMessagesFragment.this, null, null);

        hideMessagesActionMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.chat_messages_fragment, container, false);
        synchronized (this) {
            mRecyclerView = (RecyclerView) rootView;
        }
        mLayoutManager = new ScrollPosLinearLayoutManager(getContext());
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int firstVisible = mLayoutManager.findFirstVisibleItemPosition();
                if (firstVisible >= 0 && firstVisible < LOAD_MORE_BEFORE_INDEX) {
                    if (mIsLoadingMore || mLoadMoreIdentifier == null || !mAdapter.hasMessages())
                        return;
                    Log.i(TAG, "Load more: " + mChannelName);
                    mIsLoadingMore = true;
                    mConnection.getApiInstance().getMessageStorageApi().getMessages(mChannelName,
                            100, getFilterOptions(), mLoadMoreIdentifier,
                            (MessageList messages) -> {
                                updateMessageList(() -> {
                                    mAdapter.addMessagesToTop(messages.getMessages());
                                    mLoadMoreIdentifier = messages.getAfterIdentifier();
                                    mIsLoadingMore = false;
                                });
                            }, null);
                }
            }
        });

        if (mAdapter != null) {
            mRecyclerView.setAdapter(mAdapter);
            if (SettingsHelper.getInstance(getContext()).getChatUseMutliSelect()) {
                LongPressSelectTouchListener selectTouchListener =
                        new LongPressSelectTouchListener(mRecyclerView);
                mAdapter.setMultiSelectListener(selectTouchListener);
                mRecyclerView.addOnItemTouchListener(selectTouchListener);
            } else {
                ChatSelectTouchListener selectTouchListener =
                        new ChatSelectTouchListener(mRecyclerView);
                mAdapter.setSelectListener(selectTouchListener);
                mRecyclerView.addOnItemTouchListener(selectTouchListener);
            }
        } else if (mStatusAdapter != null) {
            mRecyclerView.setAdapter(mStatusAdapter);
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        synchronized (this) {
            mRecyclerView = null;
        }
        if (mAdapter != null) {
            mAdapter.setSelectListener(null);
        }
        if (mConnection != null && getUserVisibleHint() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            IRCChooserTargetService.unsetChannel(mConnection.getUUID(), mChannelName);
    }

    private void reloadMessages(SettingsHelper settingsHelper) {
        if (settingsHelper.shouldHideJoinPartMessages())
            mMessageFilterOptions = sFilterJoinParts;
        else
            mMessageFilterOptions = null;
        mConnection.getApiInstance().getMessageStorageApi().getMessages(mChannelName, 100,
                getFilterOptions(), null, (MessageList messages) -> {
                    Log.i(TAG, "Got message list for " + mChannelName + ": " +
                            messages.getMessages().size() + " messages");
                    mMessages = messages.getMessages();
                    updateMessageList(() -> {
                        mAdapter.setMessages(mMessages);
                        if (mRecyclerView != null)
                            mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                        mLoadMoreIdentifier = messages.getAfterIdentifier();
                    });

                    if (!mNeedsUnsubscribeMessages) {
                        mConnection.getApiInstance().getMessageStorageApi().subscribeChannelMessages(mChannelName, ChatMessagesFragment.this, null, null);
                        mNeedsUnsubscribeMessages = true;
                    }
                }, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getUserVisibleHint())
            mConnection.getNotificationManager().getChannelManager(mChannelName, true).setOpened(getContext(), true);
        if (mConnection != null && getUserVisibleHint() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            IRCChooserTargetService.setChannel(mConnection.getUUID(), mChannelName);
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity activity = (MainActivity) getActivity();
        if (getUserVisibleHint() && (activity == null || !activity.isAppExiting()))
            mConnection.getNotificationManager().getChannelManager(mChannelName, true).setOpened(getContext(), false);
        if (mConnection != null && getUserVisibleHint() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            IRCChooserTargetService.setChannel(mConnection.getUUID(), mChannelName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SettingsHelper settingsHelper = SettingsHelper.getInstance(getContext());
        if (mAdapter != null) {
            mAdapter.setMessageFont(settingsHelper.getChatFont(), settingsHelper.getChatFontSize());
            mAdapter.notifyDataSetChanged();
        }
        if (mStatusAdapter != null) {
            mStatusAdapter.setMessageFont(settingsHelper.getChatFont(), settingsHelper.getChatFontSize());
            mStatusAdapter.notifyDataSetChanged();
        }
        if (settingsHelper.shouldHideJoinPartMessages() != (mMessageFilterOptions != null) &&
                mChannelName != null) {
            reloadMessages(settingsHelper);
        }
    }

    private void updateParentCurrentChannel() {
        Activity activity = getActivity();
        if (activity == null)
            return;
        activity.runOnUiThread(() -> ((ChatFragment) getParentFragment())
                .setCurrentChannelInfo(mChannelTopic,
                        mChannelTopicSetBy != null ? mChannelTopicSetBy.getNick() : null,
                        mChannelTopicSetOn, mMembers));
    }

    private void updateMessageList(Runnable r) {
        synchronized (this) {
            if (mRecyclerView != null) {
                mRecyclerView.post(r);
            } else {
                r.run();
            }
        }
    }

    public ServerConnectionInfo getConnectionInfo() {
        return mConnection;
    }

    public String getChannelName() {
        return mChannelName;
    }

    public boolean isServerStatus() {
        return mStatusAdapter != null;
    }

    private void scrollToBottom() {
        int i = Math.max(mLayoutManager.findLastVisibleItemPosition(), mLayoutManager.getPendingScrollPosition());
        int count = mAdapter == null ? mStatusAdapter.getItemCount() : mAdapter.getItemCount();
        if (i >= count - 2)
            mRecyclerView.scrollToPosition(count - 1);
    }

    @Override
    public void onMessage(String channel, MessageInfo messageInfo) {
        updateMessageList(() -> {
            MessageFilterOptions opt = getFilterOptions();
            if (opt != null) {
                if (opt.restrictToMessageTypes != null &&
                        !opt.restrictToMessageTypes.contains(messageInfo.getType()))
                    return;
                if (opt.excludeMessageTypes != null &&
                        opt.excludeMessageTypes.contains(messageInfo.getType()))
                    return;
            }

            mMessages.add(messageInfo);
            mAdapter.notifyItemInserted(mMessages.size() - 1);
            if (mRecyclerView != null)
                scrollToBottom();
        });
    }

    @Override
    public void onStatusMessage(StatusMessageInfo statusMessageInfo) {
        updateMessageList(() -> {
            mStatusMessages.add(statusMessageInfo);
            mStatusAdapter.notifyItemInserted(mStatusMessages.size() - 1);
            if (mRecyclerView != null)
                scrollToBottom();
        });
    }

    @Override
    public void onMemberListChanged(List<NickWithPrefix> list) {
        this.mMembers = list;
        Collections.sort(list, (NickWithPrefix left, NickWithPrefix right) -> {
            if (left.getNickPrefixes() != null && right.getNickPrefixes() != null) {
                char leftPrefix = left.getNickPrefixes().get(0);
                char rightPrefix = right.getNickPrefixes().get(0);
                for (char c : ((ServerConnectionApi) mConnection.getApiInstance())
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
        if (getUserVisibleHint())
            updateParentCurrentChannel();
    }

    @Override
    public void onTopicChanged(String topic, MessageSenderInfo topicSetBy, Date topicSetOn) {
        mChannelTopic = topic;
        mChannelTopicSetBy = topicSetBy;
        mChannelTopicSetOn = topicSetOn;
        if (getUserVisibleHint())
            updateParentCurrentChannel();
    }

    public void showMessagesActionMenu() {
        if (mMessagesActionModeCallback == null)
            mMessagesActionModeCallback = new MessagesActionModeCallback();
        if (mMessagesActionModeCallback.mActionMode == null)
            mMessagesActionModeCallback.mActionMode = ((MainActivity) getActivity()).startSupportActionMode(mMessagesActionModeCallback);
    }

    public void hideMessagesActionMenu() {
        if (mMessagesActionModeCallback != null && mMessagesActionModeCallback.mActionMode != null) {
            mMessagesActionModeCallback.mActionMode.finish();
            mMessagesActionModeCallback.mActionMode = null;
        }
    }

    public void copySelectedMessages() {
        Set<Integer> items = mAdapter.getSelectedItems();
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean first = true;
        for (Integer msgIndex : items) {
            if (first)
                first = false;
            else
                builder.append('\n');
            builder.append(MessageBuilder.getInstance(getContext()).buildMessage(mMessages.get(msgIndex)));
        }
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("IRC Messages", builder));
    }

    private MessagesActionModeCallback mMessagesActionModeCallback;

    private class MessagesActionModeCallback implements ActionMode.Callback {

        public ActionMode mActionMode;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_context_messages, menu);
            ((ChatFragment) getParentFragment()).setTabsHidden(true);
            mActionMode = mode;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_copy:
                    copySelectedMessages();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ((ChatFragment) getParentFragment()).setTabsHidden(false);
            mAdapter.clearSelection(mRecyclerView);
            mActionMode = null;
        }

    };

}