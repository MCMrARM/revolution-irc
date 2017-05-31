package io.mrarm.irc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.mrarm.chatlib.irc.ServerConnectionApi;

public class NotificationManager {

    public static final int CHAT_NOTIFICATION_ID_START = 10000;

    private static int mNextChatNotificationId = CHAT_NOTIFICATION_ID_START;

    private static NotificationRule mNickMentionRule;
    private static List<NotificationRule> mDefaultRules;

    private final int mNotificationId = mNextChatNotificationId++;
    private ServerConnectionInfo mConnection;
    private List<CharSequence> mNotificationBacklog = new ArrayList<>();
    Map<NotificationRule, Pattern> mCompiledPatterns = new HashMap<>();

    private static void initDefaultRules() {
        mDefaultRules = new ArrayList<>();
        mNickMentionRule = new NotificationRule("${nick}", true);
        mDefaultRules.add(mNickMentionRule);
    }

    public NotificationManager(ServerConnectionInfo connection) {
        mConnection = connection;
    }

    public NotificationRule findRule(String message) {
        for (NotificationRule rule : mDefaultRules) {
            if (rule.appliesTo(this, message))
                return rule;
        }
        return null;
    }

    public int getServiceNotificationId() {
        return mNotificationId;
    }

    public List<CharSequence> getServiceNotificationBacklog() {
        return mNotificationBacklog;
    }

    public String getUserNick() { // TODO: Register for nick updates
        return ((ServerConnectionApi) mConnection.getApiInstance()).getServerConnectionData().getUserNick();
    }

    static {
        initDefaultRules();
    }

}
