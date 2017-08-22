package io.mrarm.irc.setting.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.mrarm.irc.R;
import io.mrarm.irc.setting.SettingsListAdapter;

public abstract class SettingsListFragment extends Fragment {

    private SettingsListAdapter mAdapter;

    public abstract SettingsListAdapter createAdapter();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (mAdapter == null)
            mAdapter = createAdapter();
        View view = LayoutInflater.from(container.getContext())
                .inflate(R.layout.simple_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.items);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SettingsListAdapter adapter = mAdapter;
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(adapter.createItemDecoration());
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAdapter.onActivityResult(requestCode, resultCode, data);
    }

}
