package io.mrarm.irc.newui.group;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;

@Singleton
public class GroupManager {

    ServerConfigManager mConfigManager;

    private final ObservableList<MasterGroup> mMasterGroups = new ObservableArrayList<>();
    private final Map<UUID, ServerGroupData> mServerData = new HashMap<>();
    private DefaultInsertBeforeGroup mDefaultInsertBefore;

    @Inject
    public GroupManager(ServerConfigManager configManager) {
        mConfigManager = configManager;
        initDefault();
    }

    private void initDefault() {
        mMasterGroups.clear();
        mServerData.clear();
        mDefaultInsertBefore = new DefaultInsertBeforeGroup();
        MasterGroup mg = new MasterGroup(UUID.randomUUID());
        mg.add(mDefaultInsertBefore);
        mMasterGroups.add(mg);

        for (ServerConfigData s : mConfigManager.getServers())
            createServerData(s.uuid);
    }


    public ObservableList<MasterGroup> getMasterGroups() {
        return mMasterGroups;
    }

    private void createServerData(UUID uuid) {
        if (mServerData.containsKey(uuid))
            return;
        ServerGroupData data = new ServerGroupData(this, uuid);
        DefaultServerGroup gr = data.createDefaultGroup();
        int iof = mDefaultInsertBefore.getParent().getGroups().indexOf(mDefaultInsertBefore);
        mDefaultInsertBefore.getParent().getGroups().add(iof, gr);
        mServerData.put(uuid, data);
    }

    public ServerGroupData getServerData(UUID uuid) {
        return mServerData.get(uuid);
    }

}
