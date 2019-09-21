package io.mrarm.irc.newui;

import androidx.fragment.app.Fragment;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.mrarm.irc.dagger.ActivityScope;
import io.mrarm.irc.dagger.FragmentModule;
import io.mrarm.irc.dagger.FragmentScope;
import io.mrarm.irc.newui.settings.GroupReorderFragment;

@Module
public abstract class MainActivityModule {


    @ActivityScope
    @ContributesAndroidInjector
    abstract MainActivity contributeMainActivity();

    @ContributesAndroidInjector
    abstract MainFragment contributeMainFragment();

    @ContributesAndroidInjector
    abstract GroupChannelListFragment contributeGroupChannelList();

    @ContributesAndroidInjector
    abstract GroupChannelListFragment.MyFragment contributeGroupChannelListSubFragment();

    @Module
    interface ServerListFragmentModule {
        @Binds
        Fragment bindFragment(ServerListFragment fragment);
    }

    @FragmentScope
    @ContributesAndroidInjector(modules = {ServerListFragmentModule.class, FragmentModule.class})
    public abstract ServerListFragment contributeServerListFragment();


    @ContributesAndroidInjector
    abstract GroupReorderFragment contributeGroupReorderFragment();

}
