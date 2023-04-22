package io.mrarm.irc.util;

import android.util.Log;

import java.util.regex.PatternSyntaxException;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.irc.MessageFilter;
import io.mrarm.chatlib.irc.ServerConnectionData;
import io.mrarm.irc.config.ServerConfigData;

public class IgnoreListMessageFilter implements MessageFilter {

    private ServerConfigData mConfig;

    public IgnoreListMessageFilter(ServerConfigData server) {
        mConfig = server;
    }

    @Override
    public boolean filter(ServerConnectionData serverConnectionData, String channel, MessageInfo message) {
        if (mConfig.ignoreList != null && message.getSender() != null) {
            for (ServerConfigData.IgnoreEntry entry : mConfig.ignoreList) {
                if (entry.isBad || ((entry.nick == null && entry.user == null && entry.host == null && entry.mesg == null)))
                    continue;
                if (entry.nickRegex != null && !entry.nickRegex.matcher(message.getSender().getNick()).matches())
                    continue;
                if (entry.userRegex != null && (message.getSender().getUser() == null || !entry.userRegex.matcher(message.getSender().getUser()).matches()))
                    continue;
                if (entry.hostRegex != null && (message.getSender().getHost() == null || !entry.hostRegex.matcher(message.getSender().getHost()).matches()))
                    continue;
                if (entry.mesgRegex != null && (message.getMessage() == null || !entry.mesgRegex.matcher(message.getMessage()).matches()))
                    continue;
                Log.d("IgnoreListMessageFilter", "Ignore message: " + message.getSender().getNick() + " " + message.getMessage());
                return false;
            }
        }
        return true;
    }
}
