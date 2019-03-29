package io.mrarm.irc.newui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.chatlib.dto.MessageId;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.chat.ChannelUIData;
import io.mrarm.irc.chat.ChatSelectTouchListener;
import io.mrarm.irc.util.UiThreadHelper;
import io.mrarm.irc.view.JumpToRecentButton;

public class MessagesFragment extends Fragment implements MessagesData.Listener {

    protected static final String ARG_SERVER_UUID = "server_uuid";
    protected static final String ARG_CHANNEL_NAME = "channel";

    private static final int LOAD_MORE_REMAINING_ITEM_COUNT = 10;

    private ServerConnectionInfo mConnection;
    private String mChannelName;
    private ChannelUIData mUIInfo;
    private MessagesData mData;
    private MessagesUnreadData mUnreadData;
    private MessagesAdapter mAdapter;
    private MessageId mInitialMessageId;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;

    public static MessagesFragment newInstance(ServerConnectionInfo server,
                                                   String channelName) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, server.getUUID().toString());
        if (channelName != null)
            args.putString(ARG_CHANNEL_NAME, channelName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UUID connectionUUID = UUID.fromString(getArguments().getString(ARG_SERVER_UUID));
        mConnection = ServerConnectionManager.getInstance(getContext())
                .getConnection(connectionUUID);
        mChannelName = getArguments().getString(ARG_CHANNEL_NAME);

        mUIInfo = mConnection.getChatUIData().getOrCreateChannelData(mChannelName);
        mInitialMessageId = mUIInfo.getFirstVisibleMessage();

        mData = new MessagesData(getContext(), mConnection, mChannelName);
        mData.load(mInitialMessageId, null);

        mUnreadData = new MessagesUnreadData(mConnection, mChannelName);
        mUnreadData.load();

        mData.addListener(this);

        mAdapter = new MessagesAdapter(mData);
        mAdapter.setUnreadData(mUnreadData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mData.unload();
        mUnreadData.unload();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chat_messages_fragment, container, false);
        mRecyclerView = rootView.findViewById(R.id.messages);
        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new MessagesScrollListener());
        ChatSelectTouchListener selectListener = new ChatSelectTouchListener(mRecyclerView);
        mAdapter.setMessageLongPressListener((i) -> selectListener.startLongPressSelect());
        mRecyclerView.addOnItemTouchListener(selectListener);
        if (mInitialMessageId != null) {
            ScrollToMessageListener scrollTo = new ScrollToMessageListener();
            mData.addListener(scrollTo);
            scrollTo.check();
        }
        JumpToRecentButton button = rootView.findViewById(R.id.jump_to_recent);
        button.setOnClickListener((v) -> scrollToBottom());
        return rootView;
    }

    private MessageId findFirstVisibleMessage() {
        int mi = mLayoutManager.findFirstCompletelyVisibleItemPosition();
        for (int i = mi; i < mLayoutManager.getItemCount(); i++) {
            if (mData.get(i) instanceof MessagesData.MessageItem)
                return ((MessagesData.MessageItem) mData.get(i)).getMessageId();
        }
        for (int i = mi - 1; i >= 0; --i) {
            if (mData.get(i) instanceof MessagesData.MessageItem)
                return ((MessagesData.MessageItem) mData.get(i)).getMessageId();
        }
        return null;
    }

    private void saveScrollPosition() {
        if (mLayoutManager.findLastVisibleItemPosition() >= mLayoutManager.getItemCount() - 1)
            mUIInfo.setFirstVisibleMessage(null);
        else
            mUIInfo.setFirstVisibleMessage(findFirstVisibleMessage());
    }

    private void scrollToBottom() {
        if (mAdapter.getItemCount() - mLayoutManager.findLastCompletelyVisibleItemPosition() <= 50) {
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
        } else {
            if (mData.hasMoreMessages(true)) {
                mData.load(null, null);
            } else {
                mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveScrollPosition();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScrollPosition();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
    }

    @Override
    public void onReloaded() {
    }

    @Override
    public void onItemsAdded(int pos, int count) {
        UiThreadHelper.runOnUiThread(() -> {
            if (pos + count == mData.size() && mRecyclerView != null &&
                    mLayoutManager.findLastCompletelyVisibleItemPosition() >= pos - 1) {
                mRecyclerView.scrollToPosition(mData.size() - 1);
            }
        });
    }

    @Override
    public void onItemsRemoved(int pos, int count) {
    }

    public ServerConnectionInfo getConnection() {
        return mConnection;
    }

    public String getChannelName() {
        return mChannelName;
    }

    private class MessagesScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            int firstVisible = mLayoutManager.findFirstVisibleItemPosition();
            int lastVisible = mLayoutManager.findLastVisibleItemPosition();
            if (firstVisible <= LOAD_MORE_REMAINING_ITEM_COUNT) {
                mData.loadMoreMessages(false);
            }
            if (lastVisible >= mLayoutManager.getItemCount() - LOAD_MORE_REMAINING_ITEM_COUNT) {
                mData.loadMoreMessages(true);
            }
        }

    }

    private class ScrollToMessageListener implements MessagesData.Listener {

        public void check() {
            int iof = mData.findMessageWithId(mInitialMessageId);
            if (iof != -1) {
                mLayoutManager.scrollToPositionWithOffset(iof, 0);
                mData.removeListener(this);
            }
        }

        @Override
        public void onReloaded() {
            check();
        }

        @Override
        public void onItemsAdded(int pos, int count) {
            check();
        }

        @Override
        public void onItemsRemoved(int pos, int count) {
        }
    }

}
