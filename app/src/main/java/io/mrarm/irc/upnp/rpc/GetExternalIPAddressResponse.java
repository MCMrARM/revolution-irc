package io.mrarm.irc.upnp.rpc;

import org.w3c.dom.Element;

import io.mrarm.irc.upnp.XMLParseHelper;

public class GetExternalIPAddressResponse {

    private String mNewExternalIPAddress;

    public GetExternalIPAddressResponse(Element response) {
        mNewExternalIPAddress = XMLParseHelper.getChildElementValue(response,
                "NewExternalIPAddress", null);
    }

    public String getNewExternalIPAddress() {
        return mNewExternalIPAddress;
    }

}
