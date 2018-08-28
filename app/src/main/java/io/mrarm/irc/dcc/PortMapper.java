package io.mrarm.irc.dcc;

import android.util.Log;

import java.io.IOException;

public class PortMapper {

    public static void handlePortForward() throws IOException {
        SSDPDiscovery discovery = new SSDPDiscovery();
        discovery.bind();
        discovery.setReceiveTimeout(1);
        discovery.sendSearch(UPnPTypes.UPNP_INTERNET_GATEWAY_DEVICE_V1, 1);
        SSDPDiscovery.Response response;
        while ((response = discovery.receive()) != null) {
            Log.d("DCCManager", "Found device using SSDP: " +
                    response.getDescriptionLocation());
            UPnPDeviceDescription description = new UPnPDeviceDescription();
            try {
                description.loadFromUrl(response.getDescriptionLocation());
            } catch (Exception ex) {
                Log.w("DCCManager", "Skipping device due to a parsing error");
                ex.printStackTrace();
                continue;
            }
            UPnPDeviceDescription wanDevice = description
                    .findDeviceByType(UPnPTypes.UPNP_WAN_CONNECTION_DEVICE_V1);
            if (wanDevice == null) {
                Log.d("DCCManager", "Skipping device as it has no " +
                        "WAN connection device");
                continue;
            }
            UPnPDeviceDescription.Service serviceIP =
                    wanDevice.getServiceByType(UPnPTypes.UPNP_WAN_IP_CONNECTION_V1);
            UPnPDeviceDescription.Service servicePPP =
                    wanDevice.getServiceByType(UPnPTypes.UPNP_WAN_PPP_CONNECTION_V1);
            if (serviceIP == null && servicePPP == null) {
                Log.d("DCCManager", "Skipping device as it has no " +
                        "WAN IP or PPP connection service");
                continue;
            }
            Log.d("DCCManager", "Found a valid service");
        }
    }

}
