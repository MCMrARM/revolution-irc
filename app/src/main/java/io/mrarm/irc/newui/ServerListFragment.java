package io.mrarm.irc.newui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;

public class ServerListFragment extends Fragment implements ServerChannelListAdapter.CallbackInterface {

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    private ServerIconListData mIconData;
    private ServerIconListAdapter mIconAdapter;
    private ServerIconListIndicator mIconIndicator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIconData = new ServerIconListData(getContext());
        mIconAdapter = new ServerIconListAdapter(mIconData);
        mIconData.load();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewPager view = new ViewPager(inflater.getContext());
        view.setId(R.id.main_container);
        view.setAdapter(new MyAdapter(getChildFragmentManager(), mIconData));
        mIconIndicator = new ServerIconListIndicator(view.getContext());
        view.addOnPageChangeListener(mIconIndicator);
        mIconAdapter.setClickListener(view::setCurrentItem);

        return view;
    }

    public void setServerIconView(RecyclerView rv) {
        rv.setAdapter(mIconAdapter);
        rv.addItemDecoration(mIconIndicator);
        mIconIndicator.setRecyclerView(rv);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIconData.unload();
        mIconData = null;
    }

    @Override
    public void onChatOpened(ServerConnectionInfo server, String channel) {
    }

    private static class MyAdapter extends FragmentPagerAdapter {

        private ServerIconListData mData;

        public MyAdapter(FragmentManager fm, ServerIconListData data) {
            super(fm);
            mData = data;
        }

        @Override
        public Fragment getItem(int position) {
            MyFragment fragment = new MyFragment();
            Bundle b = new Bundle();
            b.putString(MyFragment.ARG_SERVER, mData.getList().get(position).getUUID().toString());
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public int getCount() {
            return mData.getList().size();
        }

    }

    public static class MyFragment extends Fragment
            implements ServerChannelListAdapter.CallbackInterface {

        public static String ARG_SERVER = "server_uuid";

        private ServerChannelListData mData;
        private ServerChannelListAdapter mAdapter;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String serverUUID = getArguments().getString(ARG_SERVER);
            mData = new ServerChannelListData(getContext(), ServerConnectionManager.getInstance(
                    getContext()).getConnection(UUID.fromString(serverUUID)));
            mData.load();
            mAdapter = new ServerChannelListAdapter(getContext(), mData);
            mAdapter.setCallbackInterface(this);
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            RecyclerView recyclerView = new RecyclerView(inflater.getContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
            recyclerView.setAdapter(mAdapter);

            return recyclerView;
        }

        @Override
        public void onChatOpened(ServerConnectionInfo server, String channel) {
            Fragment fragment = MessagesSingleFragment.newInstance(server, channel);
            ((MainActivity) getActivity()).getContainer().push(fragment);
        }
    }

}
