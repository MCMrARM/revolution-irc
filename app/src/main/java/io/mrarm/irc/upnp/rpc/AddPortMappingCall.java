package io.mrarm.irc.upnp.rpc;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerException;

public class AddPortMappingCall extends BaseAddPortMappingCall {

    public static final String ACTION_NAME = "AddPortMapping";

    public AddPortMappingCall(String serviceType) {
        super(serviceType);
    }

    protected String getActionName() {
        return ACTION_NAME;
    }


    public void send(URL serviceEndpoint) throws IOException, SAXException, TransformerException,
            UPnPRPCError {
        doSend(serviceEndpoint);
    }

}
