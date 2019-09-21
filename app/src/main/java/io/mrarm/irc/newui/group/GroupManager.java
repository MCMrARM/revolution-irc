package io.mrarm.irc.newui.group;

import androidx.databinding.ObservableList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.util.ExtendedObservableArrayList;
import io.mrarm.irc.util.ExtendedObservableList;

@Singleton
public class GroupManager {

    ServerConfigManager mConfigManager;

    private final ExtendedObservableList<MasterGroup> mMasterGroups = new ExtendedObservableArrayList<>();
    private final Map<UUID, MasterGroup> mMasterGroupUUIDMap = new HashMap<>();
    private final Map<UUID, ServerGroupData> mServerData = new HashMap<>();
    private DefaultInsertBeforeGroup mDefaultInsertBefore;

    @Inject
    public GroupManager(ServerConfigManager configManager) {
        mConfigManager = configManager;
        mMasterGroups.addExtendedListener(new ExtendedObservableList.SimpleExtendedListener<
                ExtendedObservableList<MasterGroup>, MasterGroup>() {

            @Override
            public void onAdded(ExtendedObservableList<MasterGroup> source, int index, MasterGroup value) {
                mMasterGroupUUIDMap.put(value.getUUID(), value);
            }

            @Override
            public void onRemove(ExtendedObservableList<MasterGroup> source, int index, MasterGroup value) {
                mMasterGroupUUIDMap.remove(value.getUUID());
            }

        });
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

    public MasterGroup getMasterGroup(UUID uuid) {
        return mMasterGroupUUIDMap.get(uuid);
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
