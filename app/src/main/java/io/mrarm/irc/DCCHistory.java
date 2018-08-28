package io.mrarm.irc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;

public class DCCHistory {

    private static final String TABLE_DCC_HISTORY = "dcc_history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ENTRY_TYPE = "entry_type";
    private static final String COLUMN_DATE = "entry_date";
    private static final String COLUMN_SERVER_NAME = "server_name";
    private static final String COLUMN_SERVER_UUID = "server_uuid";
    private static final String COLUMN_USER_NICK = "user_nick";
    private static final String COLUMN_REMOTE_ADDRESS = "remote_address";
    private static final String COLUMN_FILE_NAME = "file_name";
    private static final String COLUMN_FILE_SIZE = "file_size";
    private static final String COLUMN_FILE_URI = "file_uri";

    public static final int TYPE_DOWNLOAD = 0;
    public static final int TYPE_UPLOAD = 1;

    public static File getFile(Context ctx) {
        return new File(ctx.getFilesDir(), "dcc-history.db");
    }

    private final File mPath;
    private SQLiteDatabase mDatabase;
    private List<HistoryListener> mListeners = new ArrayList<>();

    public DCCHistory(Context context) {
        mPath = getFile(context);
        open();
    }

    private void open() {
        mDatabase = SQLiteDatabase.openOrCreateDatabase(mPath, null);
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS '" + TABLE_DCC_HISTORY + "' (" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_ENTRY_TYPE + " INTEGER," +
                COLUMN_DATE + " INTEGER," +
                COLUMN_SERVER_NAME + " TEXT," +
                COLUMN_SERVER_UUID + " BLOB," +
                COLUMN_USER_NICK + " TEXT," +
                COLUMN_REMOTE_ADDRESS + " TEXT," +
                COLUMN_FILE_NAME + " TEXT," +
                COLUMN_FILE_SIZE + " INTEGER," +
                COLUMN_FILE_URI + " TEXT" +
                ")");
    }

    public synchronized void addListener(HistoryListener listener) {
        mListeners.add(listener);
    }

    public synchronized void removeListener(HistoryListener listener) {
        mListeners.remove(listener);
    }

