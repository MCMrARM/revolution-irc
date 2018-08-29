package io.mrarm.irc.dcc;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URL;

import javax.xml.transform.TransformerException;

import io.mrarm.irc.dcc.rpc.AddPortMappingCall;

public class PortMapper {

    public static void mapPort(int port, String description) throws IOException {
        SSDPDiscovery discovery = new SSDPDiscovery();
        discovery.bind();
        discovery.setReceiveTimeout(1);
        discovery.sendSearch(UPnPTypes.UPNP_INTERNET_GATEWAY_DEVICE_V1, 1);
        SSDPDiscovery.Response response;
        while ((response = discovery.receive()) != null) {
            Log.d("PortMapper", "Found device using SSDP: " +
                    response.getDescriptionLocation());
            UPnPDeviceDescription.Service service = getWANService(response);
            if (service == null) {
                Log.d("PortMapper", "Skipping device");
                continue;
            }
            Log.d("DCCManager", "Found a valid service: " + service.getControlURL());

            URL serviceURL = new URL(new URL(response.getDescriptionLocation()),
                    service.getControlURL());

            try {
                AddPortMappingCall call = new AddPortMappingCall(service.getServiceType());
                call.setNewInternalPort(port);
                call.setNewInternalClient(resolveLocalIP(serviceURL));
                call.setNewExternalPort(port);
                call.setNewProtocol(AddPortMappingCall.PROTOCOL_TCP);
                call.setNewPortMappingDescription(description);
                call.send(serviceURL);
            } catch (IOException | TransformerException e) {
                Log.w("PortMapper", "Failed to send AddPortMapping request");
                e.printStackTrace();
            }
        }
        discovery.close();
    }

    private static UPnPDeviceDescription.Service getWANService(
            SSDPDiscovery.Response ssdpResponese) {
        UPnPDeviceDescription description = new UPnPDeviceDescription();
        try {
            description.loadFromUrl(ssdpResponese.getDescriptionLocation());
        } catch (Exception ex) {
            Log.w("PortMapper", "Failed to fetch description");
            ex.printStackTrace();
            return null;
        }
        UPnPDeviceDescription wanDevice = description
                .findDeviceByType(UPnPTypes.UPNP_WAN_CONNECTION_DEVICE_V1);
        if (wanDevice == null) {
            Log.d("PortMapper", "Root device has no WAN connection device");
            return null;
        }
        UPnPDeviceDescription.Service serviceIP =
                wanDevice.getServiceByType(UPnPTypes.UPNP_WAN_IP_CONNECTION_V1);
        UPnPDeviceDescription.Service servicePPP =
                wanDevice.getServiceByType(UPnPTypes.UPNP_WAN_PPP_CONNECTION_V1);
        if (serviceIP == null && servicePPP == null) {
            Log.d("PortMapper", "Device has no WAN IP or PPP connection service");
            return null;
        }
        return (servicePPP != null ? servicePPP : serviceIP);
    }

    private static String resolveLocalIP(URL targetUrl) throws IOException {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.connect(new InetSocketAddress(targetUrl.getHost(), targetUrl.getPort()));
            return socket.getLocalAddress().getHostAddress();
        } finally {
            if (socket != null)
                socket.close();
        }
    }

}
