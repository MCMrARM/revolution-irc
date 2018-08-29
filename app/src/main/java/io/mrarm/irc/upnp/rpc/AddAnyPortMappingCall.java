package io.mrarm.irc.upnp.rpc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerException;

import io.mrarm.irc.upnp.XMLParseHelper;

public class AddAnyPortMappingCall extends BaseAddPortMappingCall {

    public static final String ACTION_NAME = "AddAnyPortMapping";

    public AddAnyPortMappingCall(String serviceType) {
        super(serviceType);
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }

    public AddAnyPortMappingResponse send(URL serviceEndpoint) throws IOException, SAXException,
            TransformerException, UPnPRPCError {
        Document document = doSend(serviceEndpoint);
        Element body = XMLParseHelper.findChildElement(document.getDocumentElement(), "Body");
        Element resp = XMLParseHelper.findChildElement(body, ACTION_NAME + "Response");
        if (resp == null)
            throw new IOException("No response element");
        return new AddAnyPortMappingResponse(resp);
    }

}
