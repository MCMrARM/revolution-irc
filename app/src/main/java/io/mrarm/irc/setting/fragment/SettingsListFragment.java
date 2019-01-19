package io.mrarm.irc.setting.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.mrarm.irc.R;
import io.mrarm.irc.setting.SettingsListAdapter;

public abstract class SettingsListFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private RecyclerView.ItemDecoration mItemDecoration;
    private SettingsListAdapter mAdapter;

    public abstract SettingsListAdapter createAdapter();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (mAdapter == null)
            mAdapter = createAdapter();
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.simple_list, container, false);
        mRecyclerView = view.findViewById(R.id.items);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);
        mItemDecoration = mAdapter.createItemDecoration();
        mRecyclerView.addItemDecoration(mItemDecoration);
        return view;
    }

    public final void recreateAdapter() {
        if (mItemDecoration != null)
            mRecyclerView.removeItemDecoration(mItemDecoration);
        mAdapter = createAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mItemDecoration = mAdapter.createItemDecoration();
        mRecyclerView.addItemDecoration(mItemDecoration);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAdapter.onActivityResult(requestCode, resultCode, data);
    }

}
