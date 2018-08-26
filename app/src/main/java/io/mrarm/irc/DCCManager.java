package io.mrarm.irc;

import android.content.Context;
import android.util.Log;

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

import io.mrarm.chatlib.irc.MessagePrefix;
import io.mrarm.chatlib.irc.ServerConnectionData;
import io.mrarm.chatlib.irc.dcc.DCCClient;
import io.mrarm.chatlib.irc.dcc.DCCClientManager;
import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;

public class DCCManager implements DCCServerManager.UploadListener {

    private static DCCManager sInstance;

    public static DCCManager getInstance(Context context) {
        if (sInstance == null)
            sInstance = new DCCManager(context.getApplicationContext());
        return sInstance;
    }

    private final Context mContext;
    private final DCCServerManager mServer;
    private final DCCClientManager mClient;
    private final Map<DCCServer, DCCServerManager.UploadEntry> mUploads = new HashMap();
    private final List<DCCServer.UploadSession> mSessions = new ArrayList<>();

    public DCCManager(Context context) {
        mContext = context;
        mServer = new DCCServerManager();
        mClient = new ClientImpl();
        mServer.addUploadListener(this);
    }

    public DCCServerManager getServer() {
        return mServer;
    }

    public DCCClientManager getClient() {
        return mClient;
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

    public Object getSessionsSyncObject() {
        return mSessions;
    }

    private class ClientImpl extends DCCClientManager {

        @Override
        public void onFileOffered(ServerConnectionData connection, MessagePrefix sender,
                                  String filename, String address, int port, long fileSize) {
            try {
                Log.d("DCCManager", "File offered: " + filename + " from " + address + ":" + port);
                SocketChannel socket = SocketChannel.open(new InetSocketAddress(address, port));
                File dstDir = mContext.getExternalFilesDir("Downloads");
                dstDir.mkdirs();
                FileChannel file = new FileOutputStream(new File(dstDir,
                        filename.replace('/', '_'))).getChannel();
                new DCCClient(socket, file, 0L, fileSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
