package io.mrarm.irc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.mrarm.chatlib.irc.ServerConnectionApi;

public class NotificationRuleManager {

    private static NotificationRule mNickMentionRule;
    private static List<NotificationRule> mDefaultRules;

    private ServerConnectionInfo mConnection;
    Map<NotificationRule, Pattern> mCompiledPatterns = new HashMap<>();

    private static void initDefaultRules() {
        mDefaultRules = new ArrayList<>();
        mNickMentionRule = new NotificationRule("${nick}", true);
        mDefaultRules.add(mNickMentionRule);
    }

    public NotificationRuleManager(ServerConnectionInfo connection) {
        mConnection = connection;
    }

    public NotificationRule findRule(String message) {
        for (NotificationRule rule : mDefaultRules) {
            if (rule.appliesTo(this, message))
                return rule;
        }
        return null;
    }

    public String getUserNick() { // TODO: Register for nick updates
        return ((ServerConnectionApi) mConnection.getApiInstance()).getServerConnectionData().getUserNick();
    }

    static {
        initDefaultRules();
    }

}
