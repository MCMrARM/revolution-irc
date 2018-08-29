package io.mrarm.irc.upnp.rpc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerException;

public class DeletePortMappingCall extends UPnPRemoteCall {

    public static final String ACTION_NAME = "DeletePortMapping";

    private final String mNamespace;
    private String mNewRemoteHost;
    private int mNewExternalPort; // required
    private String mNewProtocol;

    public DeletePortMappingCall(String serviceType) {
        mNamespace = serviceType;
    }

    public void setNewRemoteHost(String value) {
        mNewRemoteHost = value;
    }

    public void setNewExternalPort(int value) {
        mNewExternalPort = value;
    }

    public void setNewProtocol(String value) {
        mNewProtocol = value;
    }

    @Override
    protected boolean validate() {
        return (mNewExternalPort != -1 && mNewProtocol != null);
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
        Element ret = document.createElementNS(mNamespace, "u:" + getActionName());
        addArgumentNode(ret, "NewRemoteHost", mNewRemoteHost != null ? mNewRemoteHost : "");
        addArgumentNode(ret, "NewExternalPort", String.valueOf(mNewExternalPort));
        addArgumentNode(ret, "NewProtocol", mNewProtocol);
        return ret;
    }


    public void send(URL serviceEndpoint) throws IOException, SAXException,
            TransformerException, UPnPRPCError {
        doSend(serviceEndpoint);
    }

}
