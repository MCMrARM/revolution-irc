package io.mrarm.irc;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StatFs;
import android.support.annotation.NonNull;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import io.mrarm.irc.config.ServerConfigManager;

public class ChatLogStorageManager {

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
    private long mBlockSize = 0L;
    private int mGlobalMessageCounter = 0;
    private TreeSet<DeletionCandidate> mGlobalDeletionCandidates = new TreeSet<>();
    private Map<UUID, ServerManager> mServerManagers = new HashMap<>();
    private long mGlobalTotalSize = 0L;
    private HandlerThread mUpdateThread;
    private Handler mUpdateThreadHandler;

    public ChatLogStorageManager(Context context) {
        mConnectionManager = ServerConnectionManager.getInstance(context);
        File chatLogDir = ServerConfigManager.getInstance(context).getChatLogDir();
        StatFs statFs = new StatFs(chatLogDir.getAbsolutePath());
        if (Build.VERSION.SDK_INT >= 18)
            mBlockSize = statFs.getBlockSizeLong();
        else
            mBlockSize = statFs.getBlockSize();

        mUpdateThread = new HandlerThread("ChatLogStorageManager");
        mUpdateThread.start();
        mUpdateThreadHandler = new Handler(mUpdateThread.getLooper());
    }

    private void requestUpdate(UUID serverUUID) {
        mUpdateThreadHandler.post(() -> performUpdate(serverUUID));
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

    private long getFileSize(File file) {
        return (file.length() + mBlockSize - 1) / mBlockSize * mBlockSize;
    }

    public class ServerManager {

        private File mLogsDir;
        private long mTotalSize = 0L;
        private Calendar mCurrentLogTime;
        private File mCurrentLogFile;
        private long mCurrentLogSize = 0L;
        private TreeSet<DeletionCandidate> mDeletionCandidates = new TreeSet<>();

        public ServerManager(File logsDir) {
            mLogsDir = logsDir;
            mCurrentLogTime = Calendar.getInstance();

            File[] files = logsDir.listFiles();
            if (files == null)
                return;
            mTotalSize = mBlockSize;
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
        }

        public void update(int currentYear, int currentMonth, int currentDay) {
            mTotalSize -= mCurrentLogSize;
            mGlobalTotalSize -= mCurrentLogSize;
            mCurrentLogSize = getFileSize(mCurrentLogFile);
            mTotalSize += mCurrentLogSize;
            mGlobalTotalSize += mCurrentLogSize;

            while (currentYear > mCurrentLogTime.get(Calendar.YEAR) ||
                    currentMonth > mCurrentLogTime.get(Calendar.MONTH) ||
                    currentDay > mCurrentLogTime.get(Calendar.DAY_OF_MONTH)) {
                if (mCurrentLogFile.exists()) {
                    DeletionCandidate candidate = new DeletionCandidate(this, mCurrentLogSize,
                            mCurrentLogTime.getTimeInMillis());
                    addDeletionCandidate(candidate);
                    addGlobalDeletionCandidate(candidate);
                }

                mCurrentLogTime.add(Calendar.DAY_OF_MONTH, 1);
                mCurrentLogFile = new File(mLogsDir, sFileNameFormat.format(mCurrentLogTime.getTime()));
                mCurrentLogSize = (mCurrentLogFile.length() + mBlockSize - 1) / mBlockSize * mBlockSize;
                mTotalSize += mCurrentLogSize;
                mGlobalTotalSize += mCurrentLogSize;
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
