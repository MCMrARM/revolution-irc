package io.mrarm.irc.upnp.rpc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerException;

import io.mrarm.irc.upnp.XMLParseHelper;

public class GetExternalIPAddressCall extends UPnPRemoteCall {

    public static final String ACTION_NAME = "GetExternalIPAddress";

    private final String mNamespace;

    public GetExternalIPAddressCall(String serviceType) {
        mNamespace = serviceType;
    }

    @Override
    protected boolean validate() {
        return true;
    }

    protected String getActionName() {
        return ACTION_NAME;
    }

    @Override
    protected String getSOAPAction() {
        return mNamespace + "#" + getActionName();
    }

    @Override
    protected Element createRequest(Document document) {
        return document.createElementNS(mNamespace, "u:" + getActionName());
    }


    public GetExternalIPAddressResponse send(URL serviceEndpoint) throws IOException, SAXException,
            TransformerException, UPnPRPCError {
        Document document = doSend(serviceEndpoint);
        Element body = XMLParseHelper.findChildElement(document.getDocumentElement(), "Body");
        Element resp = XMLParseHelper.findChildElement(body, ACTION_NAME + "Response");
        if (resp == null)
            throw new IOException("No response element");
        return new GetExternalIPAddressResponse(resp);
    }

}
