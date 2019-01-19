package io.mrarm.irc.chat;

import android.content.Context;
import android.text.Spannable;

import java.util.ArrayList;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.SimpleTextVariableList;

public class SendMessageHelper {

    public static void sendMessage(Context context, ServerConnectionInfo connection,
                                   String channel, Spannable message, Callback cb) {
        String text = IRCColorUtils.convertSpannableToIRCString(context, message);
        if (text.length() == 0 || (!connection.isConnected() && !connection.isConnecting()))
            return;
        if (text.contains("\n")) {
            try {
                IRCConnection conn = (IRCConnection) connection.getApiInstance();
                ChannelUIData uiData = connection.getChatUIData().getOrCreateChannelData(channel);
                for (String s : text.split("\n")) {
                    conn.sendMessage(channel, s, null, null);
                    uiData.addHistoryMessage(s);
                }
            } catch (Exception ignored) {
            }
            cb.onMessageSent();
            return;
        }
        if (text.charAt(0) == '/') {
            SimpleTextVariableList vars = new SimpleTextVariableList();
            vars.set(CommandAliasManager.VAR_CHANNEL, channel);
            vars.set(CommandAliasManager.VAR_MYNICK, connection.getUserNick());
            try {
                IRCConnection conn = (IRCConnection) connection.getApiInstance();
                CommandAliasManager.ProcessCommandResult result = CommandAliasManager
                        .getInstance(context).processCommand(conn.getServerConnectionData(),
                                text.substring(1), vars);
                if (result != null) {
                    if (result.mode == CommandAliasManager.CommandAlias.MODE_RAW) {
                        cb.onRawCommandExecuted(text, result.text);
                        conn.sendCommandRaw(result.text, null, null);
                    } else if (result.mode == CommandAliasManager.CommandAlias.MODE_MESSAGE) {
                        if (result.channel == null)
                            throw new RuntimeException();
                        if (!connection.hasChannel(result.channel)) {
                            ArrayList<String> list = new ArrayList<>();
                            list.add(result.channel);
                            connection.getApiInstance().joinChannels(list, (Void v) -> {
                                conn.sendMessage(result.channel, result.text, null, null);
                            }, null);
                        } else {
                            conn.sendMessage(result.channel, result.text, null, null);
                        }
                    } else {
                        throw new RuntimeException();
                    }
                    connection.getChatUIData().getOrCreateChannelData(channel)
                            .addHistoryMessage(message);
                    cb.onMessageSent();
                    return;
                }
            } catch (RuntimeException e) {
                cb.onClientCommandError(context.getString(R.string.command_error_internal));
                return;
            }
            cb.onNoCommandHandlerFound(text);
            return;
        }
        if (channel == null)
            return;
        connection.getChatUIData().getOrCreateChannelData(channel).addHistoryMessage(message);
        cb.onMessageSent();
        connection.getApiInstance().sendMessage(channel, text, null, null);
    }

    public interface Callback {

        void onMessageSent();

        void onRawCommandExecuted(String clientCommand, String sentCommand);

        void onNoCommandHandlerFound(String message);

        void onClientCommandError(CharSequence error);

    }

}
