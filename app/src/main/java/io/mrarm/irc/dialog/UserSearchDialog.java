package io.mrarm.irc.dialog;

import android.content.Context;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.MainActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.view.ListSearchView;

public class UserSearchDialog extends SearchDialog {

    private ServerConnectionInfo mConnection;
    private ListSearchView.SimpleSuggestionsAdapter mAdapter;

    public UserSearchDialog(@NonNull Context context, ServerConnectionInfo connection) {
        super(context);
        mConnection = connection;
        setQueryHint(context.getString(R.string.action_message_user));
        mAdapter = new ListSearchView.SimpleSuggestionsAdapter();
        mAdapter.setItemClickListener((int index, CharSequence value) -> {
            onQueryTextSubmit(value.toString());
        });
        setSuggestionsAdapter(mAdapter);
    }

    @Override
    public void onQueryTextSubmit(String query) {
        List<String> channels = new ArrayList<>();
        channels.add(query);
        mConnection.getApiInstance().joinChannels(channels, (Void v) -> {
            getOwnerActivity().runOnUiThread(() -> {
                ((MainActivity) getOwnerActivity()).openServer(mConnection, query);
            });
        }, null);
        cancel();
    }

    @Override
    public void onQueryTextChange(String newText) {
        if (newText.length() < 2) {
            mAdapter.setItems(null);
            return;
        }
        mConnection.getApiInstance().getUserInfoApi().findUsers(newText, (List<String> users) -> {
            List<CharSequence> suggestions = new ArrayList<>();
            for (String sug : users) {
                suggestions.add(sug);
            }
            mAdapter.setItems(suggestions);
        }, null);
    }

}
