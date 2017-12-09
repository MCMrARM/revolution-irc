package io.mrarm.irc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.NoCopySpan;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.ChannelListListener;
import io.mrarm.chatlib.ChatApi;
import io.mrarm.chatlib.android.storage.SQLiteMessageStorageApi;
import io.mrarm.chatlib.irc.IRCConnection;
import io.mrarm.chatlib.irc.IRCConnectionRequest;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.irc.cap.SASLCapability;
import io.mrarm.chatlib.irc.cap.SASLOptions;
import io.mrarm.chatlib.irc.filters.ZNCPlaybackMessageFilter;
import io.mrarm.chatlib.irc.handlers.MessageCommandHandler;
import io.mrarm.chatlib.message.MessageStorageApi;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.dialog.ThemedAlertDialog;
import io.mrarm.irc.util.IgnoreListMessageFilter;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.SimpleTextVariableList;
import io.mrarm.irc.util.SpannableStringHelper;
import io.mrarm.irc.util.WarningHelper;

public class ServerConnectionInfo {

    private static final int HISTORY_MAX_COUNT = 24;

    private static Handler mReconnectHandler = new Handler(Looper.getMainLooper());

    private ServerConnectionManager mManager;
    private ServerConfigData mServerConfig;
    private List<String> mChannels;
    private ChatApi mApi;
    private IRCConnectionRequest mConnectionRequest;
    private SASLOptions mSASLOptions;
    private boolean mExpandedInDrawer = true;
    private boolean mConnected = false;
    private boolean mConnecting = false;
    private boolean mDisconnecting = false;
    private boolean mUserDisconnectRequest = false;
    private long mReconnectQueueTime = -1L;
    private NotificationManager.ConnectionManager mNotificationData;
    private final List<InfoChangeListener> mInfoListeners = new ArrayList<>();
    private final List<ChannelListChangeListener> mChannelsListeners = new ArrayList<>();
    private int mCurrentReconnectAttempt = -1;
    int mChatLogStorageUpdateCounter = 0;
    private final List<CharSequence> mSentMessageHistory = new ArrayList<>();

    public ServerConnectionInfo(ServerConnectionManager manager, ServerConfigData config,
                                IRCConnectionRequest connectionRequest, SASLOptions saslOptions,
                                List<String> joinChannels) {
        mManager = manager;
        mServerConfig = config;
        mConnectionRequest = connectionRequest;
        mSASLOptions = saslOptions;
        mNotificationData = new NotificationManager.ConnectionManager(this);
        mChannels = joinChannels;
        if (mChannels != null)
            Collections.sort(mChannels, String::compareToIgnoreCase);
    }

    private void setApi(ChatApi api) {
        synchronized (this) {
            mApi = api;
            api.getJoinedChannelList((List<String> channels) -> {
                setChannels(channels);
            }, null);
            api.subscribeChannelList(new ChannelListListener() {
                @Override
                public void onChannelListChanged(List<String> list) {
                    setChannels(list);
                }

                @Override
                public void onChannelJoined(String s) {
                }

                @Override
                public void onChannelLeft(String s) {
                }
            }, null, null);
        }
    }

    public ServerConnectionManager getConnectionManager() {
        return mManager;
    }

