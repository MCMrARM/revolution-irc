package io.mrarm.irc.config;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import io.mrarm.irc.util.SettableFuture;

public class NotificationCountStorage {

    private static NotificationCountStorage sInstance;

    private static final int FLUSH_DELAY = 10 * 1000;

    private static final int DB_VERSION = 2;

    public static NotificationCountStorage getInstance(Context ctx) {
        if (sInstance == null)
            sInstance = new NotificationCountStorage(getFile(ctx).getAbsolutePath());
        return sInstance;
    }

    public static File getFile(Context ctx) {
        return new File(ctx.getFilesDir(), "notification-count.db");
    }

    private final String mPath;
    private final Object mDatabaseLock = new Object();
    private SQLiteDatabase mDatabase;
    private SQLiteStatement mGetNotificationCountStatement;
    private SQLiteStatement mIncrementNotificationCountStatement;
    private SQLiteStatement mCreateNotificationCountStatement;
    private SQLiteStatement mGetFirstMessageIdStatement;
    private SQLiteStatement mSetFirstMessageIdStatement;
    private SQLiteStatement mResetFirstMessageIdStatement;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Map<UUID, Map<String, Integer>> mChangeQueue;

    public NotificationCountStorage(String path) {
        mPath = path;
        mHandlerThread = new HandlerThread("NotificationCountStorage Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mHandler.post(this::open);
    }

    public void open() {
        synchronized (mDatabaseLock) {
            if (mDatabase != null)
                return;
            mDatabase = SQLiteDatabase.openOrCreateDatabase(mPath, null);
            if (mDatabase.getVersion() < DB_VERSION) {
                mDatabase.execSQL("DROP TABLE IF EXISTS 'notification_count'");
            }
            mDatabase.execSQL("CREATE TABLE IF NOT EXISTS 'notification_count' (server TEXT, channel TEXT, count INTEGER, firstMessageId TEXT)");
            mGetNotificationCountStatement = mDatabase.compileStatement("SELECT count FROM 'notification_count' WHERE server=?1 AND channel=?2");
            mIncrementNotificationCountStatement = mDatabase.compileStatement("UPDATE 'notification_count' SET count=count+?3 WHERE server=?1 AND channel=?2");
            mCreateNotificationCountStatement = mDatabase.compileStatement("INSERT INTO 'notification_count' (server, channel, count, firstMessageId) VALUES (?1, ?2, ?3, ?4)");
            mGetFirstMessageIdStatement = mDatabase.compileStatement("SELECT firstMessageId FROM 'notification_count' WHERE server=?1 AND channel=?2");
            mSetFirstMessageIdStatement = mDatabase.compileStatement("UPDATE 'notification_count' SET firstMessageId=?3 WHERE server=?1 AND channel=?2 AND firstMessageId IS NULL");
            mResetFirstMessageIdStatement = mDatabase.compileStatement("UPDATE 'notification_count' SET firstMessageId=NULL WHERE server=?1 AND channel=?2");
            mDatabase.setVersion(DB_VERSION);
        }
    }

    public void close() {
        SettableFuture<Void> s = new SettableFuture<>();
        mHandler.post(() -> {
            synchronized (mDatabaseLock) {
                if (mDatabase != null)
                    mDatabase.close();
                mDatabase = null;
                mGetNotificationCountStatement = null;
                mIncrementNotificationCountStatement = null;
                mCreateNotificationCountStatement = null;
                mGetFirstMessageIdStatement = null;
                mSetFirstMessageIdStatement = null;
                mResetFirstMessageIdStatement = null;
            }
            s.set(null);
        });
        try {
            s.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        // No more messages can be posted as this is .finalize; .close will finish after processing
        // all previous ones so it's safe to quit the HandlerThread not safely.
        mHandlerThread.quit();
        super.finalize();
    }

    private void waitForDatabase() {
        while (mDatabase == null) {
            try {
                mDatabaseLock.wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    private int getChannelCounter(UUID server, String channel) {
        synchronized (mDatabaseLock) {
            waitForDatabase();
            mGetNotificationCountStatement.bindString(1, server.toString());
            mGetNotificationCountStatement.bindString(2, channel);
            long ret;
            try {
                ret = mGetNotificationCountStatement.simpleQueryForLong();
            } catch (SQLiteDoneException e) {
                ret = 0;
            }
            mGetNotificationCountStatement.clearBindings();
            return (int) ret;
        }
    }

    private void incrementChannelCounter(UUID server, String channel, int i) {
        synchronized (mDatabaseLock) {
            waitForDatabase();
            mIncrementNotificationCountStatement.bindString(1, server.toString());
            mIncrementNotificationCountStatement.bindString(2, channel);
            mIncrementNotificationCountStatement.bindLong(3, i);
            int ii = mIncrementNotificationCountStatement.executeUpdateDelete();
            mIncrementNotificationCountStatement.clearBindings();
            if (ii == 0) {
                mCreateNotificationCountStatement.bindString(1, server.toString());
                mCreateNotificationCountStatement.bindString(2, channel);
                mCreateNotificationCountStatement.bindLong(3, i);
                mCreateNotificationCountStatement.bindNull(4);
                mCreateNotificationCountStatement.execute();
                mCreateNotificationCountStatement.clearBindings();
            }
        }
    }

    private void resetChannelCounter(UUID server, String channel) {
        synchronized (mDatabaseLock) {
            waitForDatabase();
            mDatabase.execSQL("DELETE FROM 'notification_count' WHERE server=?1 AND channel=?2",
                    new Object[]{server.toString(), channel});
        }
    }

    private void removeServerCounters(UUID server) {
        synchronized (mDatabaseLock) {
            waitForDatabase();
            mDatabase.execSQL("DELETE FROM 'notification_count' WHERE server=?1",
                    new Object[]{server.toString()});
        }
    }

    private String getFirstMessageId(UUID server, String channel) {
        synchronized (mDatabaseLock) {
            waitForDatabase();
            mGetFirstMessageIdStatement.bindString(1, server.toString());
            mGetFirstMessageIdStatement.bindString(2, channel);
            String ret;
            try {
                ret = mGetFirstMessageIdStatement.simpleQueryForString();
            } catch (SQLiteDoneException ignored) {
                ret = null;
            }
            mGetFirstMessageIdStatement.clearBindings();
            return ret;
        }
    }

    private void setFirstMessageId(UUID server, String channel, String messageId) {
        synchronized (mDatabaseLock) {
            waitForDatabase();
            mSetFirstMessageIdStatement.bindString(1, server.toString());
            mSetFirstMessageIdStatement.bindString(2, channel);
            mSetFirstMessageIdStatement.bindString(3, messageId);
            int ii = mSetFirstMessageIdStatement.executeUpdateDelete();
            mSetFirstMessageIdStatement.clearBindings();
            if (ii == 0) {
                mCreateNotificationCountStatement.bindString(1, server.toString());
                mCreateNotificationCountStatement.bindString(2, channel);
                mCreateNotificationCountStatement.bindLong(3, 0);
                mCreateNotificationCountStatement.bindString(4, messageId);
                mCreateNotificationCountStatement.execute();
                mCreateNotificationCountStatement.clearBindings();
            }
        }
    }

    private void resetFirstMessageId(UUID server, String channel) {
        synchronized (mDatabaseLock) {
            waitForDatabase();
            mResetFirstMessageIdStatement.bindString(1, server.toString());
            mResetFirstMessageIdStatement.bindString(2, channel);
            mResetFirstMessageIdStatement.executeUpdateDelete();
            mResetFirstMessageIdStatement.clearBindings();
        }
    }

    private void flushQueuedChanges() {
        Map<UUID, Map<String, Integer>> map;
        synchronized (this) {
            map = mChangeQueue;
            mChangeQueue = null;
        }
        if (map == null)
            return;
        for (Map.Entry<UUID, Map<String, Integer>> vals : map.entrySet()) {
            for (Map.Entry<String, Integer> v : vals.getValue().entrySet()) {
                incrementChannelCounter(vals.getKey(), v.getKey(), v.getValue());
            }
        }
    }

    public void requestGetChannelCounter(UUID server, String channel, WeakReference<OnChannelCounterResult> result) {
        mHandler.post(() -> {
            mHandler.removeCallbacks(mFlushQueuedChangesRunnable);
            flushQueuedChanges();

            int res = getChannelCounter(server, channel);
            String msgId = getFirstMessageId(server, channel);
            OnChannelCounterResult cb = result.get();
            if (cb != null)
                cb.onChannelCounterResult(server, channel, res, msgId);
        });
    }

    public void requestIncrementChannelCounter(UUID server, String channel) {
        synchronized (this) {
            boolean requestQueue = false;
            if (mChangeQueue == null) {
                mChangeQueue = new HashMap<>();
                requestQueue = true;
            }
            if (!mChangeQueue.containsKey(server))
                mChangeQueue.put(server, new HashMap<>());
            Map<String, Integer> m = mChangeQueue.get(server);
            Integer i = m.get(channel);
            m.put(channel, (i == null ? 0 : i) + 1);
            if (requestQueue)
                mHandler.postDelayed(mFlushQueuedChangesRunnable, FLUSH_DELAY);
        }
    }

    public void requestResetChannelCounter(UUID server, String channel) {
        synchronized (this) {
            if (mChangeQueue != null && mChangeQueue.containsKey(server))
                mChangeQueue.get(server).remove(channel);
        }
        mHandler.post(() -> {
            mHandler.removeCallbacks(mFlushQueuedChangesRunnable);
            flushQueuedChanges();

            resetChannelCounter(server, channel);
        });
    }

    public void requestRemoveServerCounters(UUID server) {
        mHandler.post(() -> {
            mHandler.removeCallbacks(mFlushQueuedChangesRunnable);
            flushQueuedChanges();

            removeServerCounters(server);
        });
    }

    public void requestSetFirstMessageId(UUID server, String channel, String messageId) {
        mHandler.post(() -> setFirstMessageId(server, channel, messageId));
    }

    public void requestResetFirstMessageId(UUID server, String channel) {
        mHandler.post(() -> resetFirstMessageId(server, channel));
    }

    public interface OnChannelCounterResult {
        void onChannelCounterResult(UUID server, String channel, int messages, String firstMessageId);
    }

    private final Runnable mFlushQueuedChangesRunnable = this::flushQueuedChanges;

}