    public synchronized void addEntry(Entry entry) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ENTRY_TYPE, entry.entryType);
        values.put(COLUMN_DATE, entry.date.getTime());
        values.put(COLUMN_SERVER_NAME, entry.serverName);
        values.put(COLUMN_SERVER_UUID, entry.getServerUUIDBytes());
        values.put(COLUMN_USER_NICK, entry.userNick);
        if (entry.remoteAddress != null)
            values.put(COLUMN_REMOTE_ADDRESS, entry.remoteAddress);
        values.put(COLUMN_FILE_NAME, entry.fileName);
        values.put(COLUMN_FILE_SIZE, entry.fileSize);
        if (entry.fileUri != null)
            values.put(COLUMN_FILE_URI, entry.fileUri);
        entry.entryId = mDatabase.insert(TABLE_DCC_HISTORY, null, values);
        for (HistoryListener listener : mListeners)
            listener.onHistoryEntryCreated(entry);
    }

    public synchronized void removeEntry(long id) {
        mDatabase.delete(TABLE_DCC_HISTORY, COLUMN_ID + "=?",
                new String[] { String.valueOf(id) });
        for (HistoryListener listener : mListeners)
            listener.onHistoryEntryRemoved(id);
    }

    public synchronized int getEntryCount() {
        Cursor cursor = mDatabase.rawQuery("SELECT COUNT(*) FROM " + TABLE_DCC_HISTORY,
                null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    public synchronized List<Entry> getEntries(int offset, int limit) {
        List<Entry> entries = new ArrayList<>();
        Cursor cursor = mDatabase.rawQuery("SELECT * FROM " + TABLE_DCC_HISTORY +
                " ORDER BY " + COLUMN_DATE + " DESC" +
                (limit != -1 ? " LIMIT " + limit : "") +
                (offset != -1 ? " OFFSET " + offset : ""), null);
        int columnId = cursor.getColumnIndex(COLUMN_ID);
        int columnType = cursor.getColumnIndex(COLUMN_ENTRY_TYPE);
        int columnDate = cursor.getColumnIndex(COLUMN_DATE);
        int columnServerName = cursor.getColumnIndex(COLUMN_SERVER_NAME);
        int columnServerUUID = cursor.getColumnIndex(COLUMN_SERVER_UUID);
        int columnUserNick = cursor.getColumnIndex(COLUMN_USER_NICK);
        int columnRemoteAddress = cursor.getColumnIndex(COLUMN_REMOTE_ADDRESS);
        int columnFileName = cursor.getColumnIndex(COLUMN_FILE_NAME);
        int columnFileSize = cursor.getColumnIndex(COLUMN_FILE_SIZE);
        int columnFileUri = cursor.getColumnIndex(COLUMN_FILE_URI);
        while (cursor.moveToNext()) {
            Entry entry = new Entry();
            entry.entryId = cursor.getLong(columnId);
            entry.entryType = cursor.getInt(columnType);
            entry.date = new Date(cursor.getLong(columnDate));
            entry.serverName = cursor.getString(columnServerName);
            entry.setServerUUID(cursor.getBlob(columnServerUUID));
            entry.userNick = cursor.getString(columnUserNick);
            entry.remoteAddress = cursor.getString(columnRemoteAddress);
            entry.fileName = cursor.getString(columnFileName);
            entry.fileSize = cursor.getLong(columnFileSize);
            entry.fileUri = cursor.getString(columnFileUri);
            entries.add(entry);
        }
        return entries;
    }

    public interface HistoryListener {

        void onHistoryEntryCreated(Entry entry);

        void onHistoryEntryRemoved(long entryId);

    }


    public static class Entry {

        public long entryId;

        public int entryType;
        public Date date;

        public String serverName;
        public UUID serverUUID;
        public String userNick;
        public String remoteAddress;

        public String fileName;
        public long fileSize;

        public String fileUri;


        public Entry() {
        }

        public Entry(DCCManager.DownloadInfo download, Date date) {
            this.entryType = TYPE_DOWNLOAD;
            this.date = date;
            this.serverName = download.getServerName();
            this.serverUUID = download.getServerUUID();
            this.userNick = download.getSender().getNick();
            if (download.getClient() != null) {
                InetSocketAddress addr = (InetSocketAddress) download.getClient()
                        .getRemoteAddress();
                this.remoteAddress = addr.getAddress().getHostAddress() + ":" + addr.getPort();
            }

            this.fileName = download.getUnescapedFileName();
            this.fileSize = download.getFileSize();

            if (download.getDownloadedTo() != null)
                this.fileUri = download.getDownloadedTo().toString();
        }

        public Entry(DCCServerManager.UploadEntry upload, DCCServer.UploadSession session,
                     DCCManager.UploadServerInfo server, Date date) {
            this.entryType = TYPE_UPLOAD;
            this.date = date;
            this.serverName = server.getServerName();
            this.serverUUID = server.getServerUUID();
            this.userNick = upload.getUser();
            InetSocketAddress addr = (InetSocketAddress) session.getRemoteAddress();
            this.remoteAddress = addr.getAddress().getHostAddress() + ":" + addr.getPort();
            this.fileName = upload.getFileName();
            this.fileSize = session.getAcknowledgedSize();
        }

        byte[] getServerUUIDBytes() {
            ByteBuffer b = ByteBuffer.wrap(new byte[16]);
            b.putLong(serverUUID.getMostSignificantBits());
            b.putLong(serverUUID.getLeastSignificantBits());
            return b.array();
        }

        void setServerUUID(byte[] bytes) {
            ByteBuffer b = ByteBuffer.wrap(bytes);
            serverUUID = new UUID(b.getLong(), b.getLong());
        }
    }
}
