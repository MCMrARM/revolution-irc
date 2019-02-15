package io.mrarm.irc.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.ChannelInfoListener;
import io.mrarm.chatlib.ResponseCallback;
import io.mrarm.chatlib.StatusMessageListener;
import io.mrarm.chatlib.dto.ChannelInfo;
import io.mrarm.chatlib.dto.MessageFilterOptions;
import io.mrarm.chatlib.dto.MessageId;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageList;
import io.mrarm.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.dto.StatusMessageInfo;
import io.mrarm.chatlib.dto.StatusMessageList;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.message.MessageListener;
import io.mrarm.chatlib.message.MessageStorageApi;
import io.mrarm.irc.ChannelNotificationManager;
import io.mrarm.irc.IRCChooserTargetService;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.NotificationManager;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.ChatSettings;
import io.mrarm.irc.config.MessageFormatSettings;
import io.mrarm.irc.config.UiSettingChangeCallback;
import io.mrarm.irc.util.LongPressSelectTouchListener;
import io.mrarm.irc.util.ScrollPosLinearLayoutManager;
import io.mrarm.irc.config.SettingsHelper;


public class ChatMessagesFragment extends Fragment implements StatusMessageListener,
        MessageListener, ChannelInfoListener, NotificationManager.UnreadMessageCountCallback {

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
    private List<StatusMessageInfo> mStatusMessages;
    private boolean mNeedsUnsubscribeChannelInfo = false;
    private boolean mNeedsUnsubscribeMessages = false;
    private boolean mNeedsUnsubscribeStatusMessages = false;
    private MessageListAfterIdentifier mLoadOlderIdentifier;
    private MessageListAfterIdentifier mLoadNewerIdentifier;
    private boolean mIsLoadingMore;
    private MessageFilterOptions mMessageFilterOptions;
    private View mUnreadCtr;
    private TextView mUnreadText;
    private View mUnreadDiscard;
    private long mUnreadCheckedFirst = -1;
    private long mUnreadCheckedLast = -1;
    private MessageId mUnreadCheckFor;

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
        if (isVisibleToUser && getParentFragment() != null)
            ((ChatFragment) getParentFragment()).getSendMessageHelper()
                    .setCurrentChannel(mChannelName);
        if (!isVisibleToUser) {
            hideMessagesActionMenu();
        }
        if (mConnection != null && mChannelName != null) {
            mConnection.getNotificationManager().getChannelManager(mChannelName, true).setOpened(getContext(), isVisibleToUser);

            if (isVisibleToUser) {
                updateUnreadCounter();
                mConnection.getNotificationManager().addUnreadMessageCountCallback(this);
            } else {
                mConnection.getNotificationManager().removeUnreadMessageCountCallback(this);
            }
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

        if (mChannelName != null) {
            mAdapter = new ChatMessagesAdapter(this, new ArrayList<>(), new ArrayList<>());
            mAdapter.setMessageFont(ChatSettings.getFont(), ChatSettings.getFontSize());

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

            String msgIdStr = ((ChatFragment) getParentFragment()).getAndClearMessageJump(mChannelName);
            MessageId msgId = null;
            if (msgIdStr != null)
                msgId = mConnection.getApiInstance().getMessageStorageApi().getMessageIdParser().parse(msgIdStr);
            reloadMessages(msgId);
        } else if (getArguments().getBoolean(ARG_DISPLAY_STATUS)) {
            mStatusAdapter = new ServerStatusMessagesAdapter(mConnection, new StatusMessageList(new ArrayList<>()));
            mStatusAdapter.setMessageFont(ChatSettings.getFont(), ChatSettings.getFontSize());

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

        SettingsHelper.registerCallbacks(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SettingsHelper.unregisterCallbacks(this);

        if (mNeedsUnsubscribeChannelInfo)
            mConnection.getApiInstance().unsubscribeChannelInfo(getArguments().getString(ARG_CHANNEL_NAME), ChatMessagesFragment.this, null, null);
        if (mNeedsUnsubscribeMessages)
            mConnection.getApiInstance().getMessageStorageApi().unsubscribeChannelMessages(getArguments().getString(ARG_CHANNEL_NAME), ChatMessagesFragment.this, null, null);
        if (mNeedsUnsubscribeStatusMessages)
            mConnection.getApiInstance().unsubscribeStatusMessages(ChatMessagesFragment.this, null, null);

        mConnection.getNotificationManager().removeUnreadMessageCountCallback(this);

        hideMessagesActionMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.chat_messages_fragment, container, false);
        synchronized (this) {
            mRecyclerView = rootView.findViewById(R.id.messages);
            mUnreadCtr = rootView.findViewById(R.id.unread_counter_ctr);
            mUnreadText = rootView.findViewById(R.id.unread_counter);
            mUnreadDiscard = rootView.findViewById(R.id.unread_counter_discard);
        }
        mLayoutManager = new ScrollPosLinearLayoutManager(getContext());
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mAdapter == null)
                    return;
                checkForUnreadMessages();
                int firstVisible = mLayoutManager.findFirstVisibleItemPosition();
                if (firstVisible >= 0 && firstVisible < LOAD_MORE_BEFORE_INDEX) {
                    if (mIsLoadingMore || mLoadOlderIdentifier == null || !mAdapter.hasMessages())
                        return;
                    Log.i(TAG, "Load more (older): " + mChannelName);
                    mIsLoadingMore = true;
                    mConnection.getApiInstance().getMessageStorageApi().getMessages(mChannelName,
                            100, getFilterOptions(), mLoadOlderIdentifier,
                            (MessageList messages) -> {
                                updateMessageList(() -> {
                                    mAdapter.addMessagesToTop(messages.getMessages(),
                                            messages.getMessageIds());
                                    mLoadOlderIdentifier = messages.getOlder();
                                    mIsLoadingMore = false;
                                });
                            }, null);
                }
                int lastVisible = mLayoutManager.findLastVisibleItemPosition();
                if (lastVisible <= mAdapter.getItemCount() &&
                        lastVisible > mAdapter.getItemCount() - LOAD_MORE_BEFORE_INDEX) {
                    if (mIsLoadingMore || mLoadNewerIdentifier == null || !mAdapter.hasMessages())
                        return;
                    Log.i(TAG, "Load more (newer): " + mChannelName);
                    mIsLoadingMore = true;
                    mConnection.getApiInstance().getMessageStorageApi().getMessages(mChannelName,
                            100, getFilterOptions(), mLoadNewerIdentifier,
                            (MessageList messages) -> {
                                updateMessageList(() -> {
                                    mAdapter.addMessagesToBottom(messages.getMessages(),
                                            messages.getMessageIds());
                                    mLoadNewerIdentifier = messages.getNewer();
                                    mIsLoadingMore = false;
                                });
                            }, null);
                }
            }
        });

        mUnreadCtr.setOnClickListener((v) -> {
            ChannelNotificationManager mgr = mConnection.getNotificationManager().getChannelManager(mChannelName, true);
            MessageId msgId = mgr.getFirstUnreadMessage();
            int index = mAdapter.findMessageWithId(msgId);
            if (index != -1)
                ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(index, 0);
            else
                reloadMessages(msgId);
            mgr.clearUnreadMessages();
        });
        mUnreadDiscard.setOnClickListener((v) -> {
            ChannelNotificationManager mgr = mConnection.getNotificationManager().getChannelManager(mChannelName, true);
            mgr.clearUnreadMessages();
        });

        if (mAdapter != null) {
            mRecyclerView.setAdapter(mAdapter);

            LongPressSelectTouchListener selectTouchListener =
                    new LongPressSelectTouchListener(mRecyclerView);
            mAdapter.setMultiSelectListener(selectTouchListener);
            mRecyclerView.addOnItemTouchListener(selectTouchListener);

            if (!ChatSettings.shouldUseOnlyMultiSelectMode()) {
                ChatSelectTouchListener newSelectTouchListener =
                        new ChatSelectTouchListener(mRecyclerView);
                newSelectTouchListener.setMultiSelectListener(selectTouchListener);
                newSelectTouchListener.setActionModeStateCallback((android.view.ActionMode actionMode,
                                                                boolean b) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            actionMode.getType() == android.view.ActionMode.TYPE_FLOATING)
                        return;
                    ((ChatFragment) getParentFragment()).setTabsHidden(b);
                });
                mAdapter.setSelectListener(newSelectTouchListener);
                mRecyclerView.addOnItemTouchListener(newSelectTouchListener);
            }
        } else if (mStatusAdapter != null) {
            mRecyclerView.setAdapter(mStatusAdapter);
        }

        if (getUserVisibleHint())
            ((ChatFragment) getParentFragment()).getSendMessageHelper()
                    .setCurrentChannel(mChannelName);

        updateUnreadCounter();

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

    private void reloadMessages(MessageId nearMessage) {
        if (ChatSettings.shouldHideJoinPartMessages())
            mMessageFilterOptions = sFilterJoinParts;
        else
            mMessageFilterOptions = null;
        mUnreadCheckedFirst = -1;
        mUnreadCheckedLast = -1;
        mAdapter.setNewMessagesStart(mConnection.getNotificationManager()
                .getChannelManager(mChannelName, true).getFirstUnreadMessage());
        ResponseCallback<MessageList> cb = (MessageList messages) -> {
            Log.i(TAG, "Got message list for " + mChannelName + ": " +
                    messages.getMessages().size() + " messages");
            updateMessageList(() -> {
                mAdapter.setMessages(messages.getMessages(), messages.getMessageIds());
                if (mRecyclerView != null)
                    mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                mLoadOlderIdentifier = messages.getOlder();
            });

            if (!mNeedsUnsubscribeMessages) {
                mConnection.getApiInstance().getMessageStorageApi().subscribeChannelMessages(mChannelName, ChatMessagesFragment.this, null, null);
                mNeedsUnsubscribeMessages = true;
            }
        };
        MessageStorageApi storage = mConnection.getApiInstance().getMessageStorageApi();
        if (nearMessage != null) {
            storage.getMessagesNear(mChannelName, nearMessage,
                    getFilterOptions(), (MessageList messages) -> {
                        cb.onResponse(messages);
                        List<MessageId> ids = messages.getMessageIds();
                        updateMessageList(() -> {
                            for (int i = ids.size() - 1; i >= 0; --i) {
                                if (ids.get(i).equals(nearMessage)) {
                                    // TODO: Is this behaviour reliable? It looks random to me, and doesn't seem to match what the docs define :/
                                    ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(i, 0);
                                }
                            }
                            mLoadNewerIdentifier = messages.getNewer();
                        });
                    }, null);
        } else {
            mConnection.getApiInstance().getMessageStorageApi().getMessages(mChannelName, 100,
                    getFilterOptions(), null, cb, null);
        }
    }

    private void updateUnreadCounter() {
        if (mConnection == null || mRecyclerView == null)
            return;
        ChannelNotificationManager mgr = mConnection.getNotificationManager().getChannelManager(mChannelName, true);
        int unread = mgr.getUnreadMessageCount();
        MessageId unreadMsg = mgr.getFirstUnreadMessage();
        if (unreadMsg == null && unread > 0) {
            unread = 0;
            mgr.clearUnreadMessages();
        }
        if (unread > 0) {
            int index = mAdapter.findMessageWithId(unreadMsg);
            View v = mRecyclerView.getLayoutManager().findViewByPosition(index);
            if (v != null && mRecyclerView.getLayoutManager().isViewPartiallyVisible(v, true, true)) {
                unread = 0;
                mgr.clearUnreadMessages();
            }
            mAdapter.setNewMessagesStart(unreadMsg);
        }
        mUnreadCtr.setVisibility(View.GONE);
        if (unread > 0) {
            if (mUnreadCheckFor == null || !mUnreadCheckFor.equals(unreadMsg)) {
                mUnreadCheckFor = unreadMsg;
                mUnreadCheckedFirst = -1;
                mUnreadCheckedLast = -1;
            }
            mUnreadCtr.setVisibility(View.VISIBLE);
            mUnreadText.setText(getResources().getQuantityString(R.plurals.unread_message_counter, unread, unread));
        }
    }

    private void checkForUnreadMessages() {
        if (mUnreadCtr.getVisibility() == View.GONE)
            return;
        LinearLayoutManager llm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int firstPos = llm.findFirstCompletelyVisibleItemPosition();
        int lastPos = llm.findLastCompletelyVisibleItemPosition();
        long firstId = firstPos != RecyclerView.NO_POSITION ? mAdapter.getItemId(firstPos) : -1;
        long lastId = lastPos != RecyclerView.NO_POSITION ? mAdapter.getItemId(lastPos) : -1;
        boolean found = false;
        if (mUnreadCheckedFirst == -1 && firstId != -1) {
            mUnreadCheckedFirst = firstId;
            mUnreadCheckedLast = firstId;
            found = checkItemForUnread(
                    mAdapter.getMessage(mAdapter.getItemPosition(firstId)), mUnreadCheckFor);
        }
        while (firstId != -1 && firstId < mUnreadCheckedFirst) {
            found |= checkItemForUnread(mAdapter.getMessage(
                    mAdapter.getItemPosition(mUnreadCheckedFirst)), mUnreadCheckFor);
            if (found)
                break;
            --mUnreadCheckedFirst;
        }
        while (lastId != -1 && lastId > mUnreadCheckedLast) {
            found |= checkItemForUnread(mAdapter.getMessage(
                    mAdapter.getItemPosition(mUnreadCheckedLast)), mUnreadCheckFor);
            if (found)
                break;
            ++mUnreadCheckedLast;
        }
        if (found) {
            ChannelNotificationManager mgr = mConnection.getNotificationManager()
                    .getChannelManager(mChannelName, true);
            mgr.clearUnreadMessages();
            mUnreadCtr.setVisibility(View.GONE);
            mUnreadCheckedFirst = -1;
            mUnreadCheckedLast = -1;
        }
    }

    private boolean checkItemForUnread(ChatMessagesAdapter.Item item, MessageId lookingFor) {
        if (item instanceof ChatMessagesAdapter.MessageItem) {
            return ((ChatMessagesAdapter.MessageItem) item).mMessageId.equals(lookingFor);
        }
        return false;
    }

    @Override
    public void onUnreadMessageCountChanged(ServerConnectionInfo info, String channel, int messageCount, int oldMessageCount) {
        if (channel.equals(mChannelName)) {
            updateMessageList(this::updateUnreadCounter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getUserVisibleHint()) {
            mConnection.getNotificationManager().getChannelManager(mChannelName, true).setOpened(getContext(), true);
            mConnection.getNotificationManager().addUnreadMessageCountCallback(this);
            updateUnreadCounter();
        }
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
        mConnection.getNotificationManager().removeUnreadMessageCountCallback(this);
        mUnreadCtr.setVisibility(View.GONE);
    }

    @UiSettingChangeCallback(keys = {
            ChatSettings.PREF_FONT,
            ChatSettings.PREF_FONT_SIZE,
            ChatSettings.PREF_HIDE_JOIN_PART_MESSAGES,
            // it's enough to only register to the last format preference, as all preferences are always rewritten
            MessageFormatSettings.PREF_MESSAGE_FORMAT_EVENT_HOSTNAME
    })
    private void onSettingChanged() {
        if (mAdapter != null) {
            mAdapter.setMessageFont(ChatSettings.getFont(), ChatSettings.getFontSize());
            mAdapter.notifyDataSetChanged();
        }
        if (mStatusAdapter != null) {
            mStatusAdapter.setMessageFont(ChatSettings.getFont(), ChatSettings.getFontSize());
            mStatusAdapter.notifyDataSetChanged();
        }
        if (ChatSettings.shouldHideJoinPartMessages() != (mMessageFilterOptions != null) &&
                mChannelName != null) {
            reloadMessages(null);
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
    public void onMessage(String channel, MessageInfo messageInfo, MessageId messageId) {
        updateMessageList(() -> {
            if (mLoadNewerIdentifier != null)
                return;
            MessageFilterOptions opt = getFilterOptions();
            if (opt != null) {
                if (opt.restrictToMessageTypes != null &&
                        !opt.restrictToMessageTypes.contains(messageInfo.getType()))
                    return;
                if (opt.excludeMessageTypes != null &&
                        opt.excludeMessageTypes.contains(messageInfo.getType()))
                    return;
            }

            if (!getUserVisibleHint() && mAdapter.getNewMessagesStart() == null)
                mAdapter.setNewMessagesStart(messageId);
            mAdapter.appendMessage(messageInfo, messageId);
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
        CharSequence messages = mAdapter.getSelectedMessages();
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("IRC Messages", messages));
    }

    public void shareSelectedMessages() {
        CharSequence messages = mAdapter.getSelectedMessages();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, messages);
        intent.setType("text/plain");
        mRecyclerView.getContext().startActivity(Intent.createChooser(intent,
                getString(R.string.message_share_title)));
    }

    public void deleteSelectedMessages() {
        List<MessageId> msgIds = mAdapter.getSelectedMessageIds();
        for (Long l : mAdapter.getSelectedItems()) {
            ChatMessagesAdapter.Item i = mAdapter.getMessage(mAdapter.getItemPosition(l));
            if (i instanceof ChatMessagesAdapter.MessageItem)
                ((ChatMessagesAdapter.MessageItem) i).mHidden = true;
        }
        mAdapter.notifyDataSetChanged();
        mConnection.getApiInstance().getMessageStorageApi().deleteMessages(mChannelName, msgIds,
                null, null);
    }


    private MessagesActionModeCallback mMessagesActionModeCallback;

    private class MessagesActionModeCallback implements ActionMode.Callback {

        public ActionMode mActionMode;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_context_messages_full, menu);
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
                case R.id.action_share:
                    shareSelectedMessages();
                    mode.finish();
                    return true;
                case R.id.action_delete: {
                    int cnt = mAdapter.getSelectedItems().size();
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.action_delete_confirm_title)
                            .setMessage(getResources().getQuantityString(R.plurals.message_delete_confirm, cnt, cnt) + "\n\n" + getResources().getString(R.string.message_delete_confirm_note))
                            .setPositiveButton(R.string.action_delete, (di, w) -> {
                                deleteSelectedMessages();
                                mode.finish();
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                    return true;
                }
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ((ChatFragment) getParentFragment()).setTabsHidden(false);
            mAdapter.clearSelection();
            mActionMode = null;
        }

    };

}