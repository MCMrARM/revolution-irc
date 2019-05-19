package io.mrarm.irc.dagger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.mrarm.irc.persistence.RecentServerList;

@Singleton
public class EagerSingletons {

    @Inject
    RecentServerList uiRecentServerList;

    @Inject
    EagerSingletons() {
    }

}
