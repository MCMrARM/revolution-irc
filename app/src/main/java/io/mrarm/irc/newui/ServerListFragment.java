package io.mrarm.irc.newui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.mrarm.irc.EditServerActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;
import io.mrarm.irc.newui.menu.BottomSheetMenu;

public class ServerListFragment extends DaggerFragment {

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    @Inject
    ServerConfigManager serverConfigManager;
    @Inject
    ServerConnectionManager serverConnectionManager;
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
        mAdapter.setActiveItemClickListener((server) -> {
            BottomSheetMenu menu = new BottomSheetMenu(getContext());
            getActivity().getMenuInflater().inflate(R.menu.menu_newui_server_list_active, menu);
            menu.findItem(R.id.action_edit).setOnMenuItemClickListener(
                    (m) -> handleEditAction(server.getUUID(), false));
            menu.findItem(R.id.action_clone).setOnMenuItemClickListener(
                    (m) -> handleEditAction(server.getUUID(), true));
            menu.findItem(R.id.action_disconnect_and_close).setOnMenuItemClickListener((m) -> {
                server.disconnect();
                serverConnectionManager.removeConnection(server);
                return true;
            });
            menu.show();
        });
        mAdapter.setInactiveItemClickListener((server) -> {
            BottomSheetMenu menu = new BottomSheetMenu(getContext());
            getActivity().getMenuInflater().inflate(R.menu.menu_newui_server_list_inactive, menu);
            menu.findItem(R.id.action_connect).setOnMenuItemClickListener((m) -> {
                ServerConfigData data = serverConfigManager.findServer(server.uuid);
                ServerConnectionManager.getInstance(getContext())
                        .tryCreateConnection(data, getActivity());
                return true;
            });
            menu.findItem(R.id.action_edit).setOnMenuItemClickListener(
                    (m) -> handleEditAction(server.uuid, false));
            menu.findItem(R.id.action_clone).setOnMenuItemClickListener(
                    (m) -> handleEditAction(server.uuid, true));
            menu.show();
        });
        mAdapter.getSource().bind();
    }

    private boolean handleEditAction(UUID uuid, boolean clone) {
        ServerConfigData data = serverConfigManager.findServer(uuid);
        startActivity(EditServerActivity.getLaunchIntent(getContext(), data, clone));
        return true;
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
