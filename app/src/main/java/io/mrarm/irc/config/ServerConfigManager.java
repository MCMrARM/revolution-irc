package io.mrarm.irc.config;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.mrarm.irc.ServerConnectionManager;

public class ServerConfigManager {

    private static ServerConfigManager mInstance;

    private static final String TAG = "ServerConfigManager";

    private static final String SERVERS_PATH = "servers";
    private static final String SERVER_FILE_PREFIX = "server-";
    private static final String SERVER_FILE_SUFFIX = ".json";
    private static final String SERVER_CERTS_FILE_PREFIX = "server-certs-";
    private static final String SERVER_CERTS_FILE_SUFFIX = ".jks";
    private static final String SERVER_LOGS_PATH = "chat-logs";
    private static final String SERVER_MISC_DATA_FILENAME = "misc-data.db";

    public static ServerConfigManager getInstance(Context context) {
        if (mInstance == null)
            mInstance = new ServerConfigManager(context.getApplicationContext());
        return mInstance;
    }

    private final File mServersPath;
    private final File mServerLogsPath;
    private final File mFallbackServerLogsPath;

    private final Context mContext;
    private final List<ServerConfigData> mServers = new ArrayList<>();
    private final Map<UUID, ServerConfigData> mServersMap = new HashMap<>();
    private final List<ConnectionsListener> mListeners = new ArrayList<>();
    private final Object mIOLock = new Object();

    public ServerConfigManager(Context context) {
        mContext = context;
        mServersPath = new File(context.getFilesDir(), SERVERS_PATH);
        File externalFilesDir = context.getExternalFilesDir(null);
        mFallbackServerLogsPath = new File(context.getFilesDir(), SERVER_LOGS_PATH);
        mServerLogsPath = externalFilesDir != null ? new File(externalFilesDir, SERVER_LOGS_PATH)
                : mFallbackServerLogsPath;
        mServerLogsPath.mkdirs();

        if (externalFilesDir != null && mFallbackServerLogsPath.exists()) {
            migrateServerLogs(mFallbackServerLogsPath, mServerLogsPath);
            mFallbackServerLogsPath.delete();
        }

        loadServers();
    }

    // NOTE: This is not synchronized; don't call it outside of the constructor
    private void loadServers() {
        File[] files = mServersPath.listFiles();
        if (files == null)
            return;
        for (File f : files) {
            if (!f.isFile() || !f.getName().startsWith(SERVER_FILE_PREFIX) || !f.getName().endsWith(SERVER_FILE_SUFFIX))
                continue;
            try {
                ServerConfigData data = SettingsHelper.getGson().fromJson(new BufferedReader(new FileReader(f)), ServerConfigData.class);
                data.migrateLegacyProperties();
                mServers.add(data);
                mServersMap.put(data.uuid, data);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load server data");
                e.printStackTrace();
            }
        }
    }

    private void migrateServerLogs(File from, File to) {
        File[] files = from.listFiles();
        if (files == null)
            return;
        to.mkdir();
        for (File file : files) {
            if (file.isDirectory()) {
                migrateServerLogs(file, new File(to, file.getName()));
            } else {
                File toFile = new File(to, file.getName());
                File tempFile = new File(to, file.getName() + ".tmp");
                try {
                    FileInputStream fis = new FileInputStream(file);
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    byte[] buf = new byte[16 * 1024];
                    int n;
                    while ((n = fis.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                    }
                    fis.close();
                    fos.close();
                } catch (IOException e) {
                    throw new RuntimeException("Migration failed", e);
                }
                if (toFile.exists())
                    toFile.delete();
                tempFile.renameTo(toFile);
            }
            file.delete();
        }
    }

    public List<ServerConfigData> getServers() {
        synchronized (this) {
            return new ArrayList<>(mServers);
        }
    }

    public ServerConfigData findServer(UUID uuid) {
        synchronized (this) {
            return mServersMap.get(uuid);
        }
    }

    public void saveServer(ServerConfigData data) throws IOException {
        boolean existed = false;
        synchronized (this) {
            if (mServersMap.containsKey(data.uuid)) {
                existed = true;
                mServers.remove(mServersMap.get(data.uuid));
            }
            mServers.add(data);
            mServersMap.put(data.uuid, data);
        }
        synchronized (mIOLock) {
            mServersPath.mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(mServersPath, SERVER_FILE_PREFIX + data.uuid.toString() + SERVER_FILE_SUFFIX)));
            SettingsHelper.getGson().toJson(data, writer);
            writer.close();
        }
        synchronized (mListeners) {
            if (existed) {
                for (ConnectionsListener listener : mListeners)
                    listener.onConnectionUpdated(data);
            } else {
                for (ConnectionsListener listener : mListeners)
                    listener.onConnectionAdded(data);
            }
        }
    }

    public void deleteServer(ServerConfigData data, boolean deleteLogs) {
        ServerConnectionManager.getInstance(mContext).killDisconnectingConnection(data.uuid);
        synchronized (this) {
            mServers.remove(data);
            mServersMap.remove(data.uuid);
        }
        synchronized (mIOLock) {
            File file = new File(mServersPath, SERVER_FILE_PREFIX + data.uuid.toString() + SERVER_FILE_SUFFIX);
            file.delete();
            file = getServerSSLCertsFile(data.uuid);
            file.delete();
            if (deleteLogs) {
                file = getServerChatLogDir(data.uuid);
                if (file.exists()) {
                    File[] files = file.listFiles();
                    for (File f : files)
                        f.delete();
                    file.delete();
                }
            }
        }
        synchronized (mListeners) {
            for (ConnectionsListener listener : mListeners)
                listener.onConnectionRemoved(data);
        }
        NotificationCountStorage.getInstance(mContext).requestRemoveServerCounters(data.uuid);
    }

    public void deleteServer(ServerConfigData data) {
        deleteServer(data, true);
    }

    public void deleteAllServers(boolean deleteLogs) {
        synchronized (this) {
            while (mServers.size() > 0)
                deleteServer(mServers.get(mServers.size() - 1), deleteLogs);
        }
    }

    public File getServerSSLCertsFile(UUID uuid) {
        return new File(mServersPath, SERVER_CERTS_FILE_PREFIX + uuid.toString() + SERVER_CERTS_FILE_SUFFIX);
    }

    public File getChatLogDir() {
        return mServerLogsPath;
    }

    public File getServerChatLogDir(UUID uuid) {
        return new File(mServerLogsPath, uuid.toString());
    }

    public File getServerMiscDataFile(UUID uuid) {
        return new File(getServerChatLogDir(uuid), SERVER_MISC_DATA_FILENAME);
    }

    public void addListener(ConnectionsListener listener) {
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    public void removeListener(ConnectionsListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    public interface ConnectionsListener {

        void onConnectionAdded(ServerConfigData data);

        void onConnectionRemoved(ServerConfigData data);

        void onConnectionUpdated(ServerConfigData data);

    }

}
