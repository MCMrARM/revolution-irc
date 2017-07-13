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

    public List<String> nicks;
    public String user;
    public String realname;

    public List<String> autojoinChannels;
    public List<IgnoreEntry> ignoreList;

    public static class IgnoreEntry {

        public String nick;
        public String user;
        public String host;

        public boolean direct = true;
        public boolean channel = true;

    }

}
