package io.mrarm.irc.config;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;

public class NotificationRuleManager {

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

    private static class UserRuleSettings {

        private static int CURRENT_VERSION = 1;

        private int version;

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
        sNickMentionRule = new NotificationRule(R.string.notification_rule_nick, NotificationRule.AppliesToEntry.channelEvents(), "(^|[ ,:;@])${nick}($|[ ,:;'?])", true);
        sDirectMessageRule = new NotificationRule(R.string.notification_rule_direct, NotificationRule.AppliesToEntry.directMessages(), null);
        sDirectMessageRule.settings.mentionFormatting = false;
        sDirectNoticeRule = new NotificationRule(R.string.notification_rule_notice, NotificationRule.AppliesToEntry.directNotices(), null);
        sDirectNoticeRule.settings.mentionFormatting = false;
        sChannelNoticeRule = new NotificationRule(R.string.notification_rule_chan_notice, NotificationRule.AppliesToEntry.channelNotices(), null);
        sChannelNoticeRule.settings.noNotification = true;
        sZNCPlaybackRule = new NotificationRule(R.string.notification_rule_zncplayback, createZNCPlaybackAppliesToEntry(), null);
        sZNCPlaybackRule.settings.mentionFormatting = false;
        sZNCPlaybackRule.settings.noNotification = true;
        sZNCPlaybackRule.notEditable = true;
        sDefaultTopRules.add(NotificationRuleManager.sZNCPlaybackRule);
        sDefaultBottomRules.add(NotificationRuleManager.sNickMentionRule);
        sDefaultBottomRules.add(NotificationRuleManager.sDirectMessageRule);
        sDefaultBottomRules.add(NotificationRuleManager.sDirectNoticeRule);
        sDefaultBottomRules.add(NotificationRuleManager.sChannelNoticeRule);
    }

    public static void loadUserRuleSettings(Reader reader) {
        UserRuleSettings settings = SettingsHelper.getGson().fromJson(reader,
                UserRuleSettings.class);
        sZNCPlaybackRule.settings.enabled = settings.zncPlaybackRuleEnabled;
        sNickMentionRule.settings = settings.nickMentionRuleSettings;
        sDirectMessageRule.settings = settings.directMessageRuleSettings;
        sDirectNoticeRule.settings = settings.directNoticeRuleSettings;
        sChannelNoticeRule.settings = settings.channelNoticeRuleSettings;
        if (settings.version == 0) {
            // set the mentionFormatting to false for the direct message rules
            sDirectMessageRule.settings.mentionFormatting = false;
            sDirectNoticeRule.settings.mentionFormatting = false;
        }
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
        settings.version = UserRuleSettings.CURRENT_VERSION;
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

    public static List<NotificationRule> getDefaultTopRules() {
        return sDefaultTopRules;
    }

    public static List<NotificationRule> getDefaultBottomRules() {
        return sDefaultBottomRules;
    }

    private static NotificationRule.AppliesToEntry createZNCPlaybackAppliesToEntry() {
        NotificationRule.AppliesToEntry entry = NotificationRule.AppliesToEntry.any();
        entry.messageBatches = new ArrayList<>();
        entry.messageBatches.add("znc.in/playback");
        return entry;
    }

    static {
        initDefaultRules();
    }

}
