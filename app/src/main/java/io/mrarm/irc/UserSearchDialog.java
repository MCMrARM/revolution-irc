package io.mrarm.irc;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.util.SearchDialog;

public class UserSearchDialog extends SearchDialog {

    private ServerConnectionInfo mConnection;

    public UserSearchDialog(@NonNull Context context, ServerConnectionInfo connection) {
        super(context);
        mConnection = connection;
        setQueryHint(context.getString(R.string.action_message_user));
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        List<String> channels = new ArrayList<>();
        channels.add(query);
        mConnection.getApiInstance().joinChannels(channels, (Void v) -> {
            getOwnerActivity().runOnUiThread(() -> {
                ((MainActivity) getOwnerActivity()).openServer(mConnection, query);
            });
        }, null);
        cancel();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() < 2) {
            setSuggestions(null);
            return true;
        }
        mConnection.getApiInstance().getUserInfoApi().findUsers(newText, (List<String> users) -> {
            List<CharSequence> suggestions = new ArrayList<>();
            for (String sug : users) {
                suggestions.add(sug);
            }
            setSuggestions(suggestions);
        }, null);
        return true;
    }

}
