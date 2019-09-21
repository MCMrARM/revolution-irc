package io.mrarm.irc.newui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableList;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.mrarm.irc.R;
import io.mrarm.irc.newui.group.GroupManager;
import io.mrarm.irc.newui.group.MasterGroup;
import io.mrarm.irc.newui.group.ServerChannelPair;
import io.mrarm.irc.util.SimpleOnListChangedCallback;

public class GroupChannelListFragment extends DaggerFragment {

    public static GroupChannelListFragment newInstance() {
        return new GroupChannelListFragment();
    }

    @Inject
    GroupManager mGroupManager;

    private GroupIconListAdapter mIconAdapter;
    private MyAdapter mPagerAdapter;
    private GroupIconListIndicator mIconIndicator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIconAdapter = new GroupIconListAdapter(mGroupManager.getMasterGroups());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewPager view = new ViewPager(inflater.getContext());
        view.setId(R.id.main_container);
        mPagerAdapter = new MyAdapter(getChildFragmentManager(), mGroupManager.getMasterGroups());
        mPagerAdapter.bind();
        view.setAdapter(mPagerAdapter);
        mIconIndicator = new GroupIconListIndicator(view.getContext());
        view.addOnPageChangeListener(mIconIndicator);
        mIconAdapter.setClickListener(view::setCurrentItem);

        return view;
    }

    @Override
    public void onDestroyView() {
        mPagerAdapter.unbind();
        super.onDestroyView();
    }

    public void setServerIconView(RecyclerView rv) {
        rv.setAdapter(mIconAdapter);
        rv.addItemDecoration(mIconIndicator);
        mIconIndicator.setRecyclerView(rv);
    }

    private static class MyAdapter extends FragmentPagerAdapter {

        private ObservableList<MasterGroup> mData;
        private SimpleOnListChangedCallback<ObservableList<MasterGroup>> mDataListener =
                new SimpleOnListChangedCallback<>(this::notifyDataSetChanged);

        public MyAdapter(FragmentManager fm, ObservableList<MasterGroup> data) {
            super(fm);
            mData = data;
        }

        public void bind() {
            mData.addOnListChangedCallback(mDataListener);
            notifyDataSetChanged();
        }

        public void unbind() {
            mData.removeOnListChangedCallback(mDataListener);
        }

        @Override
        public Fragment getItem(int position) {
            MyFragment fragment = new MyFragment();
            Bundle b = new Bundle();
            b.putString(MyFragment.ARG_GROUP_UUID, mData.get(position).getUUID().toString());
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

    }

    public static class MyFragment extends DaggerFragment
            implements GroupChannelListAdapter.CallbackInterface {

        public static String ARG_GROUP_UUID = "group_uuid";

        @Inject
        GroupManager groupManager;
        private RecyclerView mRecyclerView;
        private GroupChannelListAdapter mAdapter;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String groupUUID = getArguments().getString(ARG_GROUP_UUID);
            MasterGroup group = groupManager.getMasterGroup(UUID.fromString(groupUUID));
            if (group != null) {
                mAdapter = new GroupChannelListAdapter(getContext(), group);
                mAdapter.setCallbackInterface(this);
            } else {
                Log.e("GroupChannelList", "Group not found: " + groupUUID);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            mRecyclerView = new RecyclerView(inflater.getContext());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mRecyclerView.getContext()));
            mRecyclerView.setAdapter(mAdapter);

            return mRecyclerView;
        }

        @Override
        public void onDestroyView() {
            mRecyclerView.setAdapter(null);
            super.onDestroyView();
        }

        @Override
        public void onChatOpened(ServerChannelPair entry) {
            Fragment fragment = MessagesSingleFragment.newInstance(
                    entry.getServer(), entry.getChannel());
            ((MainActivity) getActivity()).getContainer().push(fragment);
        }
    }

}
