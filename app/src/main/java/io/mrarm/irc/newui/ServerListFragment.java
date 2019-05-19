package io.mrarm.irc.newui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

public class ServerListFragment extends DaggerFragment {

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    @Inject
    ServerActiveListData mActiveData;
    @Inject
    ServerInactiveListData mInactiveData;
    private ServerListAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActiveData.load();
        mInactiveData.load();

        mAdapter = new ServerListAdapter(getContext(), mActiveData, mInactiveData);
        mAdapter.getSource().bind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mAdapter.getSource().unbind();
        mActiveData.unload();
        mInactiveData.unload();
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
}
