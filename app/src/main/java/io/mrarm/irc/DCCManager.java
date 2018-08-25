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
import java.util.Enumeration;

import io.mrarm.chatlib.irc.MessagePrefix;
import io.mrarm.chatlib.irc.ServerConnectionData;
import io.mrarm.chatlib.irc.dcc.DCCClient;
import io.mrarm.chatlib.irc.dcc.DCCClientManager;

public class DCCManager {

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


    public static class Client extends DCCClientManager {

        private Context context;

        public Client(Context context) {
            this.context = context;
        }

        @Override
        public void onFileOffered(ServerConnectionData connection, MessagePrefix sender,
                                  String filename, String address, int port, long fileSize) {
            try {
                Log.d("DCCManager", "File offered: " + filename + " from " + address + ":" + port);
                SocketChannel socket = SocketChannel.open(new InetSocketAddress(address, port));
                File dstDir = context.getExternalFilesDir("Downloads");
                dstDir.mkdirs();
                FileChannel file = new FileOutputStream(new File(dstDir,
                        filename.replace('/', '_'))).getChannel();
                new DCCClient(socket, file, 0L, fileSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
