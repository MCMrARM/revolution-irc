package io.mrarm.irc.newui.group;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.UiThreadHelper;

public class GroupManager implements ServerConfigManager.ConnectionsListener {

    private static final String TAG = "GroupManager";

    private static final String CONFIG_PATH = "ui_groups.json";

    private static GroupManager sInstance;

    public static GroupManager getInstance(Context ctx) {
        if (sInstance == null)
            sInstance = new GroupManager(ctx);
        return sInstance;
    }

    private final Context mContext;
    private final File mConfigFile;
    private final Map<UUID, ServerGroupData> mServerMap = new HashMap<>();
    private final List<MasterGroup> mMasterGroups = new ArrayList<>(); // used for ordering
    private final Map<UUID, MasterGroup> mMasterGroupMap = new HashMap<>();
    private final Map<UUID, SubGroup> mSubGroupMap = new HashMap<>();

    public GroupManager(Context context) {
        mContext = context;
        mConfigFile = new File(context.getFilesDir(), CONFIG_PATH);
        load();
        for (ServerConfigData s : ServerConfigManager.getInstance(mContext).getServers()) {
            if (!mServerMap.containsKey(s.uuid))
                addServer(s);
        }
    }

    private void load() {
        ConfigFileData data;
        try (BufferedReader reader = new BufferedReader(new FileReader(mConfigFile))) {
            data = SettingsHelper.getGson().fromJson(reader, ConfigFileData.class);
        } catch (Exception e) {
            return;
        }
        ServerConfigManager mgr = ServerConfigManager.getInstance(mContext);
        for (ServerGroupData s : data.servers) {
            s.mServer = mgr.findServer(s.mServerUUID);
            if (s.mServer == null) {
                Log.e(TAG, "Could not find the associated server with the UUID: " +
                        s.mServerUUID + "; dropping");
                continue;
            }
            mServerMap.put(s.mServerUUID, s);
        }
        for (MasterGroup mg : data.masterGroups) {
            if (mMasterGroupMap.containsKey(mg.getUUID())) {
                Log.e(TAG, "Master group with the UUID already exists: " + mg.getUUID() +
                        " ; dropping");
                continue;
            }
            if (mg.mOwnerUUID != null)
                mg.setOwner(mServerMap.get(mg.mOwnerUUID));
            mMasterGroups.add(mg);
            mMasterGroupMap.put(mg.getUUID(), mg);

            List<SubGroup> newSubGroups = new ArrayList<>();
            for (SubGroup sg : mg.mSubGroups) {
                if (mSubGroupMap.containsKey(sg.getUUID())) {
                    Log.e(TAG, "Sub group with the UUID already exists: " + mg.getUUID() +
                            " ; dropping");
                    continue;
                }
                if (sg.mOwnerUUID != null)
                    sg.setOwner(mServerMap.get(mg.mOwnerUUID));
                newSubGroups.add(sg);
                mSubGroupMap.put(sg.getUUID(), sg);
            }
            mg.mSubGroups = newSubGroups;
        }
        for (ServerGroupData s : mServerMap.values()) {
            if (s.mDefaultSubGroupUUID != null) {
                s.setDefaultSubGroup(mSubGroupMap.get(s.mDefaultSubGroupUUID));
            }
        }
    }

    private void addServer(ServerConfigData s) {
        if (mServerMap.containsKey(s.uuid))
            return;
        ServerGroupData sd = new ServerGroupData(s);
        mServerMap.put(s.uuid, sd);
    }

    private void removeServer(ServerConfigData s) {
        mServerMap.remove(s.uuid);
    }

    @Override
    public void onConnectionAdded(ServerConfigData data) {
        UiThreadHelper.runOnUiThread(() -> addServer(data));
    }

    @Override
    public void onConnectionRemoved(ServerConfigData data) {
        UiThreadHelper.runOnUiThread(() -> removeServer(data));
    }

    @Override
    public void onConnectionUpdated(ServerConfigData data) {

    }

    private static class ConfigFileData {

        public List<ServerGroupData> servers;
        public List<MasterGroup> masterGroups;

    }

}
