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
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;

public class ServerListFragment extends Fragment implements ChatListAdapter.CallbackInterface {

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    private ServerListChannelData mChannelData;
    private ServerListAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChannelData = new ServerListChannelData(getContext());
        mChannelData.load();
        mAdapter = new ServerListAdapter(mChannelData);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.items);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mChannelData.unload();
        mChannelData = null;
        mAdapter = null;
    }

    @Override
    public void onChatOpened(ServerConnectionInfo server, String channel) {
        Fragment fragment = MessagesFragment.newInstance(server, channel);
        ((MainActivity) getActivity()).getContainer().push(fragment);
    }

}
