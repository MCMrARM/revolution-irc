package io.mrarm.irc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StatFs;
import androidx.annotation.NonNull;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executor;

import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.SettingChangeCallback;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.PoolSerialExecutor;

public class ChatLogStorageManager implements ServerConfigManager.ConnectionsListener {

    private static final int MIN_GLOBAL_MESSAGES_UPDATE = 1024;
    private static final int MIN_SERVER_MESSAGES_UPDATE = 128;

    private static final int GLOBAL_DELETION_CANDIDATES = 1024;
    private static final int CONNECTION_DELETION_CANDIDATES = 32;

    private static final SimpleDateFormat sFileNameFormat = new SimpleDateFormat("'messages-'yyyy-MM-dd'.db'", Locale.getDefault());

    private static ChatLogStorageManager sInstance;

    public static ChatLogStorageManager getInstance(Context context) {
        if (sInstance == null)
            sInstance = new ChatLogStorageManager(context);
        return sInstance;
    }

    private ServerConnectionManager mConnectionManager;
    private ServerConfigManager mServerConfigManager;
    private long mBlockSize = 0L;
    private Map<UUID, ServerManager> mServerManagers = new HashMap<>();
    private int mGlobalMessageCounter = 0;
    private TreeSet<DeletionCandidate> mGlobalDeletionCandidates = new TreeSet<>();
    private long mGlobalTotalSize = 0L;
    private long mGlobalLimit;
    private long mDefaultServerLimit;
    private Executor mExecutor;

    public ChatLogStorageManager(Context context) {
        mConnectionManager = ServerConnectionManager.getInstance(context);
        mServerConfigManager = ServerConfigManager.getInstance(context);

        SettingsHelper.registerCallbacks(this);
        onSettingChanged();

        mExecutor = new PoolSerialExecutor();

        mServerConfigManager.addListener(this);
        List<ServerConfigData> servers = mServerConfigManager.getServers();
        mExecutor.execute(() -> {
            for (ServerConfigData data : servers)
                mServerManagers.put(data.uuid, new ServerManager(data));
            performUpdate(null);
        });
    }

    @SettingChangeCallback(keys = {
            AppSettings.PREF_STORAGE_LIMIT_GLOBAL,
            AppSettings.PREF_STORAGE_LIMIT_SERVER
    })
    private void onSettingChanged() {
        mGlobalLimit = AppSettings.getStorageLimitGlobal();
        mDefaultServerLimit = AppSettings.getStorageLimitServer();
    }

    public void requestUpdate(UUID serverUUID) {
        mExecutor.execute(() -> performUpdate(serverUUID));
    }

    public void requestUpdate(UUID serverUUID, Runnable callback) {
        mExecutor.execute(() -> {
            performUpdate(serverUUID);
            callback.run();
        });
    }

    private void performUpdate(UUID serverUUID) {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        if (serverUUID == null) {
            for (ServerManager manager : mServerManagers.values())
                manager.update(currentYear, currentMonth, currentDay);
        } else {
            ServerManager manager = mServerManagers.get(serverUUID);
            if (manager != null)
                manager.update(currentYear, currentMonth, currentDay);
        }
        if (mGlobalLimit != -1L && mGlobalTotalSize > mGlobalLimit)
            performGlobalDeletion(mGlobalTotalSize - mGlobalLimit);
    }

    private void performGlobalDeletion(long size) {
        while (true) {
            boolean deletedAnyFile = false;
            for (Iterator<DeletionCandidate> iterator = mGlobalDeletionCandidates.iterator(); iterator.hasNext(); ) {
                DeletionCandidate candidate = iterator.next();
                mGlobalTotalSize -= candidate.size;
                File file = new File(candidate.server.mLogsDir, sFileNameFormat.format(candidate.dateMs));
                size -= getFileSize(file);
                SettingsHelper.deleteSQLiteDatabase(file);
                deletedAnyFile = true;
                iterator.remove();
                if (size <= 0L) {
                    if (mGlobalDeletionCandidates.size() == 0) {
                        // If there are no deletion candidates left, refresh the list so they are
                        // available during the next performGlobalDeletion call.
                        for (ServerManager manager : mServerManagers.values())
                            manager.reload();
                    }
                    return;
                }
            }
            if (!deletedAnyFile)
                return;
            // Refresh deletion candidates
            for (ServerManager manager : mServerManagers.values())
                manager.reload();
        }
    }

