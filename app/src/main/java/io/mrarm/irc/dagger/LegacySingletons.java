package io.mrarm.irc.dagger;

import android.content.Context;

import javax.inject.Inject;

import io.mrarm.irc.IRCApplication;
import io.mrarm.irc.config.ServerChatLogManager;

/**
 * This class provides singletons to legacy classes which do not use injection.
 */
public final class LegacySingletons {

    public static LegacySingletons get(Context c) {
        return ((IRCApplication) c.getApplicationContext()).getLegacySingletons();
    }

    @Inject
    public LegacySingletons() {
    }

    @Inject ServerChatLogManager chatLogManagerInstance;

    public ServerChatLogManager chatLogManager() {
        return chatLogManagerInstance;
    }

}
