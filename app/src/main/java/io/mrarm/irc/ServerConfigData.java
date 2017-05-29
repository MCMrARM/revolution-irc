package io.mrarm.irc;

import java.util.List;
import java.util.UUID;

public class ServerConfigData {

    public String name;
    public UUID uuid;

    public String address;
    public int port;
    public boolean ssl;
    public String pass;

    public String nick;
    public String user;
    public String realname;

    public List<String> autojoinChannels;

}
