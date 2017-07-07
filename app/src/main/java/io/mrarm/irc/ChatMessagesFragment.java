package io.mrarm.irc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.mrarm.chatlib.ChannelInfoListener;
import io.mrarm.chatlib.StatusMessageListener;
import io.mrarm.chatlib.dto.ChannelInfo;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageList;
import io.mrarm.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.dto.StatusMessageInfo;
import io.mrarm.chatlib.dto.StatusMessageList;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.message.MessageListener;
import io.mrarm.irc.util.LongPressSelectTouchListener;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.ScrollPosLinearLayoutManager;
import io.mrarm.irc.util.SettingsHelper;


public class ChatMessagesFragment extends Fragment implements StatusMessageListener,
        MessageListener, ChannelInfoListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "ChatMessagesFragment";

    private static final String ARG_SERVER_UUID = "server_uuid";
    private static final String ARG_DISPLAY_STATUS = "display_status";
    private static final String ARG_CHANNEL_NAME = "channel";

    private static final int LOAD_MORE_BEFORE_INDEX = 10;

    private List<NickWithPrefix> mMembers = null;

    private ServerConnectionInfo mConnection;
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

    public ChatMessagesFragment() {
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getParentFragment() != null) {
            Log.d(TAG, "setMembers " + (mMembers == null ? -1 : mMembers.size()));
            ((ChatFragment) getParentFragment()).setCurrentChannelMembers(mMembers);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        UUID connectionUUID = UUID.fromString(getArguments().getString(ARG_SERVER_UUID));
        ServerConnectionInfo connectionInfo = ServerConnectionManager.getInstance(getContext())
                .getConnection(connectionUUID);
        mConnection = connectionInfo;
        String channelName = getArguments().getString(ARG_CHANNEL_NAME);

        View rootView = inflater.inflate(R.layout.chat_messages_fragment, container, false);
        mRecyclerView = (RecyclerView) rootView;
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
                    Log.i(TAG, "Load more: " + channelName);
                    mIsLoadingMore = true;
                    connectionInfo.getApiInstance().getMessageStorageApi().getMessages(channelName,
                            100, mLoadMoreIdentifier, (MessageList messages) -> {
                                mRecyclerView.post(() -> {
                                    mAdapter.addMessagesToTop(messages.getMessages());
                                    mLoadMoreIdentifier = messages.getAfterIdentifier();
                                    mIsLoadingMore = false;
                                });
                            }, null);
                }
            }
        });

        SettingsHelper settingsHelper = SettingsHelper.getInstance(getContext());
        if (channelName != null) {
            mAdapter = new ChatMessagesAdapter(this, new ArrayList<>());
            mAdapter.setMessageFont(settingsHelper.getChatFont(), settingsHelper.getChatFontSize());
            mRecyclerView.setAdapter(mAdapter);
            LongPressSelectTouchListener selectTouchListener = new LongPressSelectTouchListener(mRecyclerView);
            mAdapter.setSelectListener(selectTouchListener);
            mRecyclerView.addOnItemTouchListener(selectTouchListener);

            Log.i(TAG, "Request message list for: " + channelName);
            connectionInfo.getApiInstance().getChannelInfo(channelName,
                    (ChannelInfo channelInfo) -> {
                        Log.i(TAG, "Got channel info " + channelName);
                        onMemberListChanged(channelInfo.getMembers());
                    }, null);

            connectionInfo.getApiInstance().subscribeChannelInfo(channelName, this, null, null);
            mNeedsUnsubscribeChannelInfo = true;

            connectionInfo.getApiInstance().getMessageStorageApi().getMessages(channelName, 100, null,
                    (MessageList messages) -> {
                        Log.i(TAG, "Got message list for " + channelName + ": " +
                                messages.getMessages().size() + " messages");
                        mMessages = messages.getMessages();
                        mNeedsUnsubscribeMessages = true;
                        getActivity().runOnUiThread(() -> {
                            mAdapter.setMessages(mMessages);
                            mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                            mLoadMoreIdentifier = messages.getAfterIdentifier();
                        });

                        connectionInfo.getApiInstance().getMessageStorageApi().subscribeChannelMessages(channelName, ChatMessagesFragment.this, null, null);
                    }, null);
        } else if (getArguments().getBoolean(ARG_DISPLAY_STATUS)) {
            mStatusAdapter = new ServerStatusMessagesAdapter(new StatusMessageList(new ArrayList<>()));
            mStatusAdapter.setMessageFont(settingsHelper.getChatFont(), settingsHelper.getChatFontSize());
            mRecyclerView.setAdapter(mStatusAdapter);

            Log.i(TAG, "Request status message list");
            connectionInfo.getApiInstance().getStatusMessages(100, null,
                    (StatusMessageList messages) -> {
                        Log.i(TAG, "Got server status message list: " +
                                messages.getMessages().size() + " messages");
                        mStatusMessages = messages.getMessages();
                        mNeedsUnsubscribeStatusMessages = true;
                        getActivity().runOnUiThread(() -> {
                            mStatusAdapter.setMessages(messages);
                            mRecyclerView.scrollToPosition(mStatusAdapter.getItemCount() - 1);
                        });

                        connectionInfo.getApiInstance().subscribeStatusMessages(ChatMessagesFragment.this, null, null);
                    }, null);
        }

        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.addPreferenceChangeListener(SettingsHelper.PREF_CHAT_FONT, this);
        s.addPreferenceChangeListener(SettingsHelper.PREF_CHAT_FONT_SIZE, this);

        return rootView;
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.removePreferenceChangeListener(SettingsHelper.PREF_CHAT_FONT, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_CHAT_FONT_SIZE, this);

        if (mNeedsUnsubscribeChannelInfo)
            mConnection.getApiInstance().unsubscribeChannelInfo(getArguments().getString(ARG_CHANNEL_NAME), ChatMessagesFragment.this, null, null);
        if (mNeedsUnsubscribeMessages)
            mConnection.getApiInstance().getMessageStorageApi().unsubscribeChannelMessages(getArguments().getString(ARG_CHANNEL_NAME), ChatMessagesFragment.this, null, null);
        if (mNeedsUnsubscribeStatusMessages)
            mConnection.getApiInstance().unsubscribeStatusMessages(ChatMessagesFragment.this, null, null);
    }

    private void scrollToBottom() {
        int i = Math.max(mLayoutManager.findLastVisibleItemPosition(), mLayoutManager.getPendingScrollPosition());
        int count = mAdapter == null ? mStatusAdapter.getItemCount() : mAdapter.getItemCount();
        if (i >= count - 2)
            mRecyclerView.scrollToPosition(count - 1);
    }

    @Override
    public void onMessage(String channel, MessageInfo messageInfo) {
        getActivity().runOnUiThread(() -> {
            mMessages.add(messageInfo);
            mAdapter.notifyItemInserted(mMessages.size() - 1);
            scrollToBottom();
        });
    }

    @Override
    public void onStatusMessage(StatusMessageInfo statusMessageInfo) {
        getActivity().runOnUiThread(() -> {
            mStatusMessages.add(statusMessageInfo);
            mStatusAdapter.notifyItemInserted(mStatusMessages.size() - 1);
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
                        .getServerConnectionData().getSupportedNickPrefixes()) {
                    if (leftPrefix == c)
                        return -1;
                    if (rightPrefix == c)
                        return 1;
                }
            } else if (left.getNickPrefixes() != null || right.getNickPrefixes() != null)
                return left.getNickPrefixes() != null ? -1 : 1;
            return left.getNick().compareTo(right.getNick());
        });
        if (getUserVisibleHint()) {
            getActivity().runOnUiThread(() -> {
                ((ChatFragment) getParentFragment()).setCurrentChannelMembers(mMembers);
            });
        }
    }

    public void showMessagesActionMenu() {
        if (mMessagesActionModeCallback == null)
            mMessagesActionModeCallback = new MessagesActionModeCallback();
        if (!mMessagesActionModeCallback.mShown)
            ((MainActivity) getActivity()).startSupportActionMode(mMessagesActionModeCallback);
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

        public boolean mShown = false;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_context_messages, menu);
            ((ChatFragment) getParentFragment()).setTabsHidden(true);
            mShown = true;
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
            mShown = false;
        }

    };

}