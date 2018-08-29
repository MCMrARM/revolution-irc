package io.mrarm.irc.upnp;

import android.util.Log;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Random;

import javax.xml.transform.TransformerException;

import io.mrarm.irc.upnp.rpc.AddAnyPortMappingCall;
import io.mrarm.irc.upnp.rpc.AddAnyPortMappingResponse;
import io.mrarm.irc.upnp.rpc.AddPortMappingCall;
import io.mrarm.irc.upnp.rpc.BaseAddPortMappingCall;
import io.mrarm.irc.upnp.rpc.DeletePortMappingCall;
import io.mrarm.irc.upnp.rpc.GetExternalIPAddressCall;
import io.mrarm.irc.upnp.rpc.GetExternalIPAddressResponse;
import io.mrarm.irc.upnp.rpc.UPnPRPCError;

public class PortMapper {

    public static PortMappingResult mapPort(PortMappingRequest request) throws IOException {
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
            String localIP = resolveLocalIP(serviceURL);
            String externalIP;
            try {
                GetExternalIPAddressCall call =
                        new GetExternalIPAddressCall(service.getServiceType());
                GetExternalIPAddressResponse resp = call.send(serviceURL);
                externalIP = resp.getNewExternalIPAddress();
            } catch (Exception e) {
                Log.w("PortMapper", "Failed to send GetExternalIPAddressCall request");
                e.printStackTrace();
                continue;
            }

            // First try to bind using AddAnyPortMapping
            if (controlDescription != null &&
                    controlDescription.findAction(AddAnyPortMappingCall.ACTION_NAME) != null) {
                try {
                    AddAnyPortMappingCall call =
                            new AddAnyPortMappingCall(service.getServiceType());
                    fillInCallData(call, request, localIP);
                    AddAnyPortMappingResponse resp = call.send(serviceURL);
                    return new PortMappingResult(request, externalIP, resp.getNewReservedPort(),
                            service.getServiceType(), serviceURL);
                } catch (Exception e) {
                    Log.w("PortMapper", "Failed to send AddAnyPortMapping request");
                    e.printStackTrace();
                }
            }
            // Then try to bind using AddPortMapping on the desired point, and if that fails try to
            // use 2 different random ports
            int attempt = 0;
            do {
                try {
                    AddPortMappingCall call =
                            new AddPortMappingCall(service.getServiceType());
                    fillInCallData(call, request, localIP);
                    if (attempt != 0)
                        call.setNewExternalPort(1024 + new Random().nextInt(65535 - 1024));
                    call.send(serviceURL);
                    return new PortMappingResult(request, externalIP, call.getNewExternalPort(),
                            service.getServiceType(), serviceURL);
                } catch (Exception e) {
                    if (e instanceof UPnPRPCError) {
                        Log.w("PortMapper", "UPnP Error: " +
                                ((UPnPRPCError) e).getErrorCode() + " " +
                                ((UPnPRPCError) e).getErrorDescription());
                    }
                    Log.w("PortMapper", "Failed to send AddPortMapping request");
                    e.printStackTrace();
                }
                attempt++;
            } while (attempt < 3);
        }
        discovery.close();
        throw new IOException("No supported gateway found");
    }

    private static void fillInCallData(BaseAddPortMappingCall call, PortMappingRequest req,
                                       String localIP) {
        call.setNewInternalPort(req.mInternalPort);
        call.setNewInternalClient(localIP);
        call.setNewExternalPort(req.mPreferredExternalPort);
        call.setNewProtocol(req.mProtocol);
        call.setNewPortMappingDescription(req.mDescription);
        if (call instanceof AddAnyPortMappingCall ||
                call.getServiceType().equals(UPnPTypes.UPNP_WAN_IP_CONNECTION_V2))
            call.setNewLeaseDuration(3600);
    }

    public static void removePortMapping(String serviceType, URL serviceURL,
                                         int port, String protocol)
            throws IOException, TransformerException, UPnPRPCError, SAXException {
        DeletePortMappingCall call = new DeletePortMappingCall(serviceType);
        call.setNewExternalPort(port);
        call.setNewProtocol(protocol);
        call.send(serviceURL);
    }

    public static void removePortMapping(PortMappingResult result) throws UPnPRPCError,
            SAXException, TransformerException, IOException {
        removePortMapping(result.mServiceType, result.mServiceURL, result.getExternalPort(),
                result.getRequest().mProtocol);
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

    public static class PortMappingRequest {

        private int mInternalPort;
        private int mPreferredExternalPort;
        private String mDescription;
        private String mProtocol;

        public PortMappingRequest(String protocol, int internalPort, int preferredExternalPort,
                                  String description) {
            mProtocol = protocol;
            mInternalPort = internalPort;
            mPreferredExternalPort = preferredExternalPort;
            mDescription = description;
        }

    }

    public static class PortMappingResult {

        private PortMappingRequest mRequest;
        private int mExternalPort;
        private String mExternalIP;
        private String mServiceType;
        private URL mServiceURL;

        public PortMappingResult(PortMappingRequest request, String externalIP, int externalPort,
                                 String serviceType, URL serviceURL) {
            mRequest = request;
            mExternalPort = externalPort;
            mExternalIP = externalIP;
            mServiceType = serviceType;
            mServiceURL = serviceURL;
        }

        public PortMappingRequest getRequest() {
            return mRequest;
        }

        public String getExternalIP() {
            return mExternalIP;
        }

        public int getExternalPort() {
            return mExternalPort;
        }

    }

}
