package io.mrarm.irc.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.core.app.NotificationCompat;
import androidx.appcompat.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.config.CommandAliasManager;

public class UserAutoRunCommandHelper implements ServerConnectionInfo.ChannelListChangeListener {

    private static Handler sHandler = new Handler(Looper.getMainLooper());

    private final ServerConnectionInfo mConnection;
    private final Map<String, List<Runnable>> mChannelRunnables = new HashMap<>();
    private boolean mRegisteredChannelListener = false;

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
                String[] cmdp = cmd.split(" ");
                if (cmdp[0].equalsIgnoreCase("wait")) {
                    float time = Float.parseFloat(cmdp[1]) * 1000.f;
                    int execStartI = i + 1;
                    sHandler.postAtTime(() -> {
                        executeUserCommands(cmds.subList(execStartI, cmds.size()));
                    }, this, SystemClock.uptimeMillis() + Math.round(time));
                    return;
                } else if (cmdp[0].equalsIgnoreCase("wait-for")) {
                    synchronized (mConnection) {
                        String chan = cmdp[1].toLowerCase();
                        if (mConnection.hasChannel(chan))
                            continue;
                        synchronized (mChannelRunnables) {
                            int execStartI = i + 1;
                            if (!mChannelRunnables.containsKey(chan))
                                mChannelRunnables.put(chan, new ArrayList<>());
                            mChannelRunnables.get(chan).add(() -> {
                                executeUserCommands(cmds.subList(execStartI, cmds.size()));
                            });
                            if (!mRegisteredChannelListener) {
                                mRegisteredChannelListener = true;
                                mConnection.addOnChannelListChangeListener(this);
                            }
                        }
                    }
                    return;
                }

                CommandAliasManager.ProcessCommandResult result = CommandAliasManager
                        .getInstance(mConnection.getConnectionManager().getContext())
                        .processCommand(conn.getServerConnectionData(), cmd, vars);
                if (result != null) {
                    if (result.mode == CommandAliasManager.CommandAlias.MODE_RAW) {
                        conn.sendCommandRaw(result.text, null, null);
                    } else if (result.mode == CommandAliasManager.CommandAlias.MODE_MESSAGE) {
                        if (result.channel == null)
                            throw new RuntimeException();
                        if (result.channel.equalsIgnoreCase("NickServ")
                                && result.text.startsWith("IDENTIFY ")) {
                            conn.sendCommandRaw("PRIVMSG " + result.channel + " :"
                                    + result.text, null, null);
                        } else if (!mConnection.hasChannel(result.channel)) {
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

    public void cancelUserCommandExecution() {
        sHandler.removeCallbacksAndMessages(this);
    }

    @Override
    public void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels) {
        synchronized (mChannelRunnables) {
            for (String channel : newChannels) {
                String lowercase = channel.toLowerCase();
                if (!mChannelRunnables.containsKey(lowercase))
                    continue;
                for (Runnable r : mChannelRunnables.remove(lowercase))
                    r.run();
            }
            if (mChannelRunnables.size() == 0) {
                mRegisteredChannelListener = false;
                mConnection.removeOnChannelListChangeListener(this);
            }
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
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
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
