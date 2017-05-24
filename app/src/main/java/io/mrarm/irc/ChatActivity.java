package io.mrarm.irc;

import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.MessageListener;
import io.mrarm.chatlib.StatusMessageListener;
import io.mrarm.chatlib.dto.ChannelInfo;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageList;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.dto.StatusMessageInfo;
import io.mrarm.chatlib.dto.StatusMessageList;
import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.IRCConnectionRequest;
import io.mrarm.chatlib.test.TestApiImpl;
import io.mrarm.irc.drawer.DrawerHelper;

public class ChatActivity extends AppCompatActivity {

    private ServerConnectionInfo mConnectionInfo;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private DrawerHelper mDrawerHelper;
    private ChannelMembersAdapter mChannelMembersAdapter;
    private EditText mSendText;
    private ImageView mSendIcon;

    private ServerConnectionInfo createTestFileConnection() {
        TestApiImpl api = new TestApiImpl("test-user");

        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.testdata)));
        try {
            api.readTestChatLog(reader);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        ServerConnectionInfo connectionInfo = new ServerConnectionInfo(UUID.randomUUID(), "Test Connection", api);
        ServerConnectionManager.getInstance().addConnection(connectionInfo);
        return connectionInfo;
    }

    private ServerConnectionInfo createTestNetworkConnection() {
        IRCConnection connection = new IRCConnection();
        connection.connect(new IRCConnectionRequest().setServerAddress("irc.freenode.net", 8000).addNick("mrarm-testing").setUser("mrarm-testing").setRealName("mrarm-testing"),
                (Void v) -> {
                    ArrayList<String> channels = new ArrayList<>();
                    channels.add("#mrarm-testing");
                    connection.joinChannels(channels, null, null);
                }, null);

        ServerConnectionInfo connectionInfo = new ServerConnectionInfo(UUID.randomUUID(), "Test Connection", connection);
        ServerConnectionManager.getInstance().addConnection(connectionInfo);
        return connectionInfo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mConnectionInfo = createTestNetworkConnection();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(mConnectionInfo.getName());
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), mConnectionInfo);

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mConnectionInfo.addOnChannelListChangeListener((ServerConnectionInfo connection,
                                                        List<String> newChannels) -> {
            runOnUiThread(() -> {
                mSectionsPagerAdapter.notifyDataSetChanged();
            });
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mDrawerHelper = new DrawerHelper(this);

        mChannelMembersAdapter = new ChannelMembersAdapter(null);
        RecyclerView membersRecyclerView = (RecyclerView) findViewById(R.id.members_list);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersRecyclerView.setAdapter(mChannelMembersAdapter);

        mSendText = (EditText) findViewById(R.id.send_text);
        mSendIcon = (ImageButton) findViewById(R.id.send_button);

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

        mSendIcon.setOnClickListener((View view) -> {
            String text = mSendText.getText().toString();
            mConnectionInfo.getApiInstance().sendMessage(mSectionsPagerAdapter.getChannel(
                    mViewPager.getCurrentItem()), text, null, null);
            mSendText.setText("");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ChatFragment extends Fragment implements StatusMessageListener,
            MessageListener {

        private static final String ARG_SERVER_UUID = "server_uuid";
        private static final String ARG_DISPLAY_STATUS = "display_status";
        private static final String ARG_CHANNEL_NAME = "channel";

        private List<NickWithPrefix> mMembers = null;

        private ChatMessagesAdapter mAdapter;
        private ServerStatusMessagesAdapter mStatusAdapter;
        private List<MessageInfo> mMessages;
        private List<StatusMessageInfo> mStatusMessages;

        public ChatFragment() {
        }

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
            if (isVisibleToUser && getActivity() != null) {
                Log.d("ChatFragment", "setMembers " + (mMembers == null ? -1 : mMembers.size()));
                ((ChatActivity) getActivity()).mChannelMembersAdapter.setMembers(mMembers);
            }
        }

        public static ChatFragment newInstance(ServerConnectionInfo server,
                                               String channelName) {
            ChatFragment fragment = new ChatFragment();
            Bundle args = new Bundle();
            args.putString(ARG_SERVER_UUID, server.getUUID().toString());
            if (channelName != null)
                args.putString(ARG_CHANNEL_NAME, channelName);
            fragment.setArguments(args);
            return fragment;
        }

        public static ChatFragment newStatusInstance(ServerConnectionInfo server) {
            ChatFragment fragment = new ChatFragment();
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
            ServerConnectionInfo connectionInfo = ServerConnectionManager.getInstance()
                    .getConnection(connectionUUID);
            String channelName = getArguments().getString(ARG_CHANNEL_NAME);

            View rootView = inflater.inflate(R.layout.chat_fragment, container, false);
            RecyclerView recyclerView = (RecyclerView) rootView;
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            if (channelName != null) {
                mAdapter = new ChatMessagesAdapter(new MessageList(new ArrayList<>()));
                recyclerView.setAdapter(mAdapter);

                Log.i("ChatFragment", "Request message list for: " + channelName);
                connectionInfo.getApiInstance().getChannelInfo(channelName,
                        (ChannelInfo channelInfo) -> {
                            Log.i("ChatFragment", "Got channel info " + channelName);
                            Log.i("ChatFragment", "Channel member count: " + channelInfo.getMembers().size());
                            for (int i = 0; i < channelInfo.getMembers().size(); i++)
                                Log.i("ChatFragment", "Channel member: " + channelInfo.getMembers().get(i));
                            mMembers = channelInfo.getMembers();
                            if (getUserVisibleHint())
                                ((ChatActivity) getActivity()).mChannelMembersAdapter.setMembers(mMembers);
                        }, null);

                connectionInfo.getApiInstance().getMessages(channelName, 100, null,
                        (MessageList messages) -> {
                            Log.i("ChatFragment", "Got message list for " + channelName + ": " +
                                    messages.getMessages().size() + " messages");
                            mAdapter.setMessages(messages);
                            mMessages = messages.getMessages();

                            connectionInfo.getApiInstance().subscribeChannelMessages(channelName, ChatFragment.this, null, null);
                        }, null);
            } else if (getArguments().getBoolean(ARG_DISPLAY_STATUS)) {
                mStatusAdapter = new ServerStatusMessagesAdapter(new StatusMessageList(new ArrayList<>()));
                recyclerView.setAdapter(mStatusAdapter);

                Log.i("CharFragment", "Request status message list");
                connectionInfo.getApiInstance().getStatusMessages(100, null,
                        (StatusMessageList messages) -> {
                            Log.i("ChatFragment", "Got server status message list: " +
                                    messages.getMessages().size() + " messages");
                            mStatusAdapter.setMessages(messages);
                            mStatusMessages = messages.getMessages();

                            connectionInfo.getApiInstance().subscribeStatusMessages(ChatFragment.this, null, null);
                        }, null);
            }

            return rootView;
        }

        @Override
        public void onMessage(MessageInfo messageInfo) {
            getActivity().runOnUiThread(() -> {
                mMessages.add(messageInfo);
                mAdapter.notifyItemInserted(mMessages.size() - 1);
            });
        }

        @Override
        public void onStatusMessage(StatusMessageInfo statusMessageInfo) {
            getActivity().runOnUiThread(() -> {
                mStatusMessages.add(statusMessageInfo);
                mStatusAdapter.notifyItemInserted(mStatusMessages.size() - 1);
            });
        }

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ServerConnectionInfo connectionInfo;

        public SectionsPagerAdapter(FragmentManager fm, ServerConnectionInfo connectionInfo) {
            super(fm);
            this.connectionInfo = connectionInfo;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
                return ChatFragment.newStatusInstance(connectionInfo);
            return ChatFragment.newInstance(connectionInfo,
                    connectionInfo.getChannels().get(position - 1));
        }

        @Override
        public int getCount() {
            if (connectionInfo.getChannels() == null)
                return 1;
            return connectionInfo.getChannels().size() + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return getString(R.string.tab_server);
            return connectionInfo.getChannels().get(position - 1);
        }

        public String getChannel(int position) {
            return connectionInfo.getChannels().get(position - 1);
        }

    }
}
