package io.mrarm.irc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.mrarm.chatlib.irc.MessagePrefix;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.irc.ServerConnectionData;
import io.mrarm.chatlib.irc.dcc.DCCClient;
import io.mrarm.chatlib.irc.dcc.DCCClientManager;
import io.mrarm.chatlib.irc.dcc.DCCReverseClient;
import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;
import io.mrarm.chatlib.irc.dcc.DCCUtils;
import io.mrarm.irc.upnp.PortMapper;
import io.mrarm.irc.upnp.rpc.AddPortMappingCall;
import io.mrarm.irc.util.FormatUtils;

public class DCCManager implements DCCServerManager.UploadListener, DCCClient.CloseListener,
        DCCReverseClient.StateListener {

    private static DCCManager sInstance;

    private static final String PREF_DCC_ASKED_FOR_PERMISSION = "dcc_storage_permission_asked";
    private static final String PREF_DCC_ALWAYS_USE_APP_DOWNLOAD_DIR = "dcc_force_application_download_directory";
    private static final String PREF_DCC_DIRECTORY_OVERRIDE_URI = "dcc_download_directory_uri";
    private static final String PREF_DCC_DIRECTORY_OVERRIDE_URI_SYSTEM = "dcc_download_directory_uri_system";

    public static DCCManager getInstance(Context context) {
        if (sInstance == null)
            sInstance = new DCCManager(context.getApplicationContext());
        return sInstance;
    }

    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final DCCServerManager mServer;
    private final DCCHistory mHistory;
    private final Map<DCCServer, DCCServerManager.UploadEntry> mUploads = new HashMap<>();
    private final Map<DCCServerManager.UploadEntry, UploadServerInfo> mUploadServers = new HashMap<>();
    private final Map<DCCServerManager.UploadEntry, PortMapper.PortMappingResult> mUploadPortMappings = new HashMap<>();
    private final List<DCCServer.UploadSession> mSessions = new ArrayList<>();
    private final List<DownloadInfo> mDownloads = new ArrayList<>();
    private final List<DownloadListener> mDownloadListeners = new ArrayList<>();
    private File mDownloadDirectory;
    private Uri mDownloadDirectoryOverrideURI;
    private boolean mIsDownloadDirectoryOverrideURISystem;
    private final File mFallbackDownloadDirectory;
    private boolean mAlwaysUseFallbackDir;
    private boolean mHasSystemDirectoryAccess;
    private final DCCNotificationManager mNotificationManager;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public DCCManager(Context context) {
        mContext = context;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mNotificationManager = new DCCNotificationManager(mContext);
        mFallbackDownloadDirectory = mContext.getExternalFilesDir("downloads");
        mDownloadDirectory = mFallbackDownloadDirectory;
        mHistory = new DCCHistory(context);
        mServer = new DCCServerManager();
        mServer.addUploadListener(this);
        mServer.addUploadListener(mNotificationManager);
        addDownloadListener(mNotificationManager);
        mAlwaysUseFallbackDir = mPreferences.getBoolean(PREF_DCC_ALWAYS_USE_APP_DOWNLOAD_DIR, false);
        String uri = mPreferences.getString(PREF_DCC_DIRECTORY_OVERRIDE_URI, null);
        if (uri != null) {
            mDownloadDirectoryOverrideURI = Uri.parse(uri);
            mIsDownloadDirectoryOverrideURISystem = mPreferences.getBoolean(
                    PREF_DCC_DIRECTORY_OVERRIDE_URI_SYSTEM, false);
        }
        checkSystemDownloadsDirectoryAccess();
    }

    public void setAlwaysUseApplicationDownloadDirectory(boolean value) {
        mAlwaysUseFallbackDir = value;
        mPreferences.edit()
                .putBoolean(PREF_DCC_ALWAYS_USE_APP_DOWNLOAD_DIR, value)
                .apply();
        checkSystemDownloadsDirectoryAccess();
    }

    public void setOverrideDownloadDirectory(Uri uri, boolean isSystem) {
        mDownloadDirectoryOverrideURI = uri;
        mIsDownloadDirectoryOverrideURISystem = isSystem;
        mPreferences.edit()
                .putString(PREF_DCC_DIRECTORY_OVERRIDE_URI, uri.toString())
                .putBoolean(PREF_DCC_DIRECTORY_OVERRIDE_URI_SYSTEM, isSystem)
                .apply();
    }

    public Uri getDownloadDirectoryOverrideURI() {
        if (mAlwaysUseFallbackDir)
            return null;
        return mDownloadDirectoryOverrideURI;
    }

    public boolean isDownloadDirectoryOverrideURISystem() {
        return mIsDownloadDirectoryOverrideURISystem;
    }

    public boolean isSystemDownloadDirectoryUsed() {
        return mHasSystemDirectoryAccess && (mDownloadDirectoryOverrideURI == null ||
                mIsDownloadDirectoryOverrideURISystem);
    }

    private void checkSystemDownloadsDirectoryAccess() {
        if (mDownloadDirectoryOverrideURI != null && !mAlwaysUseFallbackDir) {
            DocumentFile dir = DocumentFile.fromTreeUri(mContext,
                    mDownloadDirectoryOverrideURI);
            mHasSystemDirectoryAccess = dir.exists() && dir.canWrite();
            return;
        }

        File downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir != null && downloadsDir.canWrite() && !mAlwaysUseFallbackDir) {
            mDownloadDirectory = downloadsDir;
            mHasSystemDirectoryAccess = true;
        } else {
            mDownloadDirectory = mFallbackDownloadDirectory;
            mHasSystemDirectoryAccess = false;
        }
        Log.d("DCCManager", "Download directory: " +
                (mDownloadDirectory != null ? mDownloadDirectory.getAbsolutePath() : "null"));
    }

    public boolean needsAskSystemDownloadsPermission() {
        if (!mHasSystemDirectoryAccess)
            checkSystemDownloadsDirectoryAccess();
        return !mHasSystemDirectoryAccess &&
                !mPreferences.getBoolean(PREF_DCC_ASKED_FOR_PERMISSION, false) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public DCCNotificationManager getNotificationManager() {
        return mNotificationManager;
    }

    public DCCServerManager getServer() {
        return mServer;
    }

    public DCCHistory getHistory() {
        return mHistory;
    }

    public DCCClientManager createClient(ServerConnectionInfo server) {
        return new ClientImpl(server);
    }

    public void addDownloadListener(DownloadListener listener) {
        synchronized (mDownloads) {
            mDownloadListeners.add(listener);
        }
    }

    public void removeDownloadListener(DownloadListener listener) {
        synchronized (mDownloads) {
            mDownloadListeners.remove(listener);
        }
    }

    @Override
    public void onUploadCreated(DCCServerManager.UploadEntry uploadEntry) {
        synchronized (mUploads) {
            mUploads.put(uploadEntry.getServer(), uploadEntry);

            ServerConnectionData connection = uploadEntry.getConnection();
            ServerConnectionInfo connectionInfo = null;
            for (ServerConnectionInfo info : ServerConnectionManager.getInstance(mContext)
                    .getConnections()) {
                if (((ServerConnectionApi) info.getApiInstance()).getServerConnectionData()
                        == connection) {
                    connectionInfo = info;
                    break;
                }
            }
            mUploadServers.put(uploadEntry, connectionInfo != null
                    ? new UploadServerInfo(connectionInfo) : null);
        }
    }

    @Override
    public void onUploadDestroyed(DCCServerManager.UploadEntry uploadEntry) {
        synchronized (mUploads) {
            mUploads.remove(uploadEntry.getServer());
            mUploadServers.remove(uploadEntry);

            PortMapper.PortMappingResult mapping;
            if ((mapping = mUploadPortMappings.remove(uploadEntry)) != null) {
                if (Thread.currentThread() == Looper.getMainLooper().getThread())
                    AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> deleteUploadPortMapping(mapping));
                else
                    deleteUploadPortMapping(mapping);
            }
        }
    }

    private void deleteUploadPortMapping(PortMapper.PortMappingResult mapping) {
        try {
            PortMapper.removePortMapping(mapping);
        } catch (Exception e) {
            Log.w("DCCManager", "Failed to remove port mapping for port " +
                    mapping.getExternalPort());
            e.printStackTrace();
        }
    }

    @Override
    public void onSessionCreated(DCCServer dccServer, DCCServer.UploadSession uploadSession) {
        synchronized (mSessions) {
            mSessions.add(uploadSession);
        }
    }

    @Override
    public void onSessionDestroyed(DCCServer dccServer, DCCServer.UploadSession uploadSession) {
        DCCServerManager.UploadEntry entry;
        UploadServerInfo uploadServerInfo;
        boolean shouldClose;
        synchronized (mSessions) {
            mSessions.remove(uploadSession);
            shouldClose = uploadSession.getAcknowledgedSize() >= uploadSession.getTotalSize();
            if (shouldClose) {
                for (DCCServer.UploadSession s : mSessions) {
                    if (s.getServer() == dccServer) {
                        shouldClose = false;
                        break;
                    }
                }
            }
        }
        synchronized (mUploads) {
            entry = mUploads.get(dccServer);
            uploadServerInfo = mUploadServers.get(entry);
        }
        if (shouldClose && entry != null)
            mHandler.post(() -> mServer.cancelUpload(entry));
        if (entry != null && uploadServerInfo != null)
            mHistory.addEntry(new DCCHistory.Entry(entry, uploadSession, uploadServerInfo,
                    new Date()));
    }

    public void onDownloadCreated(DownloadInfo download) {
        synchronized (mDownloads) {
            mDownloads.add(download);
            for (DownloadListener listener : mDownloadListeners)
                listener.onDownloadCreated(download);
        }
    }

    public void onDownloadDestroyed(DownloadInfo download) {
        synchronized (mDownloads) {
            mDownloads.remove(download);
            for (DownloadListener listener : mDownloadListeners)
                listener.onDownloadDestroyed(download);
        }
        mHistory.addEntry(new DCCHistory.Entry(download, new Date()));
    }

    @Override
    public void onClosed(DCCClient dccClient) {
        DownloadInfo download = null;
        synchronized (mDownloads) {
            for (int i = mDownloads.size() - 1; i >= 0; --i) {
                download = mDownloads.get(i);
                if (download.getClient() == dccClient) {
                    mDownloads.remove(i);
                    for (DownloadListener listener : mDownloadListeners)
                        listener.onDownloadDestroyed(download);
                    break;
                }
            }
        }
        if (download != null)
            mHistory.addEntry(new DCCHistory.Entry(download, new Date()));
    }

    @Override
    public void onClosed(DCCReverseClient dccReverseClient) {
        DownloadInfo download = null;
        synchronized (mDownloads) {
            for (int i = mDownloads.size() - 1; i >= 0; --i) {
                download = mDownloads.get(i);
                if (download.getReverseClient() == dccReverseClient) {
                    mDownloads.remove(i);
                    for (DownloadListener listener : mDownloadListeners)
                        listener.onDownloadDestroyed(download);
                    break;
                }
            }
        }
        if (download != null)
            mHistory.addEntry(new DCCHistory.Entry(download, new Date()));
    }

    @Override
    public void onClientConnected(DCCReverseClient dccReverseClient, DCCClient dccClient) {
        synchronized (mDownloads) {
            for (DownloadInfo download : mDownloads) {
                if (download.getReverseClient() == dccReverseClient) {
                    for (DownloadListener listener : mDownloadListeners)
                        listener.onDownloadUpdated(download);
                    return;
                }
            }
        }
    }

    public DCCServerManager.UploadEntry getUploadEntry(DCCServer server) {
        synchronized (mUploads) {
            return mUploads.get(server);
        }
    }

    public UploadServerInfo getUploadServerInfo(DCCServerManager.UploadEntry upload) {
        synchronized (mUploads) {
            return mUploadServers.get(upload);
        }
    }

    public String getUploadName(DCCServer server) {
        synchronized (mUploads) {
            DCCServerManager.UploadEntry ent = mUploads.get(server);
            if (ent == null)
                return null;
            return ent.getFileName();
        }
    }

    public List<DCCServerManager.UploadEntry> getUploads() {
        synchronized (mUploads) {
            return new ArrayList<>(mUploads.values());
        }
    }

    public List<DCCServer.UploadSession> getUploadSessions() {
        synchronized (mSessions) {
            return new ArrayList<>(mSessions);
        }
    }

    public boolean hasAnyDownloads() {
        synchronized (mSessions) {
            return !mDownloads.isEmpty();
        }
    }

    public boolean hasAnyActiveDownloads() {
        synchronized (mSessions) {
            for (DownloadInfo download : mDownloads) {
                if (!download.isPending())
                    return true;
            }
            return false;
        }
    }

    public List<DownloadInfo> getDownloads() {
        synchronized (mDownloads) {
            return new ArrayList<>(mDownloads);
        }
    }

    public void startUpload(ServerConnectionInfo server, String channel,
                            DCCServer.FileChannelFactory file, String fileName, long fileSize) {
        ServerConnectionData connectionData = ((ServerConnectionApi) server.getApiInstance())
                .getServerConnectionData();

        if (ServerConnectionManager.isWifiConnected(mContext)) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                DCCServerManager.UploadEntry upload = null;
                PortMapper.PortMappingResult mapping = null;
                try {
                    upload = mServer.startUpload(connectionData, channel, fileName, file);
                    mapping = PortMapper.mapPort(new PortMapper.PortMappingRequest(
                            AddPortMappingCall.PROTOCOL_TCP, upload.getPort(),
                            upload.getPort(), "Revolution IRC DCC transfer"));
                    synchronized (mUploads) {
                        if (!mUploads.containsKey(upload.getServer()))
                            throw new IOException("Upload cancelled while we were setting up" +
                                    " port mapping");
                        mUploadPortMappings.put(upload, mapping);
                    }
                    mServer.setUploadPortForwarded(upload, mapping.getExternalPort());
                    server.getApiInstance().sendMessage(channel, DCCUtils.buildSendMessage(
                            mapping.getExternalIP(), fileName, mapping.getExternalPort(), fileSize),
                            null, null);
                } catch (IOException e) {
                    e.printStackTrace();

                    mHandler.post(() -> Toast
                            .makeText(mContext, R.string.error_generic, Toast.LENGTH_SHORT).show());
                    if (upload != null)
                        mServer.cancelUpload(upload);
                    if (mapping != null) {
                        try {
                            PortMapper.removePortMapping(mapping);
                        } catch (Exception e2) {
                            Log.w("DCCManager", "Failed to remove port mapping " +
                                    "in error handler");
                            e2.printStackTrace();
                        }
                    }

                    // fall back to reverse DCC
                    upload = mServer.addReverseUpload(connectionData, channel, fileName, file);
                    server.getApiInstance().sendMessage(channel, DCCUtils.buildSendMessage(
                            "127.0.0.1", fileName, 0, fileSize, upload.getReverseId()),
                            null, null);
                }
            });
        } else {
            // fall back to reverse DCC
            DCCServerManager.UploadEntry upload = mServer.addReverseUpload(
                    connectionData, channel, fileName, file);
            server.getApiInstance().sendMessage(channel, DCCUtils.buildSendMessage(
                    "0.0.0.0", fileName, 0, fileSize,
                    upload.getReverseId()),
                    null, null);
        }
    }


    public class UploadServerInfo {

        private final UUID mServerUUID;
        private final String mServerName;

        public UploadServerInfo(UUID uuid, String serverName) {
            this.mServerUUID = uuid;
            this.mServerName = serverName;
        }
        public UploadServerInfo(ServerConnectionInfo connectionInfo) {
            this.mServerUUID = connectionInfo.getUUID();
            this.mServerName = connectionInfo.getName();
        }

        public UUID getServerUUID() {
            return mServerUUID;
        }

        public String getServerName() {
            return mServerName;
        }

    }

    public class DownloadInfo {

        private final UUID mServerUUID;
        private final String mServerName;
        private final MessagePrefix mSender;
        private final String mFileName;
        private final long mFileSize;
        private final String mAddress;
        private final int mPort;
        private final int mReverseUploadId;
        private boolean mPending = true;
        private boolean mCancelled = false;
        private DCCClient mClient;
        private DCCReverseClient mReverseClient;
        private Uri mDownloadedTo;

        private DownloadInfo(ServerConnectionInfo server, MessagePrefix sender, String fileName,
                             long fileSize, String address, int port) {
            mServerUUID = server.getUUID();
            mServerName = server.getName();
            mSender = sender;
            mFileName = fileName;
            mFileSize = fileSize;
            mAddress = address;
            mPort = port;
            mReverseUploadId = -1;
        }
        private DownloadInfo(ServerConnectionInfo server, MessagePrefix sender, String fileName,
                             long fileSize, int reverseUploadId) {
            mServerUUID = server.getUUID();
            mServerName = server.getName();
            mSender = sender;
            mFileName = fileName;
            mFileSize = fileSize;
            mAddress = null;
            mPort = -1;
            mReverseUploadId = reverseUploadId;
        }

        public String getServerName() {
            return mServerName;
        }

        public UUID getServerUUID() {
            return mServerUUID;
        }

        public MessagePrefix getSender() {
            return mSender;
        }

        public String getRawFileName() {
            return mFileName;
        }

        public String getUnescapedFileName() {
            return DCCUtils.unescapeFilename(mFileName);
        }

        private String getFileExtension() {
            String fileName = getUnescapedFileName();
            int iof = fileName.lastIndexOf('.');
            if (iof == -1)
                return null;
            return fileName.substring(iof + 1);
        }

        public long getFileSize() {
            return mFileSize;
        }

        public synchronized Uri getDownloadedTo() {
            return mDownloadedTo;
        }

        public boolean isPending() {
            return mPending;
        }

        public boolean isReverse() {
            return mReverseUploadId != -1;
        }

        public synchronized DCCClient getClient() {
            if (mReverseClient != null)
                return mReverseClient.getClient();
            return mClient;
        }

        public synchronized DCCReverseClient getReverseClient() {
            return mReverseClient;
        }

        private void createClient() throws IOException {
            ServerConnectionInfo connection = ServerConnectionManager.getInstance(mContext)
                    .getConnection(mServerUUID);
            if (connection == null)
                throw new IOException("The connection doesn't exist");
            FileChannel file;
            String downloadFileName = getUnescapedFileName().replace('/', '_');
            String ext = getFileExtension();
            if (mDownloadDirectoryOverrideURI != null && !mAlwaysUseFallbackDir) {
                DocumentFile dir = DocumentFile.fromTreeUri(mContext,
                        mDownloadDirectoryOverrideURI);
                String mime = ext != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                        : null;
                if (mime == null)
                    mime = "application/octet-stream";
                DocumentFile docFile = dir.createFile(mime, downloadFileName);
                OutputStream stream = mContext.getContentResolver().openOutputStream(
                        docFile.getUri());
                if (!(stream instanceof FileOutputStream))
                    throw new IOException("stream is not a file");
                file = ((FileOutputStream) stream).getChannel();
                synchronized (this) {
                    mDownloadedTo = docFile.getUri();
                }
                Log.d("DCCManager", "Starting a download: " + docFile.getUri().toString());
            } else {
                if (mDownloadDirectory == null)
                    throw new IOException("Download directory is null");
                File filePath = new File(mDownloadDirectory, downloadFileName);
                int attempt = 1;
                while (filePath.exists()) {
                    filePath = new File(mDownloadDirectory, (ext != null
                            ? downloadFileName.substring(0,
                            downloadFileName.length() - ext.length() - 1)
                            : downloadFileName) + " (" + attempt + ")" +
                            (ext != null ? "." + ext : ""));
                    attempt++;
                }
                file = new FileOutputStream(filePath).getChannel();
                synchronized (this) {
                    mDownloadedTo = Uri.fromFile(filePath);
                }
                Log.d("DCCManager", "Starting a download: " + filePath.getAbsolutePath());
            }
            try {
                if (isReverse()) {
                    synchronized (this) {
                        if (mCancelled)
                            throw new CancelledException();
                        mReverseClient = new DCCReverseClient(file, 0L, mFileSize);
                    }
                    mReverseClient.setStateListener(DCCManager.this);
                    int port = mReverseClient.createServerSocket();
                    String message = DCCUtils.buildSendMessage(getLocalIP(), mFileName, port,
                            mFileSize, mReverseUploadId);
                    connection.getApiInstance().sendMessage(mSender.getNick(), message, null, null);
                } else {
                    SocketChannel socket = SocketChannel.open(
                            new InetSocketAddress(mAddress, mPort));
                    synchronized (this) {
                        if (mCancelled)
                            throw new CancelledException();
                        mClient = new DCCClient(file, 0L, mFileSize);
                    }
                    mClient.setCloseListener(DCCManager.this);
                    mClient.start(socket);
                }
            } catch (Exception e) {
                try {
                    file.close();
                } catch (IOException ignored) {
                }
                synchronized (this) {
                    if (mClient != null)
                        mClient.close();
                    mClient = null;
                    if (mReverseClient != null)
                        mReverseClient.close();
                    mReverseClient = null;
                }
                throw e;
            }
        }

        public void approve() {
            if (!mPending || mCancelled)
                return;
            mPending = false;
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                try {
                    createClient();
                } catch (CancelledException e) {
                    onDownloadDestroyed(this);
                    return;
                } catch (IOException e) {
                    mHandler.post(() ->
                            Toast.makeText(mContext, R.string.error_generic, Toast.LENGTH_SHORT)
                                    .show());
                    e.printStackTrace();
                    onDownloadDestroyed(this);
                    return;
                }

                synchronized (mDownloads) {
                    for (DownloadListener listener : mDownloadListeners)
                        listener.onDownloadUpdated(this);
                }
            });
        }

        public void reject() {
            if (!mPending || mCancelled)
                return;
            onDownloadDestroyed(this);
        }

        public void cancel() {
            if (mPending) {
                onDownloadDestroyed(this);
            } else {
                synchronized (this) {
                    mCancelled = true;
                    if (mClient != null) {
                        mClient.close();
                        mClient = null;
                    }
                    if (mReverseClient != null) {
                        mReverseClient.close();
                        mReverseClient = null;
                    }
                }
            }
        }

        public AlertDialog createDownloadApprovalDialog(Context context,
                                                        ActivityDialogHandler handler) {
            String title;
            if (getFileSize() > 0)
                title = context.getString(R.string.dcc_approve_download_title_with_size,
                        getUnescapedFileName(), FormatUtils.formatByteSize(getFileSize()));
            else
                title = context.getString(R.string.dcc_approve_download_title,
                        getUnescapedFileName());
            AlertDialog ret = new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(context.getString(R.string.dcc_approve_download_body,
                            mSender.toString(), getServerName()))
                    .setPositiveButton(R.string.action_accept,
                            (DialogInterface dialog, int which) -> {
                                if (needsAskSystemDownloadsPermission())
                                    handler.askSystemDownloadsPermission(() -> approve());
                                else
                                    approve();
                            })
                    .setNegativeButton(R.string.action_reject,
                            (DialogInterface dialog, int which) -> reject())
                    .setOnCancelListener((DialogInterface dialog) -> reject())
                    .create();
            ret.setCanceledOnTouchOutside(false);
            return ret;
        }

    }

    private static class CancelledException extends IOException {
        public CancelledException() {
            super();
        }
    }

    public static class ActivityDialogHandler implements DownloadListener {

        private Activity mActivity;
        private AlertDialog mCurrentDialog;
        private int mStoragePermissionRequestCode;
        private int mDownloadsPermissionRequestCode;
        private List<Runnable> mStoragePermissionRequestCallbacks;
        private boolean mPermissionRequestPending;

        public ActivityDialogHandler(Activity activity, int storagePermissionRequestCode,
                                     int downloadsPermissionRequestCode) {
            mActivity = activity;
            mStoragePermissionRequestCode = storagePermissionRequestCode;
            mDownloadsPermissionRequestCode = downloadsPermissionRequestCode;
        }

        public void onResume() {
            DCCManager.getInstance(mActivity).addDownloadListener(this);
            showDialogsIfNeeded();
        }

        public void onPause() {
            DCCManager.getInstance(mActivity).removeDownloadListener(this);
            if (mCurrentDialog != null) {
                mCurrentDialog.dismiss();
                mCurrentDialog = null;
            }
        }

        private void showDialog(AlertDialog dialog) {
            if (mCurrentDialog != null)
                mCurrentDialog.dismiss();
            mCurrentDialog = dialog;
            dialog.setOnDismissListener((DialogInterface i) -> {
                mCurrentDialog = null;
                showDialogsIfNeeded();
            });
            dialog.show();
        }

        private void showDialogsIfNeeded() {
            if (mCurrentDialog != null || mPermissionRequestPending)
                return;
            for (DownloadInfo download : DCCManager.getInstance(mActivity).getDownloads()) {
                if (download.isPending())
                    showDialog(download.createDownloadApprovalDialog(mActivity, this));
            }
        }

        @Override
        public void onDownloadCreated(DownloadInfo download) {
            if (download.isPending()) {
                mActivity.runOnUiThread(() -> {
                    if (mCurrentDialog == null && download.isPending()) // download is still pending
                        showDialog(download.createDownloadApprovalDialog(mActivity, this));
                });
            }
        }

        @Override
        public void onDownloadDestroyed(DownloadInfo download) {
        }

        @Override
        public void onDownloadUpdated(DownloadInfo download) {
        }


        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == mDownloadsPermissionRequestCode &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (resultCode == Activity.RESULT_OK) {
                    mActivity.getContentResolver().takePersistableUriPermission(data.getData(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    DCCManager.getInstance(mActivity).setOverrideDownloadDirectory(
                            data.getData(), true);
                    onSystemDownloadPermissionRequestFinished();
                } else {
                    showSystemDownloadsPermissionDenialDialog();
                }
            }
        }

        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (requestCode == mStoragePermissionRequestCode) {
                String p = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                if (ContextCompat.checkSelfPermission(mActivity, p) !=
                        PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.shouldShowRequestPermissionRationale(mActivity, p)) {
                    showSystemDownloadsPermissionDenialDialog();
                } else {
                    onSystemDownloadPermissionRequestFinished();
                }
            }
        }

        private void askSystemDownloadsPermission(Runnable cb, boolean noShowDenialDialog) {
            if (cb != null) {
                if (mStoragePermissionRequestCallbacks == null)
                    mStoragePermissionRequestCallbacks = new ArrayList<>();
                mStoragePermissionRequestCallbacks.add(cb);
            }
            mPermissionRequestPending = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                StorageManager manager = (StorageManager) mActivity
                        .getSystemService(Context.STORAGE_SERVICE);
                StorageVolume volume = manager.getPrimaryStorageVolume();
                Intent intent = volume.createAccessIntent(Environment.DIRECTORY_DOWNLOADS);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                mActivity.startActivityForResult(intent, mDownloadsPermissionRequestCode);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) && !noShowDenialDialog) {
                    showSystemDownloadsPermissionDenialDialog();
                } else {
                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            mStoragePermissionRequestCode);
                }
            }
        }

        public void askSystemDownloadsPermission(Runnable cb) {
            askSystemDownloadsPermission(cb, false);
        }

        private void onSystemDownloadPermissionRequestFinished() {
            if (mStoragePermissionRequestCallbacks != null) {
                DCCManager.getInstance(mActivity).checkSystemDownloadsDirectoryAccess();
                for (Runnable r : mStoragePermissionRequestCallbacks)
                    r.run();
            }
        }

        private void showSystemDownloadsPermissionDenialDialog() {
            new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.dcc_system_downloads_permission_dialog_title)
                    .setMessage(R.string.dcc_system_downloads_permission_dialog_text)
                    .setPositiveButton(R.string.action_ok, (DialogInterface i, int w) -> {
                        DCCManager.getInstance(mActivity).mPreferences.edit()
                                .putBoolean(PREF_DCC_ASKED_FOR_PERMISSION, true)
                                .apply();
                        onSystemDownloadPermissionRequestFinished();
                    })
                    .setNegativeButton(R.string.action_ask_again, (DialogInterface i, int w) -> {
                        askSystemDownloadsPermission(null, true);
                    })
                    .show();
        }

    }

    private class ClientImpl extends DCCClientManager {

        private ServerConnectionInfo mServer;

        public ClientImpl(ServerConnectionInfo server) {
            mServer = server;
        }

        @Override
        public void onFileOffered(ServerConnectionData connection, MessagePrefix sender,
                                  String fileName, String address, int port, long fileSize) {
            Log.d("DCCManager", "File offered: " + fileName +
                    " from " + address + ":" + port);
            onDownloadCreated(new DownloadInfo(mServer, sender, fileName, fileSize, address, port));
        }

        @Override
        public void onFileOfferedUsingReverse(ServerConnectionData connection, MessagePrefix sender,
                                              String fileName, long fileSize, int uploadId) {
            Log.d("DCCManager", "File offered: " + fileName + " (reverse)");
            onDownloadCreated(new DownloadInfo(mServer, sender, fileName, fileSize, uploadId));
        }
    }

    public interface DownloadListener {

        void onDownloadCreated(DownloadInfo download);

        void onDownloadDestroyed(DownloadInfo download);

        void onDownloadUpdated(DownloadInfo download);

    }



    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress())
                        continue;
                    String hostAddr = addr.getHostAddress();
                    if (hostAddr.indexOf(':') != -1) { // IPv6
                        continue;
                        /*
                        int iof = hostAddr.indexOf('%');
                        if (iof != -1)
                            hostAddr = hostAddr.substring(0, iof);
                        */
                    }
                    return hostAddr;
                }
            }
        } catch (SocketException ignored) {
        }
        return null;
    }


}
