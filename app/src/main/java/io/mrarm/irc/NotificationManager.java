package io.mrarm.irc;

import android.content.Context;
import android.text.style.ForegroundColorSpan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.IRCColorUtils;
import io.mrarm.irc.util.SettingsHelper;

public class NotificationManager {

    public static final int CHAT_NOTIFICATION_ID_START = 10000;

    private static int mNextChatNotificationId = CHAT_NOTIFICATION_ID_START;

    public static final String RULES_PATH = "notification_rules.json";

    public static NotificationRule sNickMentionRule;
    public static NotificationRule sDirectMessageRule;
    public static NotificationRule sDirectNoticeRule;
    public static NotificationRule sChannelNoticeRule;
    public static NotificationRule sZNCPlaybackRule;
    static List<NotificationRule> sDefaultTopRules;
    static List<NotificationRule> sDefaultBottomRules;
    static List<NotificationRule> sUserRules;
    private static boolean sUserRulesLoaded;

    private ServerConnectionInfo mConnection;
    private Map<String, ChannelNotificationData> mChannelData = new HashMap<>();
    Map<NotificationRule, Pattern> mCompiledPatterns = new HashMap<>();

    private static class UserRuleSettings {

        public boolean zncPlaybackRuleEnabled = true;
        public NotificationSettings nickMentionRuleSettings;
        public NotificationSettings directMessageRuleSettings;
        public NotificationSettings directNoticeRuleSettings;
        public NotificationSettings channelNoticeRuleSettings;

        public List<NotificationRule> userRules;

    }

    private static void initDefaultRules() {
        sDefaultTopRules = new ArrayList<>();
        sDefaultBottomRules = new ArrayList<>();
        sNickMentionRule = new NotificationRule(R.string.notification_rule_nick, NotificationRule.AppliesToEntry.channelEvents(), "(^| |,|:|;)${nick}($| |,|:|;)", true);
        sDirectMessageRule = new NotificationRule(R.string.notification_rule_direct, NotificationRule.AppliesToEntry.directMessages(), null);
        sDirectNoticeRule = new NotificationRule(R.string.notification_rule_notice, NotificationRule.AppliesToEntry.directNotices(), null);
        sChannelNoticeRule = new NotificationRule(R.string.notification_rule_chan_notice, NotificationRule.AppliesToEntry.channelNotices(), null);
        sZNCPlaybackRule = new NotificationRule(R.string.notification_rule_zncplayback, createZNCPlaybackAppliesToEntry(), null);
        sZNCPlaybackRule.settings.noNotification = true;
        sZNCPlaybackRule.notEditable = true;
        sDefaultTopRules.add(NotificationManager.sZNCPlaybackRule);
        sDefaultBottomRules.add(NotificationManager.sNickMentionRule);
        sDefaultBottomRules.add(NotificationManager.sDirectMessageRule);
        sDefaultBottomRules.add(NotificationManager.sDirectNoticeRule);
        sDefaultBottomRules.add(NotificationManager.sChannelNoticeRule);
    }

    public static void loadUserRuleSettings(Reader reader) {
        UserRuleSettings settings = SettingsHelper.getGson().fromJson(reader,
                UserRuleSettings.class);
        sZNCPlaybackRule.settings.enabled = settings.zncPlaybackRuleEnabled;
        sNickMentionRule.settings = settings.nickMentionRuleSettings;
        sDirectMessageRule.settings = settings.directMessageRuleSettings;
        sDirectNoticeRule.settings = settings.directNoticeRuleSettings;
        sChannelNoticeRule.settings = settings.channelNoticeRuleSettings;
        sUserRules = settings.userRules;
    }

    public static boolean loadUserRuleSettings(Context context) {
        if (sUserRulesLoaded)
            return true;
        sUserRulesLoaded = true;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(
                    new File(context.getFilesDir(), RULES_PATH)));
            loadUserRuleSettings(reader);
            reader.close();
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void saveUserRuleSettings(Context context, Writer writer) {
        UserRuleSettings settings = new UserRuleSettings();
        settings.userRules = getUserRules(context);
        settings.zncPlaybackRuleEnabled = sZNCPlaybackRule.settings.enabled;
        settings.nickMentionRuleSettings = sNickMentionRule.settings;
        settings.directMessageRuleSettings = sDirectMessageRule.settings;
        settings.directNoticeRuleSettings = sDirectNoticeRule.settings;
        settings.channelNoticeRuleSettings = sChannelNoticeRule.settings;
        SettingsHelper.getGson().toJson(settings, writer);
    }

