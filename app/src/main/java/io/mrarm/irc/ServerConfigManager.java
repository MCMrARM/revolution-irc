package io.mrarm.irc;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerConfigManager {

    private static ServerConfigManager mInstance;

    private static String TAG = "ServerConfigManager";

    private static String SERVERS_PATH = "servers";
    private static String SERVER_FILE_PREFIX = "server-";
    private static String SERVER_FILE_SUFFIX = ".json";

    private static Gson mGson = new Gson();

    public static Gson getGson() {
        return mGson;
    }

    public static ServerConfigManager getInstance(Context context) {
        if (mInstance == null)
            mInstance = new ServerConfigManager(context.getApplicationContext());
        return mInstance;
    }

    private File mServersPath;

    private List<ServerConfigData> mServers = new ArrayList<>();
    private Map<UUID, ServerConfigData> mServersMap = new HashMap<>();
    private List<ConnectionsListener> mListeners = new ArrayList<>();

    public ServerConfigManager(Context context) {
        mServersPath = new File(context.getFilesDir(), SERVERS_PATH);
        mServersPath.mkdirs();
        loadServers();
    }

    private void loadServers() {
        File[] files = mServersPath.listFiles();
        if (files == null)
            return;
        for (File f : files) {
            if (!f.isFile() || !f.getName().startsWith(SERVER_FILE_PREFIX) || !f.getName().endsWith(SERVER_FILE_SUFFIX))
                continue;
            try {
                ServerConfigData data = mGson.fromJson(new BufferedReader(new FileReader(f)), ServerConfigData.class);
                mServers.add(data);
                mServersMap.put(data.uuid, data);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load server data");
                e.printStackTrace();
            }
        }
    }

    public List<ServerConfigData> getServers() {
        return mServers;
    }

    public ServerConfigData findServer(UUID uuid) {
        return mServersMap.get(uuid);
    }

    public void saveServer(ServerConfigData data) throws IOException {
        boolean existed = false;
        if (mServersMap.containsKey(data.uuid)) {
            existed = true;
            mServers.remove(mServersMap.get(data.uuid));
        }
        mServers.add(data);
        mServersMap.put(data.uuid, data);
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(mServersPath, SERVER_FILE_PREFIX + data.uuid.toString() + SERVER_FILE_SUFFIX)));
        mGson.toJson(data, writer);
        writer.close();

        if (existed) {
            for (ConnectionsListener listener : mListeners)
                listener.onConnectionUpdated(data);
        } else {
            for (ConnectionsListener listener : mListeners)
                listener.onConnectionAdded(data);
        }
    }

    public void deleteServer(ServerConfigData data) {
        mServers.remove(data);
        mServersMap.remove(data.uuid);
        File file = new File(mServersPath, SERVER_FILE_PREFIX + data.uuid.toString() + SERVER_FILE_SUFFIX);
        file.delete();
        for (ConnectionsListener listener : mListeners)
            listener.onConnectionRemoved(data);
    }

    public void addListener(ConnectionsListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(ConnectionsListener listener) {
        mListeners.remove(listener);
    }

    public interface ConnectionsListener {

        void onConnectionAdded(ServerConfigData data);

        void onConnectionRemoved(ServerConfigData data);

        void onConnectionUpdated(ServerConfigData data);

    }

}