    public void connect() {
        synchronized (this) {
            if (mDisconnecting)
                throw new RuntimeException("Trying to connect with mDisconnecting set");
            if (mConnected || mConnecting)
                return;
            mConnecting = true;
            mUserDisconnectRequest = false;
            mReconnectQueueTime = -1L;
        }
        Log.i("ServerConnectionInfo", "Connecting...");

        IRCConnection connection = null;
        boolean createdNewConnection = false;
        if (mApi == null || !(mApi instanceof IRCConnection)) {
            connection = new IRCConnection();
            ServerConfigManager configManager = ServerConfigManager.getInstance(mManager.getContext());
            connection.getServerConnectionData().setMessageStorageApi(new SQLiteMessageStorageApi(configManager.getServerChatLogDir(getUUID())));
            connection.getServerConnectionData().getMessageFilterList().addMessageFilter(new IgnoreListMessageFilter(mServerConfig));
            if (mSASLOptions != null)
                connection.getServerConnectionData().getCapabilityManager().registerCapability(
                        new SASLCapability(mSASLOptions));
            connection.getServerConnectionData().getMessageFilterList().addMessageFilter(
                    new ZNCPlaybackMessageFilter(connection.getServerConnectionData()));
            connection.getServerConnectionData().getCommandHandlerList().getHandler(
                    MessageCommandHandler.class).setCtcpVersionReply(mManager.getContext()
                    .getString(R.string.app_name), BuildConfig.VERSION_NAME, "Android");
            connection.addDisconnectListener((IRCConnection conn, Exception reason) -> {
                notifyDisconnected();
            });
            createdNewConnection = true;
        } else {
            connection = (IRCConnection) mApi;
        }

        IRCConnection fConnection = connection;

        List<String> rejoinChannels = getChannels();

        connection.connect(mConnectionRequest, (Void v) -> {
            synchronized (this) {
                mConnecting = false;
                setConnected(true);
                mCurrentReconnectAttempt = 0;
            }

            List<String> joinChannels = new ArrayList<>();
            if (mServerConfig.execCommandsConnected != null)
                executeUserCommands(mServerConfig.execCommandsConnected);

            if (mServerConfig.autojoinChannels != null)
                joinChannels.addAll(mServerConfig.autojoinChannels);
            if (rejoinChannels != null && mServerConfig.rejoinChannels)
                joinChannels.addAll(rejoinChannels);
            if (joinChannels.size() > 0)
                fConnection.joinChannels(joinChannels, null, null);

        }, (Exception e) -> {
            if (e instanceof UserOverrideTrustManager.UserRejectedCertificateException ||
                    (e.getCause() != null && e.getCause() instanceof
                            UserOverrideTrustManager.UserRejectedCertificateException)) {
                Log.d("ServerConnectionInfo", "User rejected the certificate");
                synchronized (this) {
                    mUserDisconnectRequest = true;
                }
            }
            notifyDisconnected();
        });

        if (createdNewConnection) {
            setApi(connection);
        }
    }

    private void executeUserCommands(List<String> cmds) {
        IRCConnection conn = (IRCConnection) getApiInstance();
        List<String> errors = null;
        for (String cmd : cmds) {
            if (cmd.length() == 0)
                continue;
            if (!cmd.startsWith("/")) {
                conn.sendCommandRaw(cmd, null, null);
                continue;
            }
            cmd = cmd.substring(1);

            SimpleTextVariableList vars = new SimpleTextVariableList();
            vars.set(CommandAliasManager.VAR_MYNICK, getUserNick());
            try {
                CommandAliasManager.ProcessCommandResult result = CommandAliasManager
                        .getInstance(mManager.getContext())
                        .processCommand(conn.getServerConnectionData(), cmd, vars);
                if (result != null) {
                    if (result.mode == CommandAliasManager.CommandAlias.MODE_RAW) {
                        conn.sendCommandRaw(result.text, null, null);
                    } else if (result.mode == CommandAliasManager.CommandAlias.MODE_MESSAGE) {
                        if (result.channel == null)
                            throw new RuntimeException();
                        if (!getChannels().contains(result.channel)) {
                            ArrayList<String> list = new ArrayList<>();
                            list.add(result.channel);
                            conn.joinChannels(list, (Void v) -> {
                                conn.sendMessage(result.channel, result.text, null, null);
                            }, null);
                        } else {
                            conn.sendMessage(result.channel, result.text, null, null);
                        }
                    } else {
                        throw new RuntimeException();
                    }
                } else {
                    throw new RuntimeException();
                }
            } catch (RuntimeException e) {
                Log.e("ServerConnectionInfo", "User command execution failed: " + cmd);
                e.printStackTrace();
                if (errors == null)
                    errors = new ArrayList<>();
                errors.add(cmd);
            }
        }
        if (errors != null) {
            WarningHelper.showWarning(new CommandProcessErrorWarning(getName(), errors));
        }
    }

