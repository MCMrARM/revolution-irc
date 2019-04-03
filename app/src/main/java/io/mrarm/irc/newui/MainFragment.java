package io.mrarm.irc.newui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import io.mrarm.irc.DCCActivity;
import io.mrarm.irc.IRCApplication;
import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;

public class MainFragment extends Fragment
        implements SlideableFragmentToolbar.FragmentToolbarCallback {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_newui_main_fragment, container, false);
        BottomNavigationView navigation = view.findViewById(R.id.navigation);

        setActiveView(navigation.getSelectedItemId());
        navigation.setOnNavigationItemSelectedListener((i) -> {
            setActiveView(i.getItemId());
            return true;
        });
        return view;
    }

    public Fragment createFragment(int selectedId) {
        if (selectedId == R.id.item_recents)
            return ChatListFragment.newInstance();
        if (selectedId == R.id.item_chats)
            return ServerListFragment.newInstance();
        return null;
    }

    private void setActiveView(int selectedId) {
        Fragment f = null;
        for (Fragment ff : getChildFragmentManager().getFragments()) {
            if (ff.isVisible())
                f = ff;
        }
        FragmentTransaction tr = getChildFragmentManager().beginTransaction();
        if (f != null)
            tr.hide(f);
        String newFragmentTag = "main:" + selectedId;
        f = getChildFragmentManager().findFragmentByTag(newFragmentTag);
        if (f != null)
            tr.show(f);
        else
            tr.add(R.id.main_container, createFragment(selectedId), newFragmentTag);
        tr.commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_server_list, menu);
        menu.findItem(R.id.action_dcc_transfers).setOnMenuItemClickListener((i) -> {
            startActivity(new Intent(getContext(), DCCActivity.class));
            return true;
        });
        menu.findItem(R.id.action_settings).setOnMenuItemClickListener((i) -> {
            startActivity(new Intent(getContext(), SettingsActivity.class));
            return true;
        });
        menu.findItem(R.id.action_exit).setOnMenuItemClickListener((i) -> {
            ((IRCApplication) getActivity().getApplication()).requestExit();
            return true;
        });
    }

    @Override
    public SlideableFragmentToolbar.ToolbarHolder onCreateToolbar(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        View toolbar = inflater.inflate(R.layout.activity_newui_main_fragment_toolbar, container, false);
        SlideableFragmentToolbar.SimpleToolbarHolder holder =
                new SlideableFragmentToolbar.SimpleToolbarHolder(toolbar);
        holder.addMenu(R.id.action_menu_view, this);

        return holder;
    }
}
