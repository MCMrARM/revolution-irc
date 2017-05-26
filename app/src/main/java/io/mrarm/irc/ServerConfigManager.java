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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerConfigManager {

    private static ServerConfigManager mInstance;

    private static String TAG = "ServerConfigManager";

    private static String SERVERS_PATH = "servers";
    private static String SERVER_FILE_PREFIX = "server-";
    private static String SERVER_FILE_SUFFIX = ".json";

    private static Gson mGson = new Gson();

    public static ServerConfigManager getInstance(Context context) {
        if (mInstance == null)
            mInstance = new ServerConfigManager(context);
        return mInstance;
    }

    private File mServersPath;

    private Map<UUID, ServerConfigData> mEntries = new HashMap<>();

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
                mEntries.put(data.uuid, data);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load server data");
                e.printStackTrace();
            }
        }
    }

    public void saveServer(ServerConfigData data) throws IOException {
        mEntries.put(data.uuid, data);
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(mServersPath, SERVER_FILE_PREFIX + data.uuid.toString() + SERVER_FILE_SUFFIX)));
        mGson.toJson(data, writer);
        writer.close();
    }


}
