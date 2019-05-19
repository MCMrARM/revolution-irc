package io.mrarm.irc.newui;

import android.content.Context;

import androidx.databinding.ObservableList;

import java.util.UUID;

import javax.inject.Inject;

import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.dagger.FragmentQualifier;
import io.mrarm.irc.dagger.FragmentScope;
import io.mrarm.irc.persistence.RecentServerList;
import io.mrarm.irc.persistence.ServerUIInfo;
import io.mrarm.observabletransform.ObservableLists;
import io.mrarm.observabletransform.TransformedObservableList;

@FragmentScope
public class ServerInactiveListData implements ServerConnectionManager.ConnectionsListener {

    private final Context mContext;
    private final RecentServerList mRecentServers;
    private final TransformedObservableList<ServerUIInfo> mInactiveConnections;

    @Inject
    public ServerInactiveListData(@FragmentQualifier Context context,
                                  RecentServerList recentServers) {
        mContext = context;
        mRecentServers = recentServers;
        mInactiveConnections = ObservableLists.filter(mRecentServers.getServers(),
                (c) -> !ServerConnectionManager.getInstance(context).hasConnection(c.uuid));
    }

    public ObservableList<ServerUIInfo> getInactiveConnections() {
        return mInactiveConnections;
    }

    public void load() {
        mRecentServers.load();
        mInactiveConnections.bind();
        ServerConnectionManager.getInstance(mContext).addListener(this);
    }

    public void unload() {
        mRecentServers.unload();
        ServerConnectionManager.getInstance(mContext).removeListener(this);
        mInactiveConnections.unbind();
    }


    private int findConnection(UUID uuid) {
        for (int i = mRecentServers.getServers().size() - 1; i >= 0; --i) {
            if (mRecentServers.getServers().get(i).uuid.equals(uuid))
                return i;
        }
        return -1;
    }

    @Override
    public void onConnectionAdded(ServerConnectionInfo connection) {
        int index = findConnection(connection.getUUID());
        if (index != -1)
            mInactiveConnections.reapply(index, 1);
    }

    @Override
    public void onConnectionRemoved(ServerConnectionInfo connection) {
        int index = findConnection(connection.getUUID());
        if (index != -1)
            mInactiveConnections.reapply(index, 1);
    }
}
