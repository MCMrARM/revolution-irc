package io.mrarm.irc;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import io.mrarm.irc.util.SimpleWildcardPattern;

public class ServerConfigData {

    public static final String AUTH_PASSWORD = "password";
    public static final String AUTH_SASL = "sasl";

    public String name;
    public UUID uuid;

    public String address;
    public int port;
    public boolean ssl;
    public String authMode;
    public String authUser;
    public String authPass;

    public List<String> nicks;
    public String user;
    public String realname;

    public List<String> autojoinChannels;
    public List<IgnoreEntry> ignoreList;

    public static class IgnoreEntry {

        public String nick;
        public String user;
        public String host;
        public String comment;
        public transient Pattern nickRegex;
        public transient Pattern userRegex;
        public transient Pattern hostRegex;

        public boolean matchDirectMessages = true;
        public boolean matchDirectNotices = true;
        public boolean matchChannelMessages = true;
        public boolean matchChannelNotices = true;

        public void updateRegexes() {
            nickRegex = SimpleWildcardPattern.compile(nick);
            userRegex = SimpleWildcardPattern.compile(user);
            hostRegex = SimpleWildcardPattern.compile(host);
        }

    }

}
