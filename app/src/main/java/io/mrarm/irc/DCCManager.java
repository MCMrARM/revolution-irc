package io.mrarm.irc;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

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

    public static String convertIPForDCC(String ip) {
        int idx = 0;
        long ret = 0;
        for (int i = 0; i < 4; i++) {
            int idx2 = ip.indexOf('.', idx);
            if (i == 3 ? (idx2 != -1) : (idx2 == -1))
                throw new IllegalArgumentException("Invalid IPv4 address");
            ret = ret * 256 + Integer.parseInt(i == 3 ? ip.substring(idx) : ip.substring(idx, idx2));
            idx = idx2 + 1;
        }
        return Long.toString(ret);
    }

    public static String buildSendMessage(String name, int port, long size) {
        StringBuilder sendCmd = new StringBuilder();
        sendCmd.append('\001');
        sendCmd.append("DCC SEND ");
        sendCmd.append(name);
        sendCmd.append(' ');
        sendCmd.append(convertIPForDCC(DCCManager.getLocalIP()));
        sendCmd.append(' ');
        sendCmd.append(port);
        sendCmd.append(' ');
        sendCmd.append(size);
        sendCmd.append('\001');
        return sendCmd.toString();
    }

}
