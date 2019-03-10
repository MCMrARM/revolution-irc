package io.mrarm.irc;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StatFs;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.android.storage.SQLiteMessageStorageApi;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.config.NotificationCountStorage;
import io.mrarm.irc.config.NotificationRuleManager;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;
import io.mrarm.irc.dialog.ServerStorageLimitDialog;
import io.mrarm.irc.dialog.StorageLimitsDialog;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.StubMessageStorageApi;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.view.SimpleBarChart;

public class StorageSettingsAdapter extends RecyclerView.Adapter {

    public static final int TYPE_SERVER_LOGS_SUMMARY = 0;
    public static final int TYPE_SERVER_LOGS = 1;
    public static final int TYPE_CONFIGURATION_SUMMARY = 2;

    private List<ServerLogsEntry> mServerLogEntries = new ArrayList<>();
    private SpaceCalculateTask mAsyncTask = null;
    private long mConfigurationSize = 0L;
    private int mSecondaryTextColor;

    public StorageSettingsAdapter(Context context) {
        refreshServerLogs(context);

        mSecondaryTextColor = StyledAttributesHelper.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);
    }

    private void refreshServerLogs(Context context) {
        if (mAsyncTask != null)
            return;
        mServerLogEntries.clear();
        mAsyncTask = new SpaceCalculateTask(context, this);
        mAsyncTask.execute();
    }

    private void addEntry(ServerLogsEntry entry) {
        int index = mServerLogEntries.size();
        if (entry.size > 0) {
            int ec = mServerLogEntries.size();
            for (int i = 0; i < ec; i++) {
                if (entry.size > mServerLogEntries.get(i).size) {
                    index = i;
                    break;
                }
            }
        }
        mServerLogEntries.add(index, entry);
        notifyItemInserted(1 + index);
        notifyItemChanged(0);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_SERVER_LOGS_SUMMARY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_storage_chat_logs_summary, parent, false);
            return new ServerLogsSummaryHolder(view);
        } else if (viewType == TYPE_SERVER_LOGS) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_storage_chat_logs_entry, parent, false);
            return new ServerLogsHolder(view);
        } else if (viewType == TYPE_CONFIGURATION_SUMMARY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_storage_configuration_summary, parent, false);
            return new ConfigurationSummaryHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int type = holder.getItemViewType();
        if (type == TYPE_SERVER_LOGS_SUMMARY) {
            ((ServerLogsSummaryHolder) holder).bind();
        } else if (type == TYPE_SERVER_LOGS) {
            ((ServerLogsHolder) holder).bind(mServerLogEntries.get(position - 1), position - 1);
        } else if (type == TYPE_CONFIGURATION_SUMMARY) {
            ((ConfigurationSummaryHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        return 1 + mServerLogEntries.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_SERVER_LOGS_SUMMARY;
        if (position >= 1 && position < 1 + mServerLogEntries.size())
            return TYPE_SERVER_LOGS;
        if (position == 1 + mServerLogEntries.size())
            return TYPE_CONFIGURATION_SUMMARY;
        return -1;
    }

    public class ServerLogsSummaryHolder extends RecyclerView.ViewHolder {

        private SimpleBarChart mChart;
        private TextView mTotal;

        public ServerLogsSummaryHolder(View view) {
            super(view);
            mChart = view.findViewById(R.id.chart);
            mTotal = view.findViewById(R.id.total_value);
            view.findViewById(R.id.set_limits).setOnClickListener((View v) -> {
                StorageLimitsDialog dialog = new StorageLimitsDialog(v.getContext());
                dialog.setOnDismissListener((DialogInterface di) -> {
                    ChatLogStorageManager.getInstance(v.getContext()).requestUpdate(null, () -> {
                        v.post(() -> refreshServerLogs(v.getContext()));
                    });
                });
                dialog.show();
            });
            view.findViewById(R.id.clear_chat_logs).setOnClickListener((View v) -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.pref_storage_clear_all_chat_logs)
                        .setMessage(R.string.pref_storage_clear_all_chat_logs_confirm)
                        .setPositiveButton(R.string.action_delete, (DialogInterface di, int i) -> {
                            new RemoveDataTask(v.getContext(), false, null).execute();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            });
        }

        public void bind() {
            int count = Math.min(4, mServerLogEntries.size());
            long total = 0;
            if (count > 0) {
                float[] values = new float[count];
                int[] colors = new int[count];
                colors[0] = mChart.getResources().getColor(R.color.storageSettingsChartFirst);
                if (count > 1)
                    colors[1] = mChart.getResources().getColor(R.color.storageSettingsChartSecond);
                if (count > 2)
                    colors[2] = mChart.getResources().getColor(R.color.storageSettingsChartThird);
                if (count > 3)
                    colors[3] = mChart.getResources().getColor(R.color.storageSettingsChartOthers);
                for (int i = mServerLogEntries.size() - 1; i >= 0; --i) {
                    long val = mServerLogEntries.get(i).size;
                    total += val;
                    values[Math.min(i, count - 1)] += (float) (val / 1024.0 / 1024.0);
                }
                mChart.setData(values, colors);
                mChart.setVisibility(View.VISIBLE);
                itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getResources().getDimensionPixelSize(R.dimen.storage_chat_logs_summary_padding_bottom));
            } else {
                mChart.setVisibility(View.GONE);
                itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getResources().getDimensionPixelSize(R.dimen.storage_chat_logs_summary_padding_bottom_no_items));
            }
            mTotal.setText(formatFileSize(total));
        }

    }

    private static class ServerLogsEntry {

        String name;
        UUID uuid;
        long size;

        public ServerLogsEntry(String name, UUID uuid, long size) {
            this.name = name;
            this.uuid = uuid;
            this.size = size;
        }

    }

    public class ServerLogsHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public ServerLogsHolder(View view) {
            super(view);
            mText = view.findViewById(R.id.text);
            view.setOnClickListener((View v) -> showActionsMenu());
            view.setOnLongClickListener((View v) -> {
                showActionsMenu();
                return true;
            });
        }

        private void showActionsMenu() {
            MenuBottomSheetDialog menu = new MenuBottomSheetDialog(itemView.getContext());
            if (mText.getTag() != null) {
                ServerConfigData server = ServerConfigManager.getInstance(itemView.getContext()).findServer((UUID) mText.getTag());
                if (server != null && server.storageLimit == 0L) {
                    menu.addItem(R.string.pref_storage_set_server_limit, R.drawable.ic_storage, (MenuBottomSheetDialog.Item it) -> {
                        ServerStorageLimitDialog dialog = new ServerStorageLimitDialog(itemView.getContext(), server);
                        dialog.show();
                        return true;
                    });
                } else if (server != null) {
                    ColoredTextBuilder builder = new ColoredTextBuilder();
                    builder.append(mText.getContext().getString(R.string.pref_storage_change_server_limit));
                    builder.setSpan(new ForegroundColorSpan(mSecondaryTextColor));
                    builder.append(" (");
                    if (server.storageLimit == -1L)
                        builder.append(itemView.getContext().getString(R.string.pref_storage_no_limit));
                    else
                        builder.append((server.storageLimit / 1024L / 1024L) + " MB");
                    builder.append(")");
                    menu.addItem(builder.getSpannable(), R.drawable.ic_storage, (MenuBottomSheetDialog.Item it) -> {
                        ServerStorageLimitDialog dialog = new ServerStorageLimitDialog(itemView.getContext(), server);
                        dialog.show();
                        return true;
                    });
                    menu.addItem(R.string.pref_storage_remove_server_limit, 0, (MenuBottomSheetDialog.Item it) -> {
                        server.storageLimit = 0L;
                        try {
                            ServerConfigManager.getInstance(itemView.getContext()).saveServer(server);
                        } catch (IOException ignored) {
                        }
                        return true;
                    });
                }
            }
            menu.addItem(R.string.pref_storage_clear_server_chat_logs, R.drawable.ic_delete, (MenuBottomSheetDialog.Item it) -> {
                new RemoveDataTask(itemView.getContext(), false, (UUID) mText.getTag()).execute();
                return true;
            });
            menu.show();
        }

        public void bind(ServerLogsEntry entry, int pos) {
            int colorId = R.color.storageSettingsChartOthers;
            if (entry.size > 0L) {
                if (pos == 0)
                    colorId = R.color.storageSettingsChartFirst;
                else if (pos == 1)
                    colorId = R.color.storageSettingsChartSecond;
                else if (pos == 2)
                    colorId = R.color.storageSettingsChartThird;
            }
            ColoredTextBuilder builder = new ColoredTextBuilder();
            builder.append(entry.name, new ForegroundColorSpan(mText.getResources().getColor(colorId)));
            builder.append("  ");
            builder.append(formatFileSize(entry.size));
            mText.setText(builder.getSpannable());
            mText.setTag(entry.uuid);
        }

    }

    public class ConfigurationSummaryHolder extends RecyclerView.ViewHolder {

        private TextView mTotal;

        public ConfigurationSummaryHolder(View view) {
            super(view);
            mTotal = view.findViewById(R.id.total_value);
            view.findViewById(R.id.reset).setOnClickListener((View v) -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.pref_storage_reset_configuration)
                        .setMessage(R.string.pref_storage_reset_configuration_confirm)
                        .setPositiveButton(R.string.action_reset, (DialogInterface di, int i) -> {
                            new RemoveDataTask(v.getContext(), true, null).execute();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            });
        }

        public void bind() {
            mTotal.setText(formatFileSize(mConfigurationSize));
        }

    }


    private static class SpaceCalculateTask extends AsyncTask<Void, Object, Void> {

        private WeakReference<StorageSettingsAdapter> mAdapter;
        private ServerConfigManager mServerManager;
        private File mDataDir;
        private StatFs mStatFs;

        public SpaceCalculateTask(Context context, StorageSettingsAdapter adapter) {
            mServerManager = ServerConfigManager.getInstance(context);
            mDataDir = new File(context.getApplicationInfo().dataDir);
            mAdapter = new WeakReference<>(adapter);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            long dataBlockSize = getBlockSize(mDataDir);
            long dataSize = 0L;
            for (File file : mDataDir.listFiles()) {
                if (file.getName().equals("cache") || file.getName().equals("lib"))
                    continue;
                dataSize += calculateDirectorySize(file, dataBlockSize);
            }
            publishProgress(dataSize);
            List<File> processedDirs = new ArrayList<>();
            for (ServerConfigData data : mServerManager.getServers()) {
                if (mAdapter.get() == null)
                    return null;
                File file = mServerManager.getServerChatLogDir(data.uuid);
                processedDirs.add(file);
                if (!file.exists())
                    continue;
                long size = calculateDirectorySize(file, getBlockSize(file));
                if (size == 0L)
                    continue;
                publishProgress(new ServerLogsEntry(data.name, data.uuid, size));
            }
            File[] files = mServerManager.getChatLogDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (processedDirs.contains(file))
                        continue;
                    long size = calculateDirectorySize(file, getBlockSize(file));
                    if (size == 0L)
                        continue;
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(file.getName());
                    } catch (IllegalArgumentException ignored) {
                    }
                    publishProgress(new ServerLogsEntry(file.getName(), uuid, size));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            StorageSettingsAdapter adapter = mAdapter.get();
            if (adapter != null)
                adapter.mAsyncTask = null;
        }

        private long getBlockSize(File file) {
            if (mStatFs != null)
                mStatFs.restat(file.getAbsolutePath());
            else
                mStatFs = new StatFs(file.getAbsolutePath());
            if (Build.VERSION.SDK_INT >= 18)
                return mStatFs.getBlockSizeLong();
            else
                return mStatFs.getBlockSize();
        }

        private long calculateDirectorySize(File file, long blockSize) {
            File[] files = file.listFiles();
            if (files == null)
                return 0L;
            long ret = blockSize;
            for (File subfile : files) {
                if (subfile.isDirectory())
                    ret += calculateDirectorySize(subfile, blockSize);
                else
                    ret += (subfile.length() + blockSize - 1) / blockSize * blockSize;
            }
            return ret;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            StorageSettingsAdapter adapter = mAdapter.get();
            if (adapter == null)
                return;
            for (Object value : values) {
                if (value instanceof ServerLogsEntry) {
                    adapter.addEntry((ServerLogsEntry) value);
                } else if (value instanceof Long) {
                    adapter.mConfigurationSize = (Long) value;
                    adapter.notifyItemChanged(adapter.getItemCount() - 1);
                }
            }
        }

    }

    private class RemoveDataTask extends AsyncTask<Void, Void, Void> {

        private Context mContext;
        private AlertDialog mAlertDialog;
        private boolean mDeleteConfig;
        private UUID mDeleteServerLogs;

        public RemoveDataTask(Context context, boolean deleteConfig, UUID deleteOnlyServerLogs) {
            mContext = context;
            mDeleteConfig = deleteConfig;
            mDeleteServerLogs = deleteOnlyServerLogs;
            mAlertDialog = new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setView(R.layout.dialog_please_wait)
                    .show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context ctx = mContext;
            if (mDeleteConfig) {
                ServerConnectionManager mgr = ServerConnectionManager.getInstance(null);
                if (mgr != null)
                    mgr.disconnectAndRemoveAllConnections(true);
                else
                    new File(ctx.getFilesDir(),
                            ServerConnectionManager.CONNECTED_SERVERS_FILE_PATH).delete();
                ServerConfigManager.getInstance(ctx).deleteAllServers(true);
                NotificationRuleManager.getUserRules(ctx).clear();
                CommandAliasManager.getInstance(ctx).getUserAliases().clear();
                SettingsHelper.getInstance(ctx).clear();
                NotificationCountStorage.getInstance(ctx).close();

                File files = ctx.getFilesDir();
                for (File file : files.listFiles()) {
                    if (file.getName().equals("cache") || file.getName().equals("lib"))
                        continue;
                    deleteRecursive(file);
                }

                NotificationCountStorage.getInstance(ctx).open();
            }
            if (mDeleteServerLogs != null) {
                deleteChatLogDir(mDeleteServerLogs);
            } else {
                File[] logFiles = ServerConfigManager.getInstance(mContext).getChatLogDir().listFiles();
                if (logFiles == null)
                    logFiles = new File[0];
                for (File file : logFiles) {
                    try {
                        deleteChatLogDir(UUID.fromString(file.getName()));
                    } catch (IllegalArgumentException ignored) {
                        deleteRecursive(file);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAlertDialog.dismiss();
            refreshServerLogs(mContext);
        }

        private void deleteChatLogDir(UUID uuid) {
            ServerConnectionManager connectionManager = ServerConnectionManager.getInstance(null);
            if (connectionManager != null)
                connectionManager.killDisconnectingConnection(uuid);
            ServerConnectionInfo connection = connectionManager != null ? connectionManager.getConnection(uuid) : null;
            SQLiteMessageStorageApi storageApi = null;
            if (connection != null && connection.getApiInstance() != null &&
                    connection.getApiInstance() instanceof ServerConnectionApi &&
                    connection.getApiInstance().getMessageStorageApi() != null &&
                    connection.getApiInstance().getMessageStorageApi() instanceof SQLiteMessageStorageApi) {
                storageApi = (SQLiteMessageStorageApi) connection.getApiInstance().getMessageStorageApi();
                storageApi.close();
                ((ServerConnectionApi) connection.getApiInstance()).getServerConnectionData().setMessageStorageApi(new StubMessageStorageApi());
            }
            File file = ServerConfigManager.getInstance(mContext).getServerChatLogDir(uuid);
            deleteRecursive(file);
            if (storageApi != null) {
                storageApi.open();
                ((ServerConnectionApi) connection.getApiInstance()).getServerConnectionData().setMessageStorageApi(storageApi);
            }
        }

        private void deleteRecursive(File file) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File subfile : files)
                        deleteRecursive(subfile);
                }
            }
            file.delete();
        }

    }

    private static String formatFileSize(long size) {
        if (size / 1024L >= 128)
            return String.format("%.2f MB", size / 1024.0 / 1024.0);
        else
            return String.format("%.2f KB", size / 1024.0);
    }

}
