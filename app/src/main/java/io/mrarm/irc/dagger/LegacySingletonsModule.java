package io.mrarm.irc.dagger;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import io.mrarm.irc.config.ServerConfigManager;

@Module
public class LegacySingletonsModule {

    @Provides
    ServerConfigManager serverConfigManager(@AppQualifier Context context) {
        return ServerConfigManager.getInstance(context);
    }

}
