package io.mrarm.irc.upnp.rpc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class BaseAddPortMappingCall extends UPnPRemoteCall {

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

    public BaseAddPortMappingCall(String serviceType) {
        mNamespace = serviceType;
    }

    public String getServiceType() {
        return mNamespace;
    }

    public void setNewRemoteHost(String value) {
        mNewRemoteHost = value;
    }

    public String getNewRemoteHost() {
        return mNewRemoteHost;
    }

    public void setNewExternalPort(int value) {
        mNewExternalPort = value;
    }

    public int getNewExternalPort() {
        return mNewExternalPort;
    }

    public void setNewProtocol(String value) {
        mNewProtocol = value;
    }

    public String getNewProtocol() {
        return mNewProtocol;
    }

    public void setNewInternalPort(int value) {
        mNewInternalPort = value;
    }

    public int getNewInternalPort() {
        return mNewInternalPort;
    }

    public void setNewInternalClient(String value) {
        mNewInternalClient = value;
    }

    public String getNewInternalClient() {
        return mNewInternalClient;
    }

    public void setNewEnabled(boolean value) {
        mNewEnabled = value;
    }

    public boolean getNewEnabled() {
        return mNewEnabled;
    }

    public void setNewPortMappingDescription(String value) {
        mNewPortMappingDescription = value;
    }

    public String getNewPortMappingDescription() {
        return mNewPortMappingDescription;
    }

    public void setNewLeaseDuration(int value) {
        mNewLeaseDuration = value;
    }

    public int getNewLeaseDuration() {
        return mNewLeaseDuration;
    }

    @Override
    protected boolean validate() {
        return (mNewExternalPort != -1 && mNewProtocol != null && mNewInternalPort != -1 &&
                mNewInternalClient != null);
    }

    protected abstract String getActionName();

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
        addArgumentNode(ret, "NewInternalPort", String.valueOf(mNewInternalPort));
        addArgumentNode(ret, "NewInternalClient", mNewInternalClient);
        addArgumentNode(ret, "NewEnabled", mNewEnabled ? "1" : "0");
        addArgumentNode(ret, "NewPortMappingDescription", mNewPortMappingDescription != null ?
                mNewPortMappingDescription : "");
        addArgumentNode(ret, "NewLeaseDuration", String.valueOf(mNewLeaseDuration));
        return ret;
    }

}
