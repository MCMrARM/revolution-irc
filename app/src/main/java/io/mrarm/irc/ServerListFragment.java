package io.mrarm.irc;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ServerListFragment extends Fragment {

    public static ServerListFragment newInstance() {
        return new ServerListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_server_list, container, false);

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ((MainActivity) getActivity()).addActionBarDrawerToggle(toolbar);

        FloatingActionButton addFab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        addFab.setOnClickListener((View view) -> {
            startActivity(new Intent(getContext(), EditServerActivity.class));
        });

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.server_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new ServerListAdapter(getContext()));


        return rootView;
    }

}
