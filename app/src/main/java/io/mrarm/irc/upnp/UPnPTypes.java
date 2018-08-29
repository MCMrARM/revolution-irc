package io.mrarm.irc.upnp;

public class UPnPTypes {

    public static final String UPNP_DEVICE_PREFIX = "urn:schemas-upnp-org:device:";
    public static final String UPNP_INTERNET_GATEWAY_DEVICE_V1 = UPNP_DEVICE_PREFIX +
            "InternetGatewayDevice:1";
    public static final String UPNP_WAN_CONNECTION_DEVICE_V1 = UPNP_DEVICE_PREFIX +
            "WANConnectionDevice:1";

    public static final String UPNP_SERVICE_PREFIX = "urn:schemas-upnp-org:service:";
    public static final String UPNP_WAN_IP_CONNECTION_V1 = UPNP_SERVICE_PREFIX +
            "WANIPConnection:1";
    public static final String UPNP_WAN_PPP_CONNECTION_V1 = UPNP_SERVICE_PREFIX +
            "WANPPPConnection:1";
    public static final String UPNP_WAN_IP_CONNECTION_V2 = UPNP_SERVICE_PREFIX +
            "WANIPConnection:2";

}
