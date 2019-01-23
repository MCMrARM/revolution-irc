package io.mrarm.irc.util;

import java.util.List;
import java.util.concurrent.Future;

import io.mrarm.chatlib.ResponseCallback;
import io.mrarm.chatlib.ResponseErrorCallback;
import io.mrarm.chatlib.dto.MessageFilterOptions;
import io.mrarm.chatlib.dto.MessageId;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageList;
import io.mrarm.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.chatlib.message.MessageListener;
import io.mrarm.chatlib.message.WritableMessageStorageApi;
import io.mrarm.chatlib.util.InstantFuture;

public class StubMessageStorageApi implements WritableMessageStorageApi {

    @Override
    public Future<Void> addMessage(String s, MessageInfo messageInfo, ResponseCallback<Void> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return new InstantFuture<>(null);
    }

    @Override
    public MessageId.Parser getMessageIdParser() {
        return null;
    }

    @Override
    public Future<MessageList> getMessages(String s, int i, MessageFilterOptions filterOptions, MessageListAfterIdentifier messageListAfterIdentifier, ResponseCallback<MessageList> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return null;
    }

    @Override
    public Future<MessageList> getMessagesNear(String s, MessageId messageId, MessageFilterOptions messageFilterOptions, ResponseCallback<MessageList> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return null;
    }

    @Override
    public Future<Void> deleteMessages(String s, List<MessageId> list, ResponseCallback<Void> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return null;
    }

    @Override
    public Future<Void> subscribeChannelMessages(String s, MessageListener messageListener, ResponseCallback<Void> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return new InstantFuture<>(null);
    }

    @Override
    public Future<Void> unsubscribeChannelMessages(String s, MessageListener messageListener, ResponseCallback<Void> responseCallback, ResponseErrorCallback responseErrorCallback) {
        return new InstantFuture<>(null);
    }

}
