package io.mrarm.irc.upnp.rpc;

import org.w3c.dom.Element;

import io.mrarm.irc.upnp.XMLParseHelper;

public class UPnPRPCError extends Exception {

    private String mSOAPFaultCode;
    private String mSOAPFaultString;

    private int mErrorCode;
    private String mErrorDescription;

    public UPnPRPCError(Element element) {
        mSOAPFaultCode = XMLParseHelper.getChildElementValue(element, "faultCode", null);
        mSOAPFaultString = XMLParseHelper.getChildElementValue(element, "faultString", null);
        Element detailError = XMLParseHelper.findChildElement(element, "detail");
        Element upnpError = XMLParseHelper.findChildElement(detailError, "UPnPError");
        try {
            mErrorCode = Integer.parseInt(XMLParseHelper.getChildElementValue(upnpError, "errorCode", ""));
        } catch (NumberFormatException ignored) {
        }
        mErrorDescription = XMLParseHelper.getChildElementValue(upnpError, "errorDescription", "");
    }

    public String getSOAPFaultCode() {
        return mSOAPFaultCode;
    }

    public String getSOAPFaultString() {
        return mSOAPFaultString;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public String getErrorDescription() {
        return mErrorDescription;
    }

    @Override
    public String getMessage() {
        return getErrorDescription();
    }
}
