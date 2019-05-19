package io.mrarm.irc.dagger;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.SettingsHelper;

@Module
public class LegacySingletonsModule {

    @Provides
    SettingsHelper settingsHelper(@AppQualifier Context context) {
        return SettingsHelper.getInstance(context);
    }

    @Provides
    ServerConfigManager serverConfigManager(@AppQualifier Context context) {
        return ServerConfigManager.getInstance(context);
    }

    @Provides
    ServerConnectionManager serverConnectionManager(@AppQualifier Context context) {
        return ServerConnectionManager.getInstance(context);
    }

}