    public static boolean saveUserRuleSettings(Context context) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    new File(context.getFilesDir(), RULES_PATH)));
            saveUserRuleSettings(context, writer);
            writer.close();
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    public static List<NotificationRule> getUserRules(Context context) {
        if (sUserRules == null) {
            loadUserRuleSettings(context);
            if (sUserRules == null)
                sUserRules = new ArrayList<>();
        }
        return sUserRules;
    }

    private static NotificationRule.AppliesToEntry createZNCPlaybackAppliesToEntry() {
        NotificationRule.AppliesToEntry entry = NotificationRule.AppliesToEntry.any();
        entry.messageBatches = new ArrayList<>();
        entry.messageBatches.add("znc.in/playback");
        return entry;
    }

    public NotificationManager(ServerConnectionInfo connection) {
        loadUserRuleSettings(connection.getConnectionManager().getContext());
        mConnection = connection;
    }

    public NotificationRule findRule(String channel, MessageInfo message) {
        ChatApi api = mConnection.getApiInstance();
        if (api instanceof ServerConnectionApi && channel != null && channel.length() > 0 &&
                !((ServerConnectionApi) api).getServerConnectionData().getSupportList()
                        .getSupportedChannelTypes().contains(channel.charAt(0)))
            channel = null;

        for (NotificationRule rule : sDefaultTopRules) {
            if (rule.appliesTo(this, channel, message) && rule.settings.enabled)
                return rule;
        }
        for (NotificationRule rule : getUserRules(mConnection.getConnectionManager().getContext())) {
            if (rule.appliesTo(this, channel, message) && rule.settings.enabled)
                return rule;
        }
        for (NotificationRule rule : sDefaultBottomRules) {
            if (rule.appliesTo(this, channel, message) && rule.settings.enabled)
                return rule;
        }
        return null;
    }

    public ChannelNotificationData getChannelNotificationData(String channel, boolean create) {
        ChannelNotificationData ret = mChannelData.get(channel);
        if (ret == null && create) {
            ret = new ChannelNotificationData(channel);
            mChannelData.put(channel, ret);
        }
        return ret;
    }

    public Collection<ChannelNotificationData> getChannelNotificationDataList() {
        return mChannelData.values();
    }

    public UUID getServerUUID() {
        return mConnection.getUUID();
    }

    public String getUserNick() { // TODO: Register for nick updates
        return ((ServerConnectionApi) mConnection.getApiInstance()).getServerConnectionData().getUserNick();
    }

    static {
        initDefaultRules();
    }

    public static class ChannelNotificationData {

        private final String mChannel;
        private final int mNotificationId = mNextChatNotificationId++;
        private List<NotificationMessage> mMessages = new ArrayList<>();
        private boolean mOpened = false;

        public ChannelNotificationData(String channel) {
            mChannel = channel;
        }

        public String getChannel() {
            return mChannel;
        }

        public int getNotificationId() {
            return mNotificationId;
        }

        public List<NotificationMessage> getNotificationMessages() {
            return mMessages;
        }

        public NotificationMessage addNotificationMessage(MessageInfo messageInfo) {
            if (mOpened)
                return null;
            NotificationMessage ret = new NotificationMessage(messageInfo);
            mMessages.add(ret);
            return ret;
        }

        public void setOpened(boolean opened) {
            mOpened = opened;
            if (mOpened)
                mMessages.clear();
        }

    }

    public static class NotificationMessage {

        private String mSender;
        private String mText;
        private CharSequence mBuilt;

        public NotificationMessage(String sender, String text) {
            this.mSender = sender;
            this.mText = text;
        }

        public NotificationMessage(MessageInfo messageInfo) {
            this(messageInfo.getSender().getNick(), messageInfo.getMessage());
        }

        private CharSequence buildNotificationText(Context context) {
            int nickColor = IRCColorUtils.getNickColor(context, mSender.toString());
            ColoredTextBuilder builder = new ColoredTextBuilder();
            builder.append(mSender + ": ", new ForegroundColorSpan(nickColor));
            builder.append(mText);
            return mBuilt = builder.getSpannable();
        }

        public CharSequence getNotificationText(Context context) {
            if (mBuilt == null)
                return buildNotificationText(context);
            return mBuilt;
        }

    }

}
