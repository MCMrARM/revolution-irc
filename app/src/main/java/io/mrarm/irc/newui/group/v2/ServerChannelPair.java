package io.mrarm.irc.newui.group.v2;

import java.util.UUID;

public final class ServerChannelPair {

    private final UUID server;
    private final String channel;

    public ServerChannelPair(UUID server, String channel) {
        this.server = server;
        this.channel = channel;
    }

    public UUID getServer() {
        return server;
    }

    public String getChannel() {
        return channel;
    }

}
