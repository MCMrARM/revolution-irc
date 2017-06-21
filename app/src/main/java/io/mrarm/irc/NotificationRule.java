package io.mrarm.irc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationRule {

    private static Pattern mMatchVariablesRegex = Pattern.compile("(?<!\\\\)\\$\\{(.*)\\}");

    private String name;
    private int nameId = -1;
    private String regex;
    private boolean regexCaseInsensitive;
    public NotificationSettings settings = new NotificationSettings();

    private transient Pattern mCompiledPattern;

    public NotificationRule() {
    }

    public NotificationRule(String name, String regex, boolean caseInsensitive) {
        this.name = name;
        setRegex(regex, caseInsensitive);
    }

    public NotificationRule(String name, String regex) {
        this.name = name;
        setRegex(regex, false);
    }

    public NotificationRule(int nameId, String regex, boolean caseInsensitive) {
        this.nameId = nameId;
        setRegex(regex, caseInsensitive);
    }

    public String getName() {
        return name;
    }

    public int getNameId() {
        return nameId;
    }

    public void setRegex(String regex, boolean caseInsensitive) {
        this.regex = regex;
        this.regexCaseInsensitive = caseInsensitive;
        updateRegex();
    }

    public void updateRegex() {
        Matcher matcher = mMatchVariablesRegex.matcher(regex);
        if (!matcher.find())
            mCompiledPattern = Pattern.compile(regex, regexCaseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
    }

    public Pattern createSpecificRegex(NotificationManager manager) {
        Matcher matcher = mMatchVariablesRegex.matcher(regex);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            String type = matcher.group(1);
            String replaceWith = "";
            if (type.equals("nick"))
                replaceWith = manager.getUserNick();
            matcher.appendReplacement(buf, Matcher.quoteReplacement(replaceWith));
        }
        matcher.appendTail(buf);
        return Pattern.compile(buf.toString(), regexCaseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
    }

    public Pattern getCompiledPattern(NotificationManager manager) {
        if (mCompiledPattern != null)
            return mCompiledPattern;
        if (!manager.mCompiledPatterns.containsKey(this))
            manager.mCompiledPatterns.put(this, createSpecificRegex(manager));
        return manager.mCompiledPatterns.get(this);
    }

    public boolean appliesTo(NotificationManager manager, String message) {
        return getCompiledPattern(manager).matcher(message).find();
    }

}
