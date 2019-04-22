package io.mrarm.irc.newui.group;

import android.content.Context;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.InvalidParameterException;
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

    private DefaultGroupingMethod mDefaultGroupingMethod = DefaultGroupingMethod.MASTER_GROUP_PER_SERVER;
    private MasterGroup mDefaultMasterGroup;
    private SubGroup mDefaultSubGroup;

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
        if (data == null)
            return;
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
                sg.mParent = mg;
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
        mDefaultGroupingMethod = data.options.defaultGroupingMethod;
        mDefaultMasterGroup = mMasterGroupMap.get(data.options.defaultMasterGroupUUID);
        mDefaultSubGroup = mSubGroupMap.get(data.options.defaultSubGroupUUID);
    }

    private void addServer(ServerConfigData s) {
        if (mServerMap.containsKey(s.uuid))
            return;
        ServerGroupData sd = new ServerGroupData(s);
        mServerMap.put(s.uuid, sd);
        setServerGroups(sd, mDefaultGroupingMethod, mDefaultMasterGroup, mDefaultSubGroup);
    }

    private void removeServer(ServerConfigData s) {
        ServerGroupData sd = mServerMap.remove(s.uuid);
        if (sd != null)
            removeServerGroups(sd);
    }

    private void removeServerGroups(ServerGroupData sd) {
        if (sd.mDefaultSubGroup == null || sd.mDefaultSubGroupCustom)
            return;
        SubGroup sg = sd.mDefaultSubGroup;
        MasterGroup mg = sg.getParent();
        if (sg.getOwner() == sd) {
            mg.removeSubGroup(sg);
            removeSubGroup(sg);
        }
        if (mg.getOwner() == sd)
            removeMasterGroup(mg);
    }

    private void createServerGroups(ServerGroupData sd, DefaultGroupingMethod method,
                                    MasterGroup mg, SubGroup sg) {
        if (method == DefaultGroupingMethod.MASTER_GROUP_PER_SERVER) {
            mg = new MasterGroup();
            addMasterGroup(mg);
            mg.setOwner(sd);
            sg = new SubGroup();
            addSubGroup(sg);
            sg.setOwner(sd);
            mg.addSubGroup(sg);
            sd.setDefaultSubGroup(sg);
        } else if (method == DefaultGroupingMethod.SUB_GROUP_PER_SERVER) {
            sg = new SubGroup();
            addSubGroup(sg);
            sg.setOwner(sd);
            mg.addSubGroup(sg);
            sd.setDefaultSubGroup(sg);
        } else if (method == DefaultGroupingMethod.SINGLE_GROUP) {
            sd.setDefaultSubGroup(sg);
        }
    }

    private void setServerGroups(ServerGroupData sd, DefaultGroupingMethod method,
                                 MasterGroup mg, SubGroup sg) {
        // Check whether the target method isn't the current one
        if (method == DefaultGroupingMethod.MASTER_GROUP_PER_SERVER) {
            if (sd.mDefaultSubGroup != null && sd.mDefaultSubGroup.getParent().getOwner() == sd)
                return;
        } else if (method == DefaultGroupingMethod.SUB_GROUP_PER_SERVER) {
            if (sd.mDefaultSubGroup != null && sd.mDefaultSubGroup.getOwner() == sd &&
                    sd.mDefaultSubGroup.getParent() == mg)
                return;
        } else if (method == DefaultGroupingMethod.SINGLE_GROUP) {
            if (sd.mDefaultSubGroup != null && sd.mDefaultSubGroup == sg)
                return;
        }
        removeServerGroups(sd);
        createServerGroups(sd, method, mg, sg);
    }

    public void setDefaultGroups(DefaultGroupingMethod method, MasterGroup mg, SubGroup sg) {
        mDefaultGroupingMethod = method;
        mDefaultMasterGroup = mg;
        mDefaultSubGroup = sg;
        for (ServerGroupData sd : mServerMap.values()) {
            if (sd.mDefaultSubGroupCustom)
                continue;
            setServerGroups(sd, method, mg, sg);
        }
    }

    public void setServerOverrideGroups(ServerGroupData sd, DefaultGroupingMethod method,
                                        MasterGroup mg, SubGroup sg) {
        if (method != null) {
            setServerGroups(sd, method, mg, sg);
            sd.mDefaultSubGroupCustom = true;
        } else {
            setServerGroups(sd, mDefaultGroupingMethod, mDefaultMasterGroup, mDefaultSubGroup);
            sd.mDefaultSubGroupCustom = false;
        }
    }

    public void addMasterGroup(MasterGroup g) {
        if (g.mUUID == null)
            g.mUUID = UUID.randomUUID();
        if (mMasterGroupMap.containsKey(g.mUUID))
            throw new InvalidParameterException("A master group with the specified UUID " +
                    "has already been added: " + g.mUUID);
        mMasterGroupMap.put(g.mUUID, g);
    }

    public void removeMasterGroup(MasterGroup g) {
        MasterGroup dmg = mMasterGroupMap.get(g.mUUID);
        if (dmg != g)
            throw new InvalidParameterException("The specified sub group has not been added " +
                    "to the manager");
        mMasterGroups.remove(g);
        mMasterGroupMap.remove(g.mUUID);
        for (SubGroup sg : g.mSubGroups)
            mSubGroupMap.remove(sg.mUUID);
    }

    public void addSubGroup(SubGroup g) {
        if (g.mUUID == null)
            g.mUUID = UUID.randomUUID();
        if (mSubGroupMap.containsKey(g.mUUID))
            throw new InvalidParameterException("A sub group with the specified UUID " +
                    "has already been added: " + g.mUUID);
        mSubGroupMap.put(g.mUUID, g);
    }

    public void removeSubGroup(SubGroup g) {
        SubGroup dsg = mSubGroupMap.get(g.mUUID);
        if (dsg != g)
            throw new InvalidParameterException("The specified sub group has not been added " +
                    "to the manager");
        mSubGroupMap.remove(g.mUUID);
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

    public enum DefaultGroupingMethod {
        @SerializedName("master_group_per_server")
        MASTER_GROUP_PER_SERVER,
        @SerializedName("sub_group_per_server")
        SUB_GROUP_PER_SERVER,
        @SerializedName("single_group")
        SINGLE_GROUP
    }

    private static class DefaultOptions {

        private DefaultGroupingMethod defaultGroupingMethod;
        private UUID defaultMasterGroupUUID;
        private UUID defaultSubGroupUUID;

    }

    private static class ConfigFileData {

        public DefaultOptions options;
        public List<ServerGroupData> servers;
        public List<MasterGroup> masterGroups;

    }

}
