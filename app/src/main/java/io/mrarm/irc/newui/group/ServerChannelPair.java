package io.mrarm.irc.newui.group;

import androidx.annotation.Nullable;

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

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ServerChannelPair))
            return false;
        return ((ServerChannelPair) obj).server.equals(server) &&
                ((ServerChannelPair) obj).channel.equals(channel);
    }

}
