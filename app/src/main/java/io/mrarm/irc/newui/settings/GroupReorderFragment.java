package io.mrarm.irc.newui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.UUID;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.mrarm.irc.R;
import io.mrarm.irc.newui.SlideableFragmentToolbar;
import io.mrarm.irc.newui.group.GroupManager;
import io.mrarm.irc.newui.group.MasterGroup;
import io.mrarm.irc.util.GroupItemTouchHelperCallback;

public class GroupReorderFragment extends DaggerFragment
        implements SlideableFragmentToolbar.FragmentToolbarCallback {

    @Inject
    GroupManager groupManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list, container, false);

        ObservableList<MasterGroup> groups = groupManager.getMasterGroups();
        GroupReorderAdapter adapter = new GroupReorderAdapter(getContext(), groups);

        ((RecyclerView) view).setLayoutManager(new LinearLayoutManager(getContext()));
        ((RecyclerView) view).setAdapter(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(new GroupItemTouchHelperCallback(adapter) {
            {
                setCreateNewGroupThresholdDp(getContext(), 12.f);
            }

            @Override
            public int getGroupSize(int group) {
                return groups.get(group).getGroups().size();
            }

            @Override
            public void createGroup(int groupPos) {
                groups.add(groupPos, new MasterGroup(UUID.randomUUID()));
            }

            @Override
            public void deleteGroup(int groupPos) {
                groups.remove(groupPos);
            }

            @Override
            public void moveItem(int fromGrp, int fromIdx, int toGrp, int toIdx) {
                adapter.move(groups.get(fromGrp).getGroups(), fromIdx,
                        groups.get(toGrp).getGroups(), toIdx);
            }
        });
        touchHelper.attachToRecyclerView((RecyclerView) view);

        return view;
    }

    @Override
    public SlideableFragmentToolbar.ToolbarHolder onCreateToolbar(@NonNull LayoutInflater inflater,
                                                                  @Nullable ViewGroup container) {
        return new SlideableFragmentToolbar.TextToolbarHolder(this, container);
    }

}
