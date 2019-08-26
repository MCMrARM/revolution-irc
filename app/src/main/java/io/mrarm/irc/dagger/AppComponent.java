package io.mrarm.irc.dagger;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import io.mrarm.irc.IRCApplication;
import io.mrarm.irc.newui.MainActivityModule;
import io.mrarm.irc.persistence.UIStorage;

@Component(modules = {AndroidInjectionModule.class, AppModule.class, UIStorage.class,
        MainActivityModule.class, LegacySingletonsProviderModule.class})
@Singleton
public interface AppComponent extends AndroidInjector<IRCApplication> {
    @Component.Factory
    interface Factory extends AndroidInjector.Factory<IRCApplication> {
        AppComponent create(@BindsInstance IRCApplication app);
    }

    EagerSingletons createEagerSingletons();
}
