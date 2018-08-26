package io.mrarm.irc;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.mrarm.chatlib.irc.MessagePrefix;
import io.mrarm.chatlib.irc.ServerConnectionData;
import io.mrarm.chatlib.irc.dcc.DCCClient;
import io.mrarm.chatlib.irc.dcc.DCCClientManager;
import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;
import io.mrarm.irc.util.FormatUtils;

public class DCCManager implements DCCServerManager.UploadListener, DCCClient.CloseListener {

    private static DCCManager sInstance;

    public static DCCManager getInstance(Context context) {
        if (sInstance == null)
            sInstance = new DCCManager(context.getApplicationContext());
        return sInstance;
    }

    private final Context mContext;
    private final DCCServerManager mServer;
    private final Map<DCCServer, DCCServerManager.UploadEntry> mUploads = new HashMap();
    private final List<DCCServer.UploadSession> mSessions = new ArrayList<>();
    private final List<DownloadInfo> mDownloads = new ArrayList<>();
    private final List<DownloadListener> mDownloadListeners = new ArrayList<>();
    private File mDownloadDirectory;

    public DCCManager(Context context) {
        mContext = context;
        mDownloadDirectory = mContext.getExternalFilesDir("Downloads");
        mDownloadDirectory.mkdirs();
        mServer = new DCCServerManager();
        mServer.addUploadListener(this);
    }

    public DCCServerManager getServer() {
        return mServer;
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
        }
    }

    @Override
    public void onUploadDestroyed(DCCServerManager.UploadEntry uploadEntry) {
        synchronized (mUploads) {
            mUploads.remove(uploadEntry.getServer());
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
        synchronized (mSessions) {
            mSessions.remove(uploadSession);
        }
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
    }

    @Override
    public void onClosed(DCCClient dccClient) {
        synchronized (mDownloads) {
            for (int i = mDownloads.size() - 1; i >= 0; --i) {
                DownloadInfo download = mDownloads.get(i);
                if (download.getClient() == dccClient) {
                    mDownloads.remove(i);
                    for (DownloadListener listener : mDownloadListeners)
                        listener.onDownloadDestroyed(download);
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

    public List<DownloadInfo> getDownloads() {
        synchronized (mSessions) {
            return new ArrayList<>(mDownloads);
        }
    }

    public Object getSessionsSyncObject() {
        return mSessions;
    }

    public class DownloadInfo {

        private final UUID mServerUUID;
        private final String mServerName;
        private final MessagePrefix mSender;
        private final String mFileName;
        private final long mFileSize;
        private final String mAddress;
        private final int mPort;
        private boolean mPending = true;
        private DCCClient mClient;

        private DownloadInfo(ServerConnectionInfo server, MessagePrefix sender, String fileName,
                             long fileSize, String address, int port) {
            mServerUUID = server.getUUID();
            mServerName = server.getName();
            mSender = sender;
            mFileName = fileName;
            mFileSize = fileSize;
            mAddress = address;
            mPort = port;
        }

        public String getServerName() {
            return mServerName;
        }

        public MessagePrefix getSender() {
            return mSender;
        }

        public String getFileName() {
            return mFileName;
        }

        public long getFileSize() {
            return mFileSize;
        }

        public boolean isPending() {
            return mPending;
        }

        public synchronized DCCClient getClient() {
            return mClient;
        }

        public void approve() {
            if (!mPending)
                return;
            mPending = false;
            AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                try {
                    SocketChannel socket = SocketChannel.open(
                            new InetSocketAddress(mAddress, mPort));
                    FileChannel file = new FileOutputStream(new File(mDownloadDirectory,
                            mFileName.replace('/', '_'))).getChannel();
                    synchronized (this) {
                        mClient = new DCCClient(file, 0L, mFileSize);
                    }
                    mClient.setCloseListener(DCCManager.this);
                    mClient.start(socket);
                } catch (IOException e) {
                    Toast.makeText(mContext, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });
        }

        public void reject() {
            if (!mPending)
                return;
            onDownloadDestroyed(this);
        }

        public AlertDialog createDownloadApprovalDialog(Context context) {
            String title;
            if (getFileSize() > 0)
                title = context.getString(R.string.dcc_approve_download_title_with_size,
                        getFileName(), FormatUtils.formatByteSize(getFileSize()));
            else
                title = context.getString(R.string.dcc_approve_download_title, getFileName());
            return new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(context.getString(R.string.dcc_approve_download_body,
                            mSender.toString(), getServerName()))
                    .setPositiveButton(R.string.action_accept,
                            (DialogInterface dialog, int which) -> approve())
                    .setNegativeButton(R.string.action_reject,
                            (DialogInterface dialog, int which) -> reject())
                    .setOnCancelListener((DialogInterface dialog) -> reject())
                    .create();
        }

    }

    public static class ActivityDialogHandler implements DownloadListener {

        private Activity mActivity;
        private AlertDialog mCurrentDialog;

        public ActivityDialogHandler(Activity activity) {
            mActivity = activity;
        }

        public void onResume() {
            DCCManager.getInstance(mActivity).addDownloadListener(this);
            showDialogsIfNeeded();
        }

        public void onPause() {
            DCCManager.getInstance(mActivity).removeDownloadListener(this);
            if (mCurrentDialog != null) {
                mCurrentDialog.cancel();
                mCurrentDialog = null;
            }
        }

        private void showDialog(AlertDialog dialog) {
            if (mCurrentDialog != null)
                mCurrentDialog.cancel();
            mCurrentDialog = dialog;
            dialog.setOnDismissListener((DialogInterface i) -> {
                mCurrentDialog = null;
                showDialogsIfNeeded();
            });
            dialog.show();
        }

        private void showDialogsIfNeeded() {
            for (DownloadInfo download : DCCManager.getInstance(mActivity).getDownloads()) {
                if (download.isPending())
                    showDialog(download.createDownloadApprovalDialog(mActivity));
            }
        }

        @Override
        public void onDownloadCreated(DownloadInfo download) {
            if (download.isPending()) {
                mActivity.runOnUiThread(() -> {
                    if (mCurrentDialog == null && download.isPending()) // download is still pending
                        showDialog(download.createDownloadApprovalDialog(mActivity));
                });
            }
        }

        @Override
        public void onDownloadDestroyed(DownloadInfo download) {
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

    }

    public interface DownloadListener {

        void onDownloadCreated(DownloadInfo download);

        void onDownloadDestroyed(DownloadInfo download);

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
