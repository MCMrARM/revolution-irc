package io.mrarm.irc.dcc.rpc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AddPortMappingCall extends UPnPRemoteCall {

    public static final String PROTOCOL_UDP = "UDP";
    public static final String PROTOCOL_TCP = "TCP";

    private final String mNamespace;
    private String mNewRemoteHost;
    private int mNewExternalPort; // required
    private String mNewProtocol;
    private int mNewInternalPort; // required
    private String mNewInternalClient;
    private boolean mNewEnabled = true;
    private String mNewPortMappingDescription;
    private int mNewLeaseDuration = 0;

    public AddPortMappingCall(String serviceType) {
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

    public void setNewInternalPort(int value) {
        mNewInternalPort = value;
    }

    public void setNewInternalClient(String value) {
        mNewInternalClient = value;
    }

    public void setNewEnabled(boolean value) {
        mNewEnabled = value;
    }

    public void setNewPortMappingDescription(String value) {
        mNewPortMappingDescription = value;
    }

    public void setNewLeaseDuration(int value) {
        mNewLeaseDuration = value;
    }

    @Override
    protected boolean validate() {
        return (mNewExternalPort != -1 && mNewProtocol != null && mNewInternalPort != -1 &&
                mNewInternalClient != null);
    }

    @Override
    protected String getSOAPAction() {
        return mNamespace + "#AddPortMapping";
    }

    @Override
    protected Element createRequest(Document document) {
        Element ret = document.createElementNS(mNamespace, "u:AddPortMapping");
        addArgumentNode(ret, "NewRemoteHost", mNewRemoteHost != null ? mNewRemoteHost : "");
        addArgumentNode(ret, "NewExternalPort", String.valueOf(mNewExternalPort));
        addArgumentNode(ret, "NewProtocol", mNewProtocol);
        addArgumentNode(ret, "NewInternalPort", String.valueOf(mNewInternalPort));
        addArgumentNode(ret, "NewInternalClient", mNewInternalClient);
        addArgumentNode(ret, "NewEnabled", mNewEnabled ? "1" : "0");
        addArgumentNode(ret, "NewPortMappingDescription", mNewPortMappingDescription != null ?
                mNewPortMappingDescription : "");
        addArgumentNode(ret, "NewLeaseDuration", String.valueOf(mNewLeaseDuration));
        return ret;
    }

}
