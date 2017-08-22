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

import io.mrarm.irc.CommandAliasesAdapter;
import io.mrarm.irc.EditCommandAliasActivity;
import io.mrarm.irc.R;

public class CommandSettingsFragment extends Fragment implements NamedSettingsFragment {

    private CommandAliasesAdapter mAdapter;

    @Override
    public String getName() {
        return getString(R.string.pref_header_command_aliases);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list_with_fab, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.items);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new CommandAliasesAdapter(getActivity());
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(mAdapter.createItemDecoration(getActivity()));

        View addButton = view.findViewById(R.id.add);
        addButton.setOnClickListener((View v) -> {
            startActivity(new Intent(getActivity(), EditCommandAliasActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

}