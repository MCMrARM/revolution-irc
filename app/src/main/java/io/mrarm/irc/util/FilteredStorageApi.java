package io.mrarm.irc.util;

import android.util.Log;

import java.util.concurrent.Future;

import io.mrarm.chatlib.ResponseCallback;
import io.mrarm.chatlib.ResponseErrorCallback;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.message.WrapperMessageStorageApi;
import io.mrarm.chatlib.message.WritableMessageStorageApi;
import io.mrarm.chatlib.util.InstantFuture;
import io.mrarm.irc.config.ServerConfigData;

public class FilteredStorageApi extends WrapperMessageStorageApi
        implements WritableMessageStorageApi {

    private WritableMessageStorageApi mApi;
    private ServerConfigData mConfig;

    public FilteredStorageApi(WritableMessageStorageApi wrapped, ServerConfigData server) {
        super(wrapped);
        mApi = wrapped;
        mConfig = server;
    }

    @Override
    public Future<Void> addMessage(String channel, MessageInfo message,
                                   ResponseCallback<Void> callback, ResponseErrorCallback errorCallback) {
        if (mConfig.ignoreList != null && message.getSender() != null) {
            for (ServerConfigData.IgnoreEntry entry : mConfig.ignoreList) {
                if (entry.nick == null && entry.user == null && entry.host == null)
                    continue;
                if (entry.nickRegex == null && entry.userRegex == null && entry.hostRegex == null)
                    entry.updateRegexes();
                if (entry.nickRegex != null && !entry.nickRegex.matcher(message.getSender().getNick()).matches())
                    continue;
                if (entry.userRegex != null && (message.getSender().getUser() == null || !entry.userRegex.matcher(message.getSender().getUser()).matches()))
                    continue;
                if (entry.hostRegex != null && (message.getSender().getHost() == null || !entry.hostRegex.matcher(message.getSender().getHost()).matches()))
                    continue;
                Log.d("FilteredStorageApi", "Ignore message: " + message.getSender().getNick() + " " + message.getMessage());
                return new InstantFuture<>(null);
            }
        }
        return mApi.addMessage(channel, message, callback, errorCallback);
    }

}
