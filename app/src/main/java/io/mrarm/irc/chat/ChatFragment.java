package io.mrarm.irc.chat;

import android.database.DataSetObserver;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.core.widget.ImageViewCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.ChannelNotificationManager;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.NotificationManager;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.ChatSettings;
import io.mrarm.irc.config.NickAutocompleteSettings;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.config.UiSettingChangeCallback;

public class ChatFragment extends Fragment implements
        ServerConnectionInfo.ChannelListChangeListener,
        ServerConnectionInfo.InfoChangeListener,
        NotificationManager.UnreadMessageCountCallback {

    public static final String ARG_SERVER_UUID = "server_uuid";
    public static final String ARG_CHANNEL_NAME = "channel";
    public static final String ARG_MESSAGE_ID = "message_id";
    public static final String ARG_SEND_MESSAGE_TEXT = "message_text";

    private ServerConnectionInfo mConnectionInfo;

    private AppBarLayout mAppBar;
    private TabLayout mTabLayout;
    private ChatPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private ChatFragmentSendMessageHelper mSendHelper;
    private int mNormalToolbarInset;
    private OneTimeMessageJump mMessageJump;
    private String mAutoOpenChannel;

    public static ChatFragment newInstance(ServerConnectionInfo server, String channel, String messageId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, server.getUUID().toString());
        if (channel != null)
            args.putString(ARG_CHANNEL_NAME, channel);
        if (messageId != null)
            args.putString(ARG_MESSAGE_ID, messageId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chat_fragment, container, false);

        UUID connectionUUID = UUID.fromString(getArguments().getString(ARG_SERVER_UUID));
        mConnectionInfo = ServerConnectionManager.getInstance(getContext()).getConnection(connectionUUID);
        String requestedChannel = getArguments().getString(ARG_CHANNEL_NAME);
        String requestedMessageId = getArguments().getString(ARG_MESSAGE_ID);

        if (mConnectionInfo == null) {
            ((MainActivity) getActivity()).openManageServers();
            return null;
        }

        mAppBar = rootView.findViewById(R.id.appbar);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        mNormalToolbarInset = toolbar.getContentInsetStartWithNavigation();

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mConnectionInfo.getName());

        ((MainActivity) getActivity()).addActionBarDrawerToggle(toolbar);

        mSectionsPagerAdapter = new ChatPagerAdapter(getContext(), getChildFragmentManager(), mConnectionInfo, savedInstanceState);

        mViewPager = rootView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        if (requestedChannel != null)
            setCurrentChannel(requestedChannel, requestedMessageId);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                ((MainActivity) getActivity()).getDrawerHelper().setSelectedChannel(mConnectionInfo,
                        mSectionsPagerAdapter.getChannel(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        mConnectionInfo.addOnChannelListChangeListener(this);
        mConnectionInfo.addOnChannelInfoChangeListener(this);

        mTabLayout = rootView.findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager, false);

        mSectionsPagerAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updateTabLayoutTabs();
            }
        });
        mConnectionInfo.getNotificationManager().addUnreadMessageCountCallback(this);
        updateTabLayoutTabs();

        rootView.addOnLayoutChangeListener((View v, int left, int top, int right, int bottom,
                                            int oldLeft, int oldTop, int oldRight, int oldBottom) -> {
            int height = bottom - top;
            mAppBar.post(() -> {
                if (!isAdded())
                    return;
                if (height < getResources().getDimensionPixelSize(R.dimen.collapse_toolbar_activate_height)) {
                    mAppBar.setVisibility(View.GONE);
                } else {
                    updateToolbarCompactLayoutStatus(height);
                    mAppBar.setVisibility(View.VISIBLE);
                }
            });
        });
        mTabLayout.addOnLayoutChangeListener((View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) -> {
            if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom)
                return;
            mTabLayout.setScrollPosition(mTabLayout.getSelectedTabPosition(), 0.f, false);
        });

        mSendHelper = new ChatFragmentSendMessageHelper(this, rootView);
        String sendText = getArguments().getString(ARG_SEND_MESSAGE_TEXT);
        if (sendText != null)
            mSendHelper.setMessageText(sendText);

        SettingsHelper.registerCallbacks(this);
        onSettingChange();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSendHelper.setCurrentChannel(null);
        if (mConnectionInfo == null)
            return;
        mConnectionInfo.removeOnChannelListChangeListener(this);
        mConnectionInfo.removeOnChannelInfoChangeListener(this);
        mConnectionInfo.getNotificationManager().removeUnreadMessageCountCallback(this);
        SettingsHelper.unregisterCallbacks(this);
    }

    private void updateTabLayoutTabs() {
        mTabLayout.removeAllTabs();
        final int c = mSectionsPagerAdapter.getCount();
        for (int i = 0; i < c; i++) {
            TabLayout.Tab tab = mTabLayout.newTab();
            tab.setText(mSectionsPagerAdapter.getPageTitle(i));
            tab.setTag(mSectionsPagerAdapter.getChannel(i));
            tab.setCustomView(R.layout.chat_tab);
            TextView textView = tab.getCustomView().findViewById(android.R.id.text1);
            textView.setTextColor(mTabLayout.getTabTextColors());
            ImageViewCompat.setImageTintList(tab.getCustomView().findViewById(R.id.notification_icon), mTabLayout.getTabTextColors());
            updateTabLayoutTab(tab);
            mTabLayout.addTab(tab, false);
        }

        final int currentItem = mViewPager.getCurrentItem();
        if (currentItem != mTabLayout.getSelectedTabPosition() && currentItem < mTabLayout.getTabCount())
            mTabLayout.getTabAt(currentItem).select();
    }

    private void updateTabLayoutTab(TabLayout.Tab tab) {
        String channel = (String) tab.getTag();
        boolean highlight = false;
        if (channel != null) {
            ChannelNotificationManager data = mConnectionInfo.getNotificationManager().getChannelManager(channel, false);
            if (data != null)
                highlight = data.hasUnreadMessages();
        }
        tab.getCustomView().findViewById(R.id.notification_icon).setVisibility(highlight ? View.VISIBLE : View.GONE);
    }

    @UiSettingChangeCallback(keys = {
            ChatSettings.PREF_APPBAR_COMPACT_MODE,
            ChatSettings.PREF_TEXT_AUTOCORRECT_ENABLED,
            ChatSettings.PREF_FONT,
            ChatSettings.PREF_SEND_BOX_ALWAYS_MULTILINE,
            NickAutocompleteSettings.PREF_SHOW_BUTTON,
            NickAutocompleteSettings.PREF_DOUBLE_TAP
    })
    private void onSettingChange() {
        if (getView() != null)
            updateToolbarCompactLayoutStatus(getView().getBottom() - getView().getTop());
        mSendHelper.setTabButtonVisible(NickAutocompleteSettings.isButtonVisible());
        mSendHelper.setMessageFieldTypeface(ChatSettings.getFont());
        mSendHelper.setAutocorrectEnabled(ChatSettings.isTextAutocorrectEnabled());
        mSendHelper.setAlwaysMultiline(ChatSettings.isSendBoxAlwaysMultiline());
    }

    public void updateToolbarCompactLayoutStatus(int height) {
        String mode = ChatSettings.getAppbarCompactMode();
        boolean enabled = mode.equals(SettingsHelper.COMPACT_MODE_ALWAYS) ||
                (mode.equals(SettingsHelper.COMPACT_MODE_AUTO) &&
                        height < getResources().getDimensionPixelSize(R.dimen.compact_toolbar_activate_height));
        setUseToolbarCompactLayout(enabled);
    }

    public void setUseToolbarCompactLayout(boolean enable) {
        Toolbar toolbar = ((MainActivity) getActivity()).getToolbar();
        if (enable == (mTabLayout.getParent() == toolbar))
            return;
        ((ViewGroup) mTabLayout.getParent()).removeView(mTabLayout);
        if (enable) {
            ViewGroup.LayoutParams params = new Toolbar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mTabLayout.setLayoutParams(params);
            toolbar.addView(mTabLayout);
            toolbar.setContentInsetStartWithNavigation(0);
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mTabLayout.setLayoutParams(params);
        } else {
            mAppBar.addView(mTabLayout);
            toolbar.setContentInsetStartWithNavigation(mNormalToolbarInset);
            ViewGroup.LayoutParams params = mTabLayout.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mTabLayout.setLayoutParams(params);
        }
    }

    public void setTabsHidden(boolean hidden) {
        mTabLayout.setVisibility(hidden ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSectionsPagerAdapter != null)
            mSectionsPagerAdapter.onSaveInstanceState(outState);
    }

    public ServerConnectionInfo getConnectionInfo() {
        return mConnectionInfo;
    }

    public void setCurrentChannel(String channel, String messageId) {
        if (messageId != null)
            mMessageJump = new OneTimeMessageJump(channel, messageId);
        int i = mSectionsPagerAdapter.findChannel(channel);
        mViewPager.setCurrentItem(i);
        if (i == 0) {
            // If channel was not found, cancel the notification as we most probably came here from
            // a notification.
            ChannelNotificationManager chanMgr = mConnectionInfo.getNotificationManager()
                    .getChannelManager(channel, false);
            if (chanMgr != null)
                chanMgr.cancelNotification(getActivity());
        }
    }

    public String getAndClearMessageJump(String channel) {
        if (channel != null && mMessageJump != null && channel.equals(mMessageJump.mChannel)) {
            OneTimeMessageJump ret = mMessageJump;
            mMessageJump = null;
            return ret.mMessageId;
        }
        return null;
    }

    public void setCurrentChannelInfo(String topic, String topicSetBy, Date topicSetOn,
                                      List<NickWithPrefix> members) {
        ((MainActivity) getActivity()).setCurrentChannelInfo(getConnectionInfo(),
                topic, topicSetBy, topicSetOn, members);
        if (mSendHelper != null)
            mSendHelper.setCurrentChannelMembers(members);
    }

    public String getCurrentChannel() {
        return mSectionsPagerAdapter.getChannel(mViewPager.getCurrentItem());
    }

    public ChatFragmentSendMessageHelper getSendMessageHelper() {
        return mSendHelper;
    }

    public void setAutoOpenChannel(String channelName) {
        mAutoOpenChannel = channelName;
        checkForAutoOpenChannel();
    }

    private void checkForAutoOpenChannel() {
        int i = mSectionsPagerAdapter.findChannel(mAutoOpenChannel);
        if (i != 0) {
            mViewPager.setCurrentItem(i);
            mAutoOpenChannel = null;
        }
    }

    @Override
    public void onConnectionInfoChanged(ServerConnectionInfo connection) {
        getActivity().runOnUiThread(() -> {
            mSendHelper.updateVisibility();
        });
    }

    @Override
    public void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        getActivity().runOnUiThread(() -> {
            mSectionsPagerAdapter.updateChannelList();
            checkForAutoOpenChannel();
        });
    }

    @Override
    public void onUnreadMessageCountChanged(ServerConnectionInfo info, String channel,
                                            int messageCount, int oldMessageCount) {
        if (messageCount == 0 || (messageCount > 0 && oldMessageCount == 0)) {
            getActivity().runOnUiThread(() -> {
                int tabNumber = mSectionsPagerAdapter.findChannel(channel);
                TabLayout.Tab tab = mTabLayout.getTabAt(tabNumber);
                if (tab != null)
                    updateTabLayoutTab(tab);
            });
        }
    }


    private static class OneTimeMessageJump {

        private String mChannel;
        private String mMessageId;

        private OneTimeMessageJump(String channel, String messageId) {
            this.mChannel = channel;
            this.mMessageId = messageId;
        }

    }

}
