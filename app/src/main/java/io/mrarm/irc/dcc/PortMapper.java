package io.mrarm.irc.dcc;

import android.util.Log;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URL;

import javax.xml.transform.TransformerException;

import io.mrarm.irc.dcc.rpc.AddAnyPortMappingCall;
import io.mrarm.irc.dcc.rpc.AddPortMappingCall;
import io.mrarm.irc.dcc.rpc.DeletePortMappingCall;

public class PortMapper {

    public static void mapPort(int localPort, int preferredExternalPort,
                               String description) throws IOException {
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


            URL controlDescURL = new URL(new URL(response.getDescriptionLocation()),
                    service.getSCPDURL());
            UPnPServiceControlDescription controlDescription =
                    new UPnPServiceControlDescription();
            try {
                controlDescription.loadFromUrl(controlDescURL.toString());
            } catch (IOException | SAXException e) {
                Log.w("PortMapper", "Failed to fetch service control description");
                e.printStackTrace();
                controlDescription = null;
            }

            URL serviceURL = new URL(new URL(response.getDescriptionLocation()),
                    service.getControlURL());

            try {
                DeletePortMappingCall test = new DeletePortMappingCall(service.getServiceType());
                test.setNewExternalPort(preferredExternalPort);
                test.setNewProtocol(AddPortMappingCall.PROTOCOL_TCP);
                test.send(serviceURL);

                boolean canUseAddAny = controlDescription != null &&
                        controlDescription.findAction(AddAnyPortMappingCall.ACTION_NAME) != null;
                AddPortMappingCall call = canUseAddAny ?
                        new AddAnyPortMappingCall(service.getServiceType()) :
                        new AddPortMappingCall(service.getServiceType());
                call.setNewInternalPort(localPort);
                call.setNewInternalClient(resolveLocalIP(serviceURL));
                call.setNewExternalPort(preferredExternalPort);
                call.setNewProtocol(AddPortMappingCall.PROTOCOL_TCP);
                call.setNewPortMappingDescription(description);
                if (canUseAddAny || service.getServiceType()
                        .equals(UPnPTypes.UPNP_WAN_IP_CONNECTION_V2))
                    call.setNewLeaseDuration(3600);
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
                wanDevice.getServiceByType(UPnPTypes.UPNP_WAN_IP_CONNECTION_V2);
        if (serviceIP != null)
            return serviceIP;
        serviceIP = wanDevice.getServiceByType(UPnPTypes.UPNP_WAN_IP_CONNECTION_V1);
        UPnPDeviceDescription.Service servicePPP =
                wanDevice.getServiceByType(UPnPTypes.UPNP_WAN_PPP_CONNECTION_V1);
        if (serviceIP != null || servicePPP != null)
            return (servicePPP != null ? servicePPP : serviceIP);
        Log.d("PortMapper", "Device has no WAN IP or PPP connection service");
        return null;
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

    public static class PortMapping {

        private int mExternalPort;

        public int getExternalPort() {
            return mExternalPort;
        }



    }

}
