package io.mrarm.irc;

import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.test.TestApiImpl;
import io.mrarm.irc.drawer.DrawerHelper;

public class ChatActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private DrawerHelper mDrawerHelper;
    private EditText mSendText;
    private ImageView mSendIcon;

    private ChatApi api;

    private ChatApi createApi() {
        TestApiImpl api = new TestApiImpl("test-user");

        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.testdata)));
        try {
            api.readTestChatLog(reader);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        return api;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        api = createApi();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), api);

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mDrawerHelper = new DrawerHelper(this);

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
                    ImageViewTintUtils.animateTint(mSendIcon, 0x54000000, accentColor, 200);
                else
                    ImageViewTintUtils.animateTint(mSendIcon, accentColor, 0x54000000, 200);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        refreshChannelList();
    }

    public void refreshChannelList() {
        api.getJoinedChannelList((List<String> channels) -> {
            mSectionsPagerAdapter.updateChannelList(channels);
        }, null);
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

    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText("Tab: " + getArguments().getInt(ARG_SECTION_NUMBER));
            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ChatApi api;
        private List<String> joinedChannelList;

        public SectionsPagerAdapter(FragmentManager fm, ChatApi api) {
            super(fm);
            this.api = api;
        }

        void updateChannelList(List<String> list) {
            joinedChannelList = list;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            if (joinedChannelList == null)
                return 1;
            return joinedChannelList.size() + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (joinedChannelList == null || position == 0)
                return getString(R.string.tab_server);
            return joinedChannelList.get(position - 1);
        }
    }
}