    public void disconnect() {
        synchronized (this) {
            mUserDisconnectRequest = true;
            mReconnectHandler.removeCallbacks(mReconnectRunnable);
            if (!isConnected() && isConnecting()) {
                mConnecting = false;
                mDisconnecting = true;
                Thread disconnectThread = new Thread(() -> {
                    ((IRCConnection) getApiInstance()).disconnect(true);
                });
                disconnectThread.setName("Disconnect Thread");
                disconnectThread.start();
            } else if (isConnected()) {
                mDisconnecting = true;
                String message = SettingsHelper.getInstance(mManager.getContext()).getDefaultQuitMessage();
                mApi.quit(message, null, (Exception e) -> {
                    ((IRCConnection) getApiInstance()).disconnect(true);
                });
            } else {
                notifyFullyDisconnected();
            }
        }
    }

    private void notifyDisconnected() {
        if (isDisconnecting()) {
            notifyFullyDisconnected();
            return;
        }
        synchronized (this) {
            setConnected(false);
            mConnecting = false;
            if (mDisconnecting) {
                notifyFullyDisconnected();
                return;
            }
            if (mUserDisconnectRequest)
                return;
        }
        int reconnectDelay = mManager.getReconnectDelay(mCurrentReconnectAttempt++);
        if (reconnectDelay == -1)
            return;
        Log.i("ServerConnectionInfo", "Queuing reconnect in " + reconnectDelay + " ms");
        mReconnectQueueTime = System.nanoTime();
        mReconnectHandler.postDelayed(mReconnectRunnable, reconnectDelay);
    }

    private void notifyFullyDisconnected() {
        synchronized (this) {
            setConnected(false);
            mConnecting = false;
            mDisconnecting = false;
            if (getApiInstance() != null) {
                MessageStorageApi m = getApiInstance().getMessageStorageApi();
                if (m != null && m instanceof SQLiteMessageStorageApi)
                    ((SQLiteMessageStorageApi) m).close();
            }
        }
        mManager.notifyConnectionFullyDisconnected(this);
    }

    public void notifyConnectivityChanged(boolean hasAnyConnectivity, boolean hasWifi) {
        mReconnectHandler.removeCallbacks(mReconnectRunnable);

        SettingsHelper helper = SettingsHelper.getInstance(getConnectionManager().getContext());
        if (!hasAnyConnectivity || !helper.isReconnectEnabled() || (helper.isReconnectWifiRequired()
                && !hasWifi))
            return;
        if (helper.shouldReconnectOnConnectivityChange()) {
            connect(); // this will be ignored if we are already corrected
        } else if (mReconnectQueueTime != -1L) {
            long reconnectDelay = mManager.getReconnectDelay(mCurrentReconnectAttempt++);
            if (reconnectDelay == -1)
                return;
            reconnectDelay = reconnectDelay - (System.nanoTime() - mReconnectQueueTime) / 1000000L;
            if (reconnectDelay <= 0L)
                connect();
            else
                mReconnectHandler.postDelayed(mReconnectRunnable, reconnectDelay);
        }
    }

    public UUID getUUID() {
        return mServerConfig.uuid;
    }

    public String getName() {
        return mServerConfig.name;
    }

    public ChatApi getApiInstance() {
        synchronized (this) {
            return mApi;
        }
    }

    public boolean isConnected() {
        synchronized (this) {
            return mConnected;
        }
    }

    public void setConnected(boolean connected) {
        synchronized (this) {
            mConnected = connected;
        }
        notifyInfoChanged();
    }

    public boolean isConnecting() {
        synchronized (this) {
            return mConnecting;
        }
    }

    public boolean isDisconnecting() {
        synchronized (this) {
            return mDisconnecting;
        }
    }

    public boolean hasUserDisconnectRequest() {
        synchronized (this) {
            return mUserDisconnectRequest;
        }
    }

    public List<String> getChannels() {
        synchronized (this) {
            return mChannels;
        }
    }

    public void setChannels(List<String> channels) {
        Collections.sort(channels, String::compareToIgnoreCase);
        synchronized (this) {
            mChannels = channels;
        }
        synchronized (mChannelsListeners) {
            mManager.notifyChannelListChanged(this, channels);
            mManager.saveAutoconnectListAsync();
            List<ChannelListChangeListener> listeners = new ArrayList<>(mChannelsListeners);
            for (ChannelListChangeListener listener : listeners)
                listener.onChannelListChanged(this, channels);
        }
    }

    public boolean isExpandedInDrawer() {
        synchronized (this) {
            return mExpandedInDrawer;
        }
    }

    public void setExpandedInDrawer(boolean expanded) {
        synchronized (this) {
            mExpandedInDrawer = expanded;
        }
    }

