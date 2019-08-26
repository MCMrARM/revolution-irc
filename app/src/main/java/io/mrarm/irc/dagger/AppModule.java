package io.mrarm.irc.dagger;

import android.app.Application;
import android.content.Context;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import io.mrarm.irc.IRCApplication;
import io.mrarm.irc.config.AppServerChatLogManager;
import io.mrarm.irc.config.ServerChatLogManager;

@Module(includes = {AppModule.BindsModule.class})
public class AppModule {

    @Provides
    @AppQualifier
    Context provideContext(Application application) {
        return application.getApplicationContext();
    }

    @Module
    public interface BindsModule {
        @Binds
        Application application(IRCApplication app);

        @Binds
        ServerChatLogManager chatLogManager(AppServerChatLogManager mgr);
    }

}
