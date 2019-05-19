package io.mrarm.irc.newui;

import androidx.fragment.app.Fragment;

import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.mrarm.irc.dagger.ActivityScope;
import io.mrarm.irc.dagger.FragmentModule;
import io.mrarm.irc.dagger.FragmentScope;

@Module
public abstract class MainActivityModule {


    @ActivityScope
    @ContributesAndroidInjector
    abstract MainActivity contributeMainActivity();

    @ContributesAndroidInjector
    abstract MainFragment contributeMainFragment();

    @Module
    interface ServerListFragmentModule {
        @Binds
        Fragment bindFragment(ServerListFragment fragment);
    }

    @FragmentScope
    @ContributesAndroidInjector(modules = {ServerListFragmentModule.class, FragmentModule.class})
    public abstract ServerListFragment contributeServerListFragment();

}