    public void onMessage(ServerConnectionInfo connection) {
        if (++mGlobalMessageCounter >= MIN_GLOBAL_MESSAGES_UPDATE) {
            requestUpdate(null);
            for (ServerConnectionInfo info : mConnectionManager.getConnections())
                info.mChatLogStorageUpdateCounter = 0;
            mGlobalMessageCounter = 0;
            return;
        }
        if (++connection.mChatLogStorageUpdateCounter >= MIN_SERVER_MESSAGES_UPDATE) {
            requestUpdate(connection.getUUID());
            connection.mChatLogStorageUpdateCounter = 0;
        }
    }

    private void addGlobalDeletionCandidate(DeletionCandidate candidate) {
        if (mGlobalDeletionCandidates.size() < GLOBAL_DELETION_CANDIDATES) {
            mGlobalDeletionCandidates.add(candidate);
        } else if (mGlobalDeletionCandidates.lower(candidate) != null) {
            mGlobalDeletionCandidates.add(candidate);
            mGlobalDeletionCandidates.remove(mGlobalDeletionCandidates.last());
        }
    }

    private long getBlockSize() {
        if (mBlockSize == 0L) {
            File chatLogDir = mServerConfigManager.getChatLogDir();
            if (!chatLogDir.exists())
                return 0L;
            StatFs statFs = new StatFs(chatLogDir.getAbsolutePath());
            if (Build.VERSION.SDK_INT >= 18)
                mBlockSize = statFs.getBlockSizeLong();
            else
                mBlockSize = statFs.getBlockSize();
        }
        return mBlockSize;
    }

    private long getFileSize(File file) {
        long blockSize = getBlockSize();
        return (file.length() + blockSize - 1) / blockSize * blockSize;
    }

    @Override
    public void onConnectionAdded(ServerConfigData data) {
        mExecutor.execute(() -> {
            mServerManagers.put(data.uuid, new ServerManager(data));
        });
    }

    @Override
    public void onConnectionRemoved(ServerConfigData data) {
        mExecutor.execute(() -> {
            mServerManagers.get(data.uuid).remove();
            mServerManagers.remove(data.uuid);
        });
    }

    @Override
    public void onConnectionUpdated(ServerConfigData data) {
        requestUpdate(data.uuid);
    }

    public class ServerManager {

        private ServerConfigData mServerConfig;
        private File mLogsDir;
        private long mTotalSize = 0L;
        private Calendar mCurrentLogTime;
        private File mCurrentLogFile;
        private long mCurrentLogSize = 0L;
        private TreeSet<DeletionCandidate> mDeletionCandidates = new TreeSet<>();

        public ServerManager(ServerConfigData config) {
            mServerConfig = config;
            mCurrentLogTime = Calendar.getInstance();
            mLogsDir = mServerConfigManager.getServerChatLogDir(config.uuid);
            reload();
        }

        private void reload() {
            remove();
            File[] files = mLogsDir.listFiles();
            if (files == null)
                return;
            mTotalSize = getBlockSize();
            int currentYear = mCurrentLogTime.get(Calendar.YEAR);
            int currentMonth = mCurrentLogTime.get(Calendar.MONTH);
            int currentDay = mCurrentLogTime.get(Calendar.DAY_OF_MONTH);
            Calendar calendar = Calendar.getInstance();
            for (File file : files) {
                long fileSize = getFileSize(file);
                mTotalSize += fileSize;
                Date date;
                try {
                    date = sFileNameFormat.parse(file.getName());
                } catch (ParseException ignored) {
                    continue;
                }
                calendar.setTime(date);
                if (calendar.get(Calendar.YEAR) == currentYear &&
                        calendar.get(Calendar.MONTH) == currentMonth &&
                        calendar.get(Calendar.DAY_OF_MONTH) == currentDay) {
                    mCurrentLogFile = file;
                    mCurrentLogSize = fileSize;
                    continue;
                }
                DeletionCandidate candidate = new DeletionCandidate(this, fileSize, date.getTime());
                addDeletionCandidate(candidate);
                addGlobalDeletionCandidate(candidate);
            }
            mGlobalTotalSize += mTotalSize;
        }