    public NotificationManager.ConnectionManager getNotificationManager() {
        return mNotificationData;
    }

    public String getUserNick() {
        return ((ServerConnectionApi) getApiInstance()).getServerConnectionData().getUserNick();
    }

    // Should be called only from main thread
    public List<CharSequence> getSentMessageHistory() {
        return mSentMessageHistory;
    }

    // Should be called only from main thread
    public void addHistoryMessage(CharSequence msg) {
        SpannableString str = new SpannableString(msg);
        for (Object o : str.getSpans(0, str.length(), NoCopySpan.class))
            str.removeSpan(o);
        mSentMessageHistory.add(str);
        if (mSentMessageHistory.size() >= HISTORY_MAX_COUNT)
            mSentMessageHistory.remove(0);
    }

    public void addOnChannelInfoChangeListener(InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.add(listener);
        }
    }

    public void removeOnChannelInfoChangeListener(InfoChangeListener listener) {
        synchronized (mInfoListeners) {
            mInfoListeners.remove(listener);
        }
    }

    public void addOnChannelListChangeListener(ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.add(listener);
        }
    }

    public void removeOnChannelListChangeListener(ChannelListChangeListener listener) {
        synchronized (mChannelsListeners) {
            mChannelsListeners.remove(listener);
        }
    }

    private void notifyInfoChanged() {
        synchronized (mInfoListeners) {
            for (InfoChangeListener listener : mInfoListeners)
                listener.onConnectionInfoChanged(this);
            mManager.notifyConnectionInfoChanged(this);
        }
    }

    private Runnable mReconnectRunnable = () -> {
        mReconnectQueueTime = -1L;
        SettingsHelper helper = SettingsHelper.getInstance(mManager.getContext());
        if (!helper.isReconnectEnabled() || (helper.isReconnectWifiRequired() &&
                !ServerConnectionManager.isWifiConnected(mManager.getContext())))
            return;
        this.connect();
    };

    public interface InfoChangeListener {
        void onConnectionInfoChanged(ServerConnectionInfo connection);
    }

    public interface ChannelListChangeListener {
        void onChannelListChanged(ServerConnectionInfo connection, List<String> newChannels);
    }


    public static class CommandProcessErrorWarning extends WarningHelper.Warning {

        private AlertDialog mDialog;
        private String mNetworkName;
        private List<String> mCommands;

        public CommandProcessErrorWarning(String networkName, List<String> commands) {
            mNetworkName = networkName;
            mCommands = commands;
        }

        @Override
        public void showDialog(Activity activity) {
            super.showDialog(activity);
            dismissDialog(activity);
            ThemedAlertDialog.Builder dialog = new ThemedAlertDialog.Builder(activity);
            dialog.setTitle(R.string.connection_error_command_title);

            StringBuilder commands = new StringBuilder();
            for (String cmd : mCommands) {
                commands.append('/');
                commands.append(cmd);
                commands.append('\n');
            }
            SpannableString commandsSeq = new SpannableString(commands);
            commandsSeq.setSpan(new TypefaceSpan("monospace"), 0, commandsSeq.length(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog.setMessage(SpannableStringHelper.format(activity.getResources().getQuantityText(
                    R.plurals.connection_error_command_dialog_content, mCommands.size()),
                    mNetworkName, commandsSeq));
            dialog.setPositiveButton(R.string.action_ok, null);
            dialog.setOnDismissListener((DialogInterface di) -> {
                dismiss();
            });
            mDialog = dialog.show();
        }

        @Override
        public void dismissDialog(Activity activity) {
            if (mDialog != null) {
                mDialog.setOnDismissListener(null);
                mDialog.dismiss();
                mDialog = null;
            }
        }

        @Override
        protected void buildNotification(Context context, NotificationCompat.Builder notification, int notificationId) {
            super.buildNotification(context, notification, notificationId);
            notification.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notification.setContentTitle(context.getString(R.string.connection_error_command_title));
            notification.setContentText(context.getString(R.string.connection_error_command_notification_desc));
            notification.setContentIntent(PendingIntent.getActivity(context, notificationId, MainActivity.getLaunchIntent(context, null, null), PendingIntent.FLAG_CANCEL_CURRENT));
        }

    }

}
