package io.mrarm.irc.dagger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.persistence.RecentServerList;

@Singleton
public class EagerSingletons {

    @Inject
    SettingsHelper settingsHelper; // required for some code not to break

    @Inject
    RecentServerList uiRecentServerList;

    @Inject
    EagerSingletons() {
    }

}
