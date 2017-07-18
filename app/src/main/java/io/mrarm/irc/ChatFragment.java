package io.mrarm.irc;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.ImageViewTintUtils;
import io.mrarm.irc.util.SettingsHelper;
import io.mrarm.irc.util.SimpleTextVariableList;
import io.mrarm.irc.view.NickAutoCompleteEditText;
import io.mrarm.irc.view.TextFormatBar;

public class ChatFragment extends Fragment implements
        ServerConnectionInfo.ChannelListChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ARG_SERVER_UUID = "server_uuid";
    private static final String ARG_CHANNEL_NAME = "channel";

    private ServerConnectionInfo mConnectionInfo;

    private AppBarLayout mAppBar;
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private DrawerLayout mDrawerLayout;
    private ChannelMembersAdapter mChannelMembersAdapter;
    private ChannelMembersListAdapter mChannelMembersListAdapter;
    private NickAutoCompleteEditText mSendText;
    private View mFormatBarDivider;
    private TextFormatBar mFormatBar;
    private ImageView mSendIcon;
    private ImageView mTabIcon;
    private int mNormalToolbarInset;
    private boolean mJustDismissedPopup;
    private boolean mClickForceAutocomplete;

    public static ChatFragment newInstance(ServerConnectionInfo server, String channel) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_UUID, server.getUUID().toString());
        if (channel != null)
            args.putString(ARG_CHANNEL_NAME, channel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat_content, container, false);

        UUID connectionUUID = UUID.fromString(getArguments().getString(ARG_SERVER_UUID));
        mConnectionInfo = ServerConnectionManager.getInstance(getContext()).getConnection(connectionUUID);
        String requestedChannel = getArguments().getString(ARG_CHANNEL_NAME);

        mAppBar = (AppBarLayout) rootView.findViewById(R.id.appbar);

        mToolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        mNormalToolbarInset = mToolbar.getContentInsetStartWithNavigation();

        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mConnectionInfo.getName());

        ((MainActivity) getActivity()).addActionBarDrawerToggle(mToolbar);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(), mConnectionInfo);

        mViewPager = (ViewPager) rootView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        if (requestedChannel != null)
            setCurrentChannel(requestedChannel);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) { }

            @Override
            public void onPageSelected(int i) {
                ((MainActivity) getActivity()).getDrawerHelper().setSelectedChannel(mConnectionInfo,
                        mSectionsPagerAdapter.getChannel(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) { }
        });

        mConnectionInfo.addOnChannelListChangeListener(this);

        mTabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        mDrawerLayout = (DrawerLayout) rootView.findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        mChannelMembersAdapter = new ChannelMembersAdapter(mConnectionInfo, null);
        RecyclerView membersRecyclerView = (RecyclerView) rootView.findViewById(R.id.members_list);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        membersRecyclerView.setAdapter(mChannelMembersAdapter);

        mFormatBar = (TextFormatBar) rootView.findViewById(R.id.format_bar);
        mFormatBarDivider = rootView.findViewById(R.id.format_bar_divider);
        mSendText = (NickAutoCompleteEditText) rootView.findViewById(R.id.send_text);
        mSendIcon = (ImageButton) rootView.findViewById(R.id.send_button);
        mTabIcon = (ImageButton) rootView.findViewById(R.id.tab_button);

        if (Build.VERSION.SDK_INT >= 17) {
            mSendText.setOnDismissListener(() -> {
                mJustDismissedPopup = true;
                mSendText.postDelayed(() -> {
                    mJustDismissedPopup = false;
                }, 100L);
            });
            mTabIcon.setOnTouchListener((View v, MotionEvent event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    mClickForceAutocomplete = mJustDismissedPopup;
                return false;
            });
        }

        mSendText.setFormatBar(mFormatBar);
        mSendText.setCustomSelectionActionModeCallback(new FormatItemActionMode());

        mChannelMembersListAdapter = new ChannelMembersListAdapter(null);
        mSendText.setAdapter(mChannelMembersListAdapter);

        mFormatBar.setExtraButton(R.drawable.ic_close, getString(R.string.action_close), (View v) -> {
            setFormatBarVisible(false);
        });

        ImageViewTintUtils.setTint(mSendIcon, 0x54000000);

        mSendText.addTextChangedListener(new TextWatcher() {
            boolean wasEmpty = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isEmpty = (s.length() > 0);
                if (isEmpty == wasEmpty)
                    return;
                wasEmpty = isEmpty;
                int accentColor = getResources().getColor(R.color.colorAccent);
                if (s.length() > 0)
                    ImageViewTintUtils.setTint(mSendIcon, accentColor);
                else
                    ImageViewTintUtils.setTint(mSendIcon, 0x54000000);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mSendText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND)
                sendMessage();
            return false;
        });

        mSendIcon.setOnClickListener((View view) -> {
            sendMessage();
        });

        mTabIcon.setOnClickListener((View v) -> {
            doTabNickComplete();
        });

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

        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.addPreferenceChangeListener(SettingsHelper.PREF_CHAT_APPBAR_COMPACT_MODE, this);
        s.addPreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_SHOW_BUTTON, this);
        s.addPreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_DOUBLE_TAP, this);

        setTabButtonVisible(s.isNickAutocompleteButtonVisible());
        setDoubleTapCompleteEnabled(s.isNickAutocompleteDoubleTapEnabled());

        return rootView;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getView() != null) {
            updateToolbarCompactLayoutStatus(getView().getBottom() - getView().getTop());
            SettingsHelper s = SettingsHelper.getInstance(getContext());
            setTabButtonVisible(s.isNickAutocompleteButtonVisible());
            setDoubleTapCompleteEnabled(s.isNickAutocompleteDoubleTapEnabled());
        }
    }

    public void updateToolbarCompactLayoutStatus(int height) {
        String mode = SettingsHelper.getInstance(getContext()).getChatAppbarCompactMode();
        boolean enabled = mode.equals(SettingsHelper.COMPACT_MODE_ALWAYS) ||
                (mode.equals(SettingsHelper.COMPACT_MODE_AUTO) &&
                        height < getResources().getDimensionPixelSize(R.dimen.compact_toolbar_activate_height));
        setUseToolbarCompactLayout(enabled);
    }

    public void setUseToolbarCompactLayout(boolean enable) {
        if (enable == (mTabLayout.getParent() == mToolbar))
            return;
        if (enable) {
            mAppBar.removeView(mTabLayout);
            mToolbar.addView(mTabLayout);
            mToolbar.setContentInsetStartWithNavigation(0);
            ViewGroup.LayoutParams params = mTabLayout.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mTabLayout.setLayoutParams(params);
        } else {
            mToolbar.removeView(mTabLayout);
            mAppBar.addView(mTabLayout);
            mToolbar.setContentInsetStartWithNavigation(mNormalToolbarInset);
            ViewGroup.LayoutParams params = mTabLayout.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mTabLayout.setLayoutParams(params);
        }
    }

    public void setTabsHidden(boolean hidden) {
        mTabLayout.setVisibility(hidden ? View.GONE : View.VISIBLE);
    }

    public void setFormatBarVisible(boolean visible) {
        if (visible) {
            mFormatBar.setVisibility(View.VISIBLE);
            mFormatBarDivider.setVisibility(View.VISIBLE);
        } else {
            mFormatBar.setVisibility(View.GONE);
            mFormatBarDivider.setVisibility(View.GONE);
        }
    }

    public void setTabButtonVisible(boolean visible) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                mSendText.getLayoutParams();
        if (visible) {
            MarginLayoutParamsCompat.setMarginStart(layoutParams, 0);
            mTabIcon.setVisibility(View.VISIBLE);
        } else {
            MarginLayoutParamsCompat.setMarginStart(layoutParams,
                    getResources().getDimensionPixelSize(R.dimen.message_edit_text_margin_left));
            mTabIcon.setVisibility(View.GONE);
        }
        mSendText.setLayoutParams(layoutParams);
    }

    public void setDoubleTapCompleteEnabled(boolean enabled) {
        if (enabled) {
            GestureDetector detector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    doTabNickComplete();
                    return true;
                }
            });
            mSendText.setOnTouchListener((View v, MotionEvent event) -> detector.onTouchEvent(event));
        } else {
            mSendText.setOnTouchListener(null);
        }
    }

    public void doTabNickComplete() {
        int end = mSendText.getSelectionStart();
        int start = mSendText.getTokenizer().findTokenStart(mSendText.getText(), end);
        if (start < mSendText.length() && mSendText.getText().charAt(start) == '@')
            start++;
        String startNick = mSendText.getText().subSequence(start, end).toString();
        int matches = 0;
        String match = null;
        for (NickWithPrefix n : mChannelMembersAdapter.getMembers()) {
            if (n.getNick().startsWith(startNick) && matches++ == 0)
                match = n.getNick();
        }
        if (match == null)
            return;

        if (mClickForceAutocomplete || matches == 1) {
            mSendText.getText().replace(end, end, mSendText.getTokenizer().terminateToken(match.substring(startNick.length())));
            mSendText.dismissDropDown();
        } else {
            mSendText.forceShowDropDown();
        }
    }

    public void sendMessage() {
        String text = IRCColorUtils.convertSpannableToIRCString(getContext(), mSendText.getText());
        if (text.length() == 0)
            return;
        mSendText.setText("");
        String channel = mSectionsPagerAdapter.getChannel(mViewPager.getCurrentItem());
        if (text.charAt(0) == '/') {
            SimpleTextVariableList vars = new SimpleTextVariableList();
            vars.set(CommandAliasManager.VAR_CHANNEL, channel);
            vars.set(CommandAliasManager.VAR_MYNICK, mConnectionInfo.getNotificationManager().getUserNick());
            CommandAliasManager.getInstance(getContext()).processCommand((IRCConnection) mConnectionInfo.getApiInstance(), text.substring(1), vars);
            return;
        }
        mConnectionInfo.getApiInstance().sendMessage(channel, text, null, null);
    }

    @Override
    public void onDestroyView() {
        mConnectionInfo.removeOnChannelListChangeListener(this);
        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.removePreferenceChangeListener(SettingsHelper.PREF_CHAT_APPBAR_COMPACT_MODE, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_SHOW_BUTTON, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_DOUBLE_TAP, this);
        super.onDestroyView();
    }

    public ServerConnectionInfo getConnectionInfo() {
        return mConnectionInfo;
    }

    public void setCurrentChannel(String channel) {
        mViewPager.setCurrentItem(mConnectionInfo.getChannels().indexOf(channel) + 1);
    }

    public void setCurrentChannelMembers(List<NickWithPrefix> members) {
        mChannelMembersAdapter.setMembers(members);
        mChannelMembersListAdapter.setMembers(members);
        if (members == null || members.size() == 0)
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        else
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    public String getCurrentChannel() {
        return mSectionsPagerAdapter.getChannel(mViewPager.getCurrentItem());
    }

    @Override
    public void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        getActivity().runOnUiThread(() -> {
            mSectionsPagerAdapter.updateChannelList();
        });
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ServerConnectionInfo connectionInfo;
        private List<String> channels;

        public SectionsPagerAdapter(FragmentManager fm, ServerConnectionInfo connectionInfo) {
            super(fm);
            this.connectionInfo = connectionInfo;
            channels = connectionInfo.getChannels();
        }

        public void updateChannelList() {
            channels = connectionInfo.getChannels();
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
        public int getCount() {
            if (channels == null)
                return 1;
            return channels.size() + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return getString(R.string.tab_server);
            return channels.get(position - 1);
        }

        public String getChannel(int position) {
            if (position == 0)
                return null;
            return channels.get(position - 1);
        }

    }

    private class FormatItemActionMode implements ActionMode.Callback {

        private MenuItem mFormatMenuItem;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mFormatMenuItem = menu.add(R.string.message_format)
                    .setIcon(R.drawable.ic_text_format);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (mFormatMenuItem == item) {
                setFormatBarVisible(true);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

    }

}