        public void remove() {
            mGlobalTotalSize -= mTotalSize;
            mDeletionCandidates.clear();
            for (Iterator<DeletionCandidate> iterator = mGlobalDeletionCandidates.iterator(); iterator.hasNext(); ) {
                DeletionCandidate candidate = iterator.next();
                if (candidate.server == this)
                    iterator.remove();
            }
            mTotalSize = 0L;
            mCurrentLogFile = null;
            mCurrentLogSize = 0L;
        }

        public void update(int currentYear, int currentMonth, int currentDay) {
            mTotalSize -= mCurrentLogSize;
            mGlobalTotalSize -= mCurrentLogSize;
            mCurrentLogSize = mCurrentLogFile == null ? 0L : getFileSize(mCurrentLogFile);
            mTotalSize += mCurrentLogSize;
            mGlobalTotalSize += mCurrentLogSize;

            long blockSize = getBlockSize();
            while (currentYear > mCurrentLogTime.get(Calendar.YEAR) || (currentYear == mCurrentLogTime.get(Calendar.YEAR) &&
                    (currentMonth > mCurrentLogTime.get(Calendar.MONTH) || (currentMonth == mCurrentLogTime.get(Calendar.MONTH) &&
                            currentDay > mCurrentLogTime.get(Calendar.DAY_OF_MONTH))))) {
                if (mCurrentLogFile != null && mCurrentLogFile.exists()) {
                    DeletionCandidate candidate = new DeletionCandidate(this, mCurrentLogSize,
                            mCurrentLogTime.getTimeInMillis());
                    addDeletionCandidate(candidate);
                    addGlobalDeletionCandidate(candidate);
                }

                mCurrentLogTime.add(Calendar.DAY_OF_MONTH, 1);
                mCurrentLogFile = new File(mLogsDir, sFileNameFormat.format(mCurrentLogTime.getTime()));
                mCurrentLogSize = (mCurrentLogFile.length() + blockSize - 1) / blockSize * blockSize;
                mTotalSize += mCurrentLogSize;
                mGlobalTotalSize += mCurrentLogSize;
            }

            long limit = mServerConfig.storageLimit != 0L ? mServerConfig.storageLimit : mDefaultServerLimit;
            if (limit != -1L && mTotalSize >= limit)
                performDeletion(mTotalSize - limit);
        }

        private void performDeletion(long size) {
            while (true) {
                boolean deletedAnyFile = false;
                for (Iterator<DeletionCandidate> iterator = mDeletionCandidates.iterator(); iterator.hasNext(); ) {
                    DeletionCandidate candidate = iterator.next();
                    mGlobalTotalSize -= candidate.size;
                    File file = new File(candidate.server.mLogsDir, sFileNameFormat.format(candidate.dateMs));
                    size -= getFileSize(file);
                    SettingsHelper.deleteSQLiteDatabase(file);
                    deletedAnyFile = true;
                    iterator.remove();
                    if (size <= 0L) {
                        if (mGlobalDeletionCandidates.size() == 0)
                            reload();
                        return;
                    }
                }
                if (!deletedAnyFile)
                    return;
                reload();
            }
        }

        private void addDeletionCandidate(DeletionCandidate candidate) {
            if (mDeletionCandidates.size() < CONNECTION_DELETION_CANDIDATES) {
                mDeletionCandidates.add(candidate);
            } else if (mDeletionCandidates.lower(candidate) != null) {
                mDeletionCandidates.add(candidate);
                mDeletionCandidates.remove(mDeletionCandidates.last());
            }
        }

    }


    private static class DeletionCandidate implements Comparable<DeletionCandidate> {

        private ServerManager server;
        private long size;
        private long dateMs;

        public DeletionCandidate(ServerManager server, long size, long dateMs) {
            this.server = server;
            this.size = size;
            this.dateMs = dateMs;
        }

        @Override
        public int compareTo(@NonNull DeletionCandidate o) {
            if (dateMs != o.dateMs)
                return Long.compare(o.dateMs, dateMs);
            if (size != o.size)
                return Long.compare(size, o.size);
            return 0;
        }

    }

}
