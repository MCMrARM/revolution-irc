package io.mrarm.irc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.irc.config.NotificationRule;
import io.mrarm.irc.config.NotificationRuleManager;

public class NotificationManager {

    public static final int CHAT_SUMMARY_NOTIFICATION_ID = 101;

    public static final String NOTIFICATION_GROUP_CHAT = "chat";

    private static final NotificationManager sInstance = new NotificationManager();

    public static NotificationManager getInstance() {
        return sInstance;
    }

    public void processMessage(Context context, ServerConnectionInfo connection, String channel,
                               MessageInfo info) {
        if (info.getMessage() == null || info.getSender().getNick().equals(
                connection.getUserNick()))
            return;

        NotificationRule rule = findNotificationRule(connection, channel, info);
        if (rule == null || rule.settings.noNotification)
            return;
        ChannelNotificationManager channelManager = connection.getNotificationData().getChannelManager(channel, true);
        if (channelManager.addNotificationMessage(info)) {
            updateSummaryNotification(context);
            channelManager.showNotification(context, rule);
        }
    }

    public void clearAllNotifications(Context context, ServerConnectionInfo connection) {
        ConnectionData connectionData = connection.getNotificationData();
        for (ChannelNotificationManager mgr : connectionData.getChannelManagerList())
            mgr.cancelNotification(context);
        connectionData.getChannelManagerList().clear();
        updateSummaryNotification(context);
    }

    private NotificationRule findNotificationRule(ServerConnectionInfo connection, String channel,
                                                  MessageInfo message) {
        ChatApi api = connection.getApiInstance();
        if (api instanceof ServerConnectionApi && channel != null && channel.length() > 0 &&
                !((ServerConnectionApi) api).getServerConnectionData().getSupportList()
                        .getSupportedChannelTypes().contains(channel.charAt(0)))
            channel = null;

        ConnectionData connData = connection.getNotificationData();
        for (NotificationRule rule : NotificationRuleManager.getDefaultTopRules()) {
            if (rule.appliesTo(connData, channel, message) && rule.settings.enabled)
                return rule;
        }
        for (NotificationRule rule : NotificationRuleManager.getUserRules(connection.getConnectionManager().getContext())) {
            if (rule.appliesTo(connData, channel, message) && rule.settings.enabled)
                return rule;
        }
        for (NotificationRule rule : NotificationRuleManager.getDefaultBottomRules()) {
            if (rule.appliesTo(connData, channel, message) && rule.settings.enabled)
                return rule;
        }
        return null;
    }

    public void onNotificationDismissed(Context context, ServerConnectionInfo connection,
                                        String channel) {
        ChannelNotificationManager channelManager = connection.getNotificationData()
                .getChannelManager(channel, true);
        channelManager.onNotificationDismissed();
    }


    public void updateSummaryNotification(Context context) {
        ChannelNotificationManager first = null;
        boolean isLong = false;
        int notificationCount = 0;
        StringBuilder longBuilder = new StringBuilder();
        for (ServerConnectionInfo info : ServerConnectionManager.getInstance(context).getConnections()) {
            for (ChannelNotificationManager channelManager : info.getNotificationData().getChannelManagerList()) {
                if (channelManager.getNotificationMessages().size() == 0)
                    continue;
                if (first == null) {
                    first = channelManager;
                } else {
                    longBuilder.append(context.getString(R.string.text_comma));
                    isLong = true;
                }
                longBuilder.append(channelManager.getChannel());
                notificationCount++;
            }
        }
        // Remove the notification if no notification entries were found
        if (first == null) {
            NotificationManagerCompat.from(context).cancel(CHAT_SUMMARY_NOTIFICATION_ID);
            return;
        }
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_message)
                .setGroup(NOTIFICATION_GROUP_CHAT)
                .setGroupSummary(true)
                .setColor(context.getResources().getColor(R.color.colorNotificationMention));
        if (isLong) {
            PendingIntent intent = PendingIntent.getActivity(context, CHAT_SUMMARY_NOTIFICATION_ID,
                    new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
            notification
                    .setContentTitle(context.getResources().getQuantityString(R.plurals.notify_multiple_messages, notificationCount, notificationCount))
                    .setContentText(longBuilder.toString())
                    .setContentIntent(intent);
        } else {
            List<ChannelNotificationManager.NotificationMessage> list = first.getNotificationMessages();
            PendingIntent intent = PendingIntent.getActivity(context, CHAT_SUMMARY_NOTIFICATION_ID,
                    MainActivity.getLaunchIntent(context, first.getConnection(), first.getChannel()),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            notification
                    .setContentTitle(first.getChannel())
                    .setContentText(list.get(list.size() - 1).getNotificationText(context))
                    .setContentIntent(intent);
        }
        NotificationManagerCompat.from(context).notify(CHAT_SUMMARY_NOTIFICATION_ID, notification.build());
    }

    public static class ConnectionData {

        private ServerConnectionInfo mConnection;
        private Map<NotificationRule, Pattern> mCompiledPatterns = new HashMap<>();
        private Map<String, ChannelNotificationManager> mChannels = new HashMap<>();

        public ConnectionData(ServerConnectionInfo connection) {
            mConnection = connection;
        }

        public ServerConnectionInfo getConnection() {
            return mConnection;
        }

        public Map<NotificationRule, Pattern> getCompiledPatterns() {
            return mCompiledPatterns;
        }

        public ChannelNotificationManager getChannelManager(String channel, boolean create) {
            ChannelNotificationManager ret = mChannels.get(channel);
            if (ret == null && create) {
                ret = new ChannelNotificationManager(mConnection, channel);
                mChannels.put(channel, ret);
            }
            return ret;
        }

        public Collection<ChannelNotificationManager> getChannelManagerList() {
            return mChannels.values();
        }

        public UUID getServerUUID() {
            return mConnection.getUUID();
        }

    }


}
