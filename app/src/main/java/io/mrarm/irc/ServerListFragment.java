package io.mrarm.irc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;

public class ServerListFragment extends Fragment {

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    private ServerListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.server_list_fragment, container, false);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ((MainActivity) getActivity()).addActionBarDrawerToggle(toolbar);

        FloatingActionButton addFab = rootView.findViewById(R.id.fab);
        addFab.setOnClickListener((View view) -> {
            startActivity(new Intent(getContext(), EditServerActivity.class));
        });

        RecyclerView recyclerView = rootView.findViewById(R.id.server_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new ServerListAdapter(getActivity());
        recyclerView.setAdapter(mAdapter);
        mAdapter.setActiveServerClickListener((ServerConnectionInfo info) -> {
            ((MainActivity) getActivity()).openServer(info, null, null, true);
        });
        mAdapter.setActiveServerLongClickListener((ServerConnectionInfo info) -> {
            MenuBottomSheetDialog menu = new MenuBottomSheetDialog(getContext());
            menu.addItem(R.string.action_open, R.drawable.ic_open_in_new
                    , (MenuBottomSheetDialog.Item item) -> {
                ((MainActivity) getActivity()).openServer(info, null);
                return true;
            });
            menu.addItem(R.string.action_edit, R.drawable.ic_edit, (MenuBottomSheetDialog.Item item) -> {
                ServerConfigData data = ServerConfigManager.getInstance(getContext()).findServer(info.getUUID());
                startActivity(EditServerActivity.getLaunchIntent(getContext(), data));
                return true;
            });
            menu.addItem(R.string.action_clone, R.drawable.ic_content_copy, (MenuBottomSheetDialog.Item item) -> {
                ServerConfigData data = ServerConfigManager.getInstance(getContext()).findServer(info.getUUID());
                startActivity(EditServerActivity.getLaunchIntent(getContext(), data, true));
                return true;
            });
            menu.addItem(R.string.action_disconnect_and_close, R.drawable.ic_close, (MenuBottomSheetDialog.Item item) -> {
                info.disconnect();
                ServerConnectionManager.getInstance(getContext()).removeConnection(info);
                return true;
            });
            menu.show();
            ((MainActivity) getActivity()).setFragmentDialog(menu);
        });
        mAdapter.setInactiveServerClickListener((ServerConfigData data) -> {
            ServerConnectionManager.getInstance(getContext()).tryCreateConnection(data, getActivity());
        });
        mAdapter.setInactiveServerLongClickListener((ServerConfigData data) -> {
            MenuBottomSheetDialog menu = new MenuBottomSheetDialog(getContext());
            menu.addItem(R.string.action_connect, R.drawable.ic_server_connected, (MenuBottomSheetDialog.Item item) -> {
                ServerConnectionManager.getInstance(getContext()).tryCreateConnection(data, getActivity());
                return true;
            });
            menu.addItem(R.string.action_edit, R.drawable.ic_edit, (MenuBottomSheetDialog.Item item) -> {
                startActivity(EditServerActivity.getLaunchIntent(getContext(), data));
                return true;
            });
            menu.addItem(R.string.action_clone, R.drawable.ic_content_copy, (MenuBottomSheetDialog.Item item) -> {
                startActivity(EditServerActivity.getLaunchIntent(getContext(), data, true));
                return true;
            });
            menu.addItem(R.string.action_delete, R.drawable.ic_delete, (MenuBottomSheetDialog.Item item) -> {
                AlertDialog.Builder builder2 = new AlertDialog.Builder(getContext());
                builder2.setTitle(R.string.action_delete_confirm_title);
                builder2.setMessage(getString(R.string.action_delete_confirm_body, data.name));
                builder2.setPositiveButton(R.string.action_delete, (DialogInterface dialog2, int which2) -> {
                    ServerConfigManager.getInstance(getContext()).deleteServer(data);
                });
                builder2.setNegativeButton(R.string.action_cancel, null);
                ((MainActivity) getActivity()).setFragmentDialog(builder2.show());
                return true;
            });
            menu.show();
            ((MainActivity) getActivity()).setFragmentDialog(menu);
        });
        mAdapter.registerListeners();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null)
            mAdapter.unregisterListeners();
        super.onDestroyView();
    }

    public ServerListAdapter getAdapter() {
        return mAdapter;
    }

}
