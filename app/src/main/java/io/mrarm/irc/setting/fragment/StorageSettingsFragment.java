package io.mrarm.irc.setting.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.mrarm.irc.R;
import io.mrarm.irc.StorageSettingsAdapter;

public class StorageSettingsFragment extends Fragment implements NamedSettingsFragment {

    @Override
    public String getName() {
        return getString(R.string.pref_header_storage);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.items);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        StorageSettingsAdapter adapter = new StorageSettingsAdapter(getActivity());
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);

        return view;
    }

}