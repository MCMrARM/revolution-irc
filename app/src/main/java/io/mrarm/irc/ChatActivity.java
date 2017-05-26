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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.NickWithPrefix;
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
        ServerSSLHelper sslHelper = new ServerSSLHelper(this, new File(getFilesDir(), "test-keystore.jks"));
        connection.connect(new IRCConnectionRequest().setServerAddress("irc.freenode.net", 6697).addNick("mrarm-testing").setUser("mrarm-testing").setRealName("mrarm-testing").enableSSL(sslHelper.createSocketFactory(), sslHelper.createHostnameVerifier()),
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
        mDrawerHelper.setChannelClickListener((ServerConnectionInfo server, String channel) -> {
            if (server == mConnectionInfo) {
                mViewPager.setCurrentItem(server.getChannels().indexOf(channel) + 1);
                drawer.closeDrawers();
            }
        });
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                mDrawerHelper.setSelectedChannel(mConnectionInfo,
                        mSectionsPagerAdapter.getChannel(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

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

    public void setCurrentChannelMembers(List<NickWithPrefix> members) {
        mChannelMembersAdapter.setMembers(members);
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

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ServerConnectionInfo connectionInfo;

        public SectionsPagerAdapter(FragmentManager fm, ServerConnectionInfo connectionInfo) {
            super(fm);
            this.connectionInfo = connectionInfo;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
                return MessagesFragment.newStatusInstance(connectionInfo);
            return MessagesFragment.newInstance(connectionInfo,
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
            if (position == 0)
                return null;
            return connectionInfo.getChannels().get(position - 1);
        }

    }
}
