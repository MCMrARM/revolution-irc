package io.mrarm.irc.newui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ServerListFragment extends Fragment {

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    private ServerActiveListData mActiveData;
    private ServerInactiveListData mInactiveData;
    private ServerListAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActiveData = new ServerActiveListData(getContext());
        mInactiveData = new ServerInactiveListData(getContext());
        mActiveData.load();
        mInactiveData.load();

        mAdapter = new ServerListAdapter(getContext(), mActiveData, mInactiveData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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
