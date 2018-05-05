package io.mrarm.irc.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.NotificationManager;

public class NotificationRule {

    private String name;
    private int nameId = -1;
    private String regex;
    private boolean regexCaseInsensitive;
    private List<AppliesToEntry> appliesTo = new ArrayList<>();
    public NotificationSettings settings = new NotificationSettings();
    public transient boolean notEditable = false;

    private transient Pattern mCompiledPattern;

    public NotificationRule() {
    }

    public NotificationRule(String name, AppliesToEntry applies, String regex, boolean caseInsensitive) {
        this.name = name;
        appliesTo.add(applies);
        setRegex(regex, caseInsensitive);
    }

    public NotificationRule(String name, AppliesToEntry applies, String regex) {
        this(name, applies, regex, false);
    }

    public NotificationRule(int nameId, AppliesToEntry applies, String regex, boolean caseInsensitive) {
        this.nameId = nameId;
        appliesTo.add(applies);
        setRegex(regex, caseInsensitive);
    }

    public NotificationRule(int nameId, AppliesToEntry applies, String regex) {
        this(nameId, applies, regex, false);
    }

    public String getName() {
        return name;
    }

    public int getNameId() {
        return nameId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegex() {
        return regex;
    }

    public boolean isRegexCaseInsensitive() {
        return regexCaseInsensitive;
    }

    public void setRegex(String regex, boolean caseInsensitive) {
        this.regex = regex;
        this.regexCaseInsensitive = caseInsensitive;
        updateRegex();
    }

    public void setMatchText(String text, boolean matchWord, boolean caseInsensitive) {
        if (matchWord)
            setRegex("(^|[ ,:;@])" + Pattern.quote(text) + "($|[ ,:;'?])", caseInsensitive);
        else
            setRegex(Pattern.quote(text), caseInsensitive);
    }

    public List<AppliesToEntry> getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(List<AppliesToEntry> appliesTo) {
        this.appliesTo = appliesTo;
    }

    public void updateRegex() {
        if (regex == null)
            return;
        Matcher matcher = CommandAliasManager.mMatchVariablesRegex.matcher(regex);
        if (!matcher.find())
            mCompiledPattern = Pattern.compile(regex, regexCaseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
    }

    public Pattern createSpecificRegex(NotificationManager.ConnectionManager conn) {
        Matcher matcher = CommandAliasManager.mMatchVariablesRegex.matcher(regex);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String type = matcher.group(1);
            String replaceWith = "";
            if (type.equals("nick"))
                replaceWith = conn.getConnection().getUserNick();
            matcher.appendReplacement(buf, Matcher.quoteReplacement(Pattern.quote(replaceWith)));
        }
        matcher.appendTail(buf);
        return Pattern.compile(buf.toString(), regexCaseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
    }

    public Pattern getCompiledPattern(NotificationManager.ConnectionManager conn) {
        if (mCompiledPattern != null)
            return mCompiledPattern;
        if (regex == null)
            return null;
        if (!conn.getCompiledPatterns().containsKey(this))
            conn.getCompiledPatterns().put(this, createSpecificRegex(conn));
        return conn.getCompiledPatterns().get(this);
    }

    public boolean appliesTo(NotificationManager.ConnectionManager conn, String channel, MessageInfo message) {
        if (regex != null && !getCompiledPattern(conn).matcher(message.getMessage()).find())
            return false;
        boolean isNotice = message.getType() == MessageInfo.MessageType.NOTICE;
        for (AppliesToEntry entry : appliesTo) {
            if (entry.server != null && entry.server != conn.getServerUUID())
                continue;
            if (channel == null) {
                if ((isNotice && !entry.matchDirectNotices) ||
                        (!isNotice && !entry.matchDirectMessages))
                    continue;
            } else {
                if ((isNotice && !entry.matchChannelNotices) ||
                        (!isNotice && !entry.matchChannelMessages) ||
                        (entry.channels != null && !entry.channels.contains(channel)))
                    continue;
            }
            if (entry.nicks != null && !entry.nicks.contains(message.getSender().getNick()))
                continue;
            if (entry.messageBatches != null && (message.getBatch() == null || !entry.messageBatches.contains(message.getBatch().getType())))
                continue;
            return true;
        }
        return false;
    }

    public static class AppliesToEntry implements Cloneable {

        public UUID server = null;
        public List<String> channels = null;
        public List<String> nicks = null;
        public List<String> messageBatches = null;

        public boolean matchDirectMessages = true;
        public boolean matchDirectNotices = true;
        public boolean matchChannelMessages = true;
        public boolean matchChannelNotices = true;

        public AppliesToEntry clone() {
            try {
                return (AppliesToEntry) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public static AppliesToEntry any() {
            return new AppliesToEntry();
        }

        public static AppliesToEntry channelEvents() {
            AppliesToEntry ret = any();
            ret.matchDirectMessages = false;
            ret.matchDirectNotices = false;
            return ret;
        }

        public static AppliesToEntry channelMessages() {
            AppliesToEntry ret = channelEvents();
            ret.matchChannelNotices = false;
            return ret;
        }

        public static AppliesToEntry channelNotices() {
            AppliesToEntry ret = channelEvents();
            ret.matchChannelMessages = false;
            return ret;
        }

        public static AppliesToEntry directEvents() {
            AppliesToEntry ret = any();
            ret.matchChannelMessages = false;
            ret.matchChannelNotices = false;
            return ret;
        }

        public static AppliesToEntry directMessages() {
            AppliesToEntry ret = directEvents();
            ret.matchDirectNotices = false;
            return ret;
        }

        public static AppliesToEntry directNotices() {
            AppliesToEntry ret = directEvents();
            ret.matchDirectMessages = false;
            return ret;
        }

    }

}
