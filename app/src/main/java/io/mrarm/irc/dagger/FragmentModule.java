package io.mrarm.irc.dagger;

import android.content.Context;

import androidx.fragment.app.Fragment;

import dagger.Module;
import dagger.Provides;

@Module
public class FragmentModule {

    @Provides
    @FragmentQualifier
    @FragmentScope
    public Context provideContext(Fragment fragment) {
        return fragment.getContext();
    }

}
