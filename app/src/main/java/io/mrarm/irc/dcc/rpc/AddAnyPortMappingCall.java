package io.mrarm.irc.dcc.rpc;

public class AddAnyPortMappingCall extends AddPortMappingCall {

    public static final String ACTION_NAME = "AddAnyPortMapping";

    public AddAnyPortMappingCall(String serviceType) {
        super(serviceType);
    }

    @Override
    protected String getActionName() {
        return ACTION_NAME;
    }
}
