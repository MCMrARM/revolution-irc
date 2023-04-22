package io.mrarm.irc.util;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.dialog.StatusBarColorBottomSheetDialog;

public class OperatorActions {

    // Order must match 'operator_actions_items' in strings.xml
    public static final int ACTION_GIVE_VOICE  = 0;
    public static final int ACTION_TAKE_VOICE  = 1;
    public static final int ACTION_GIVE_OPS  = 2;
    public static final int ACTION_TAKE_OPS  = 3;
    public static final int ACTION_KICK  = 4;
    public static final int ACTION_BAN  = 5;

    private String mNick;
    private String mHostname;
    private String mChannel;
    private IRCConnection api;
    private Context mContext;
    private StatusBarColorBottomSheetDialog mDialog;

    public OperatorActions(Context context, ServerConnectionInfo connection, String nick, String hostname, String channel, StatusBarColorBottomSheetDialog dialog) {
        mContext = context;
        api = (IRCConnection) connection.getApiInstance();
        mNick = nick;
        mHostname = hostname;
        mChannel = channel;
        mDialog = dialog;
    }

    public String[] getItems() {
        String[] items = new String[6];
        items[ACTION_GIVE_VOICE] = mContext.getResources().getString(R.string.operator_actions_give_voice);
        items[ACTION_TAKE_VOICE] = mContext.getResources().getString(R.string.operator_actions_take_voice);
        items[ACTION_GIVE_OPS] = mContext.getResources().getString(R.string.operator_actions_give_ops);
        items[ACTION_TAKE_OPS] = mContext.getResources().getString(R.string.operator_actions_take_ops);
        items[ACTION_KICK] = mContext.getResources().getString(R.string.operator_actions_kick);
        items[ACTION_BAN] = mContext.getResources().getString(R.string.operator_actions_ban);
        return items;
    }

    public void executeAction(int action) {
        String command = null;
        if (mChannel == null || mNick == null) {
            mDialog.cancel();
            return;
        }
        switch (action) {
            case ACTION_GIVE_VOICE:
                command = "MODE " + mChannel + " +v " + mNick;
                break;
            case ACTION_TAKE_VOICE:
                command = "MODE " + mChannel + " -v " + mNick;
                break;
            case ACTION_GIVE_OPS:
                command = "MODE " + mChannel + " +o " + mNick;
                break;
            case ACTION_TAKE_OPS:
                command = "MODE " + mChannel + " -o " + mNick;
                break;
            case ACTION_KICK:
                executeKick();
                return;
            case ACTION_BAN:
                executeBan();
                return;
        }

        if (command != null)
            api.sendCommandRaw(command, null, null);
            mDialog.cancel();
    }

    private void executeKick() {
        View v = LayoutInflater.from(mContext).inflate(R.layout.dialog_edit_text, null);
        EditText editText = v.findViewById(R.id.edit_text);
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.operator_actions_kick_reason)
                .setView(v)
                .setPositiveButton(R.string.action_ok, (DialogInterface d, int which) -> {
                    editText.clearFocus();
                    String kickReason = editText.getText().toString();
                    String command = "KICK " + mChannel + " " + mNick + " " + kickReason;
                    api.sendCommandRaw(command, null, null);
                    mDialog.cancel();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void executeBan() {
        View v = LayoutInflater.from(mContext).inflate(R.layout.dialog_edit_text, null);
        EditText editText = v.findViewById(R.id.edit_text);

        String hostnameWildcard = "*!*@" + mHostname;
        editText.setText(hostnameWildcard);

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.operator_actions_ban_host)
                .setView(v)
                .setPositiveButton(R.string.action_ok, (DialogInterface d, int which) -> {
                    editText.clearFocus();
                    String banHostname = editText.getText().toString();
                    String command = "MODE " + mChannel + " +b " + banHostname;
                    api.sendCommandRaw(command, null, null);
                    mDialog.cancel();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
