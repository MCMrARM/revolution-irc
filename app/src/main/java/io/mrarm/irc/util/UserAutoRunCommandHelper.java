package io.mrarm.irc.util;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.dialog.ThemedAlertDialog;

public class UserAutoRunCommandHelper {

    private final ServerConnectionInfo mConnection;

    public UserAutoRunCommandHelper(ServerConnectionInfo connection) {
        mConnection = connection;
    }

    public void executeUserCommands(List<String> cmds) {
        IRCConnection conn = (IRCConnection) mConnection.getApiInstance();
        List<String> errors = null;
        for (int i = 0; i < cmds.size(); i++) {
            String cmd = cmds.get(i);
            if (cmd.length() == 0)
                continue;
            if (!cmd.startsWith("/")) {
                conn.sendCommandRaw(cmd, null, null);
                continue;
            }
            cmd = cmd.substring(1);

            SimpleTextVariableList vars = new SimpleTextVariableList();
            vars.set(CommandAliasManager.VAR_MYNICK, mConnection.getUserNick());
            try {
                CommandAliasManager.ProcessCommandResult result = CommandAliasManager
                        .getInstance(mConnection.getConnectionManager().getContext())
                        .processCommand(conn.getServerConnectionData(), cmd, vars);
                if (result != null) {
                    if (result.mode == CommandAliasManager.CommandAlias.MODE_RAW) {
                        conn.sendCommandRaw(result.text, null, null);
                    } else if (result.mode == CommandAliasManager.CommandAlias.MODE_MESSAGE) {
                        if (result.channel == null)
                            throw new RuntimeException();
                        if (!mConnection.getChannels().contains(result.channel)) {
                            ArrayList<String> list = new ArrayList<>();
                            list.add(result.channel);
                            conn.joinChannels(list, (Void v) -> {
                                conn.sendMessage(result.channel, result.text, null, null);
                            }, null);
                        } else {
                            conn.sendMessage(result.channel, result.text, null, null);
                        }
                    } else {
                        throw new RuntimeException();
                    }
                } else {
                    throw new RuntimeException();
                }
            } catch (RuntimeException e) {
                Log.e("ServerConnectionInfo", "User command execution failed: " + cmd);
                e.printStackTrace();
                if (errors == null)
                    errors = new ArrayList<>();
                errors.add(cmd);
            }
        }
        if (errors != null) {
            WarningHelper.showWarning(new CommandProcessErrorWarning(mConnection.getName(), errors));
        }
    }


    public static class CommandProcessErrorWarning extends WarningHelper.Warning {

        private AlertDialog mDialog;
        private String mNetworkName;
        private List<String> mCommands;

        public CommandProcessErrorWarning(String networkName, List<String> commands) {
            mNetworkName = networkName;
            mCommands = commands;
        }

        @Override
        public void showDialog(Activity activity) {
            super.showDialog(activity);
            dismissDialog(activity);
            ThemedAlertDialog.Builder dialog = new ThemedAlertDialog.Builder(activity);
            dialog.setTitle(R.string.connection_error_command_title);

            StringBuilder commands = new StringBuilder();
            for (String cmd : mCommands) {
                commands.append('/');
                commands.append(cmd);
                commands.append('\n');
            }
            SpannableString commandsSeq = new SpannableString(commands);
            commandsSeq.setSpan(new TypefaceSpan("monospace"), 0, commandsSeq.length(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog.setMessage(SpannableStringHelper.format(activity.getResources().getQuantityText(
                    R.plurals.connection_error_command_dialog_content, mCommands.size()),
                    mNetworkName, commandsSeq));
            dialog.setPositiveButton(R.string.action_ok, null);
            dialog.setOnDismissListener((DialogInterface di) -> {
                dismiss();
            });
            mDialog = dialog.show();
        }

        @Override
        public void dismissDialog(Activity activity) {
            if (mDialog != null) {
                mDialog.setOnDismissListener(null);
                mDialog.dismiss();
                mDialog = null;
            }
        }

        @Override
        protected void buildNotification(Context context, NotificationCompat.Builder notification, int notificationId) {
            super.buildNotification(context, notification, notificationId);
            notification.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notification.setContentTitle(context.getString(R.string.connection_error_command_title));
            notification.setContentText(context.getString(R.string.connection_error_command_notification_desc));
            notification.setContentIntent(PendingIntent.getActivity(context, notificationId, MainActivity.getLaunchIntent(context, null, null), PendingIntent.FLAG_CANCEL_CURRENT));
        }

    }

}
