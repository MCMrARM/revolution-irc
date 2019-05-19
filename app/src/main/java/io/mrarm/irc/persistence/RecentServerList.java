package io.mrarm.irc.persistence;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.util.UiThreadHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@SuppressWarnings("ResultOfMethodCallIgnored")
@SuppressLint("CheckResult")
@Singleton
public class RecentServerList implements ServerConfigManager.ConnectionsListener {

    private final ServerConfigManager mServerConfigManager;
    private final UIDatabase mDatabase;
    private final ObservableList<ServerUIInfo> mServers = new ObservableArrayList<>();
    private boolean loadRequested = false;

    @Inject
    public RecentServerList(ServerConfigManager serverConfigManager, UIDatabase database) {
        mServerConfigManager = serverConfigManager;
        mDatabase = database;
        mServerConfigManager.addListener(this);
    }

    public ObservableList<ServerUIInfo> getServers() {
        return mServers;
    }

    private void moveToFrontAndUpdate(ServerUIInfo entry) {
        mServers.remove(entry);
        mServers.add(0, entry);

        mDatabase.serverInfoDao().maybeCreate(entry)
                .subscribeOn(Schedulers.io())
                .subscribe((result) -> {
                    if (result != -1)
                        return;
                    mDatabase.serverInfoDao().update(entry)
                            .subscribeOn(Schedulers.io())
                            .subscribe();
                });
    }

    private void addOlder(List<ServerUIInfo> addList) {
        Set<UUID> alreadyExistingUUIDs = new HashSet<>();
        for (ServerUIInfo s : mServers)
            alreadyExistingUUIDs.add(s.uuid);
        for (ServerUIInfo s : addList) {
            if (alreadyExistingUUIDs.contains(s.uuid))
                continue;
            mServers.add(s);
        }
    }

    private void addMissing() {
        Set<UUID> alreadyExistingUUIDs = new HashSet<>();
        for (ServerUIInfo s : mServers)
            alreadyExistingUUIDs.add(s.uuid);
        for (ServerConfigData srv : mServerConfigManager.getServers()) {
            if (alreadyExistingUUIDs.contains(srv.uuid))
                continue;
            mServers.add(new ServerUIInfo(srv.uuid, srv.name));
        }
    }

    public void load() {
        if (loadRequested) {
            Log.w("RecentServerList", "Trying to load more than once");
            return;
        }
        loadRequested = true;
        Log.v("RecentServerList", "Load requested");
        mDatabase.serverInfoDao().getList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((v) -> {
                            addOlder(v);
                            addMissing();
                            Log.v("RecentServerList", "Fetch done: " + mServers.size());
                        },
                        (error) -> {
                            Log.e("RecentServerList", "Fetch failed");
                            addMissing();
                        });
    }

    public void unload() {
    }

    private int findConnection(UUID uuid) {
        for (int i = 0; i < mServers.size(); i++) {
            if (mServers.get(i).uuid.equals(uuid))
                return i;
        }
        return -1;
    }

    public void makeInteraction(UUID uuid, String interactionType) {
        int iof = findConnection(uuid);
        if (iof == -1)
            return;
        ServerUIInfo info = mServers.get(iof);
        info.lastInteractionType = interactionType;
        info.lastInteractionTime = new Date();
        moveToFrontAndUpdate(info);
    }

    @Override
    public void onConnectionAdded(ServerConfigData data) {
        UiThreadHelper.runOnUiThread(() -> {
            int i = findConnection(data.uuid);
            if (i == -1)
                mServers.add(new ServerUIInfo(data.uuid, data.name));
            makeInteraction(data.uuid, ServerUIInfo.INTERACTION_TYPE_ADD);
        });
    }

    @Override
    public void onConnectionRemoved(ServerConfigData data) {
        UiThreadHelper.runOnUiThread(() -> {
            int i = findConnection(data.uuid);
            if (i != -1) {
                mDatabase.serverInfoDao().delete(mServers.get(i));
                mServers.remove(mServers.get(i));
            }
        });
    }

    @Override
    public void onConnectionUpdated(ServerConfigData data) {
        UiThreadHelper.runOnUiThread(() -> {
            makeInteraction(data.uuid, ServerUIInfo.INTERACTION_TYPE_EDIT);
        });
    }

}
