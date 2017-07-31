package io.mrarm.irc;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StatFs;
import android.support.v7.widget.RecyclerView;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.view.SimpleBarChart;

public class StorageSettingsAdapter extends RecyclerView.Adapter {

    public static final int TYPE_SERVER_LOGS_SUMMARY = 0;
    public static final int TYPE_SERVER_LOGS = 1;
    public static final int TYPE_CONFIGURATION_SUMMARY = 2;

    private List<ServerLogsEntry> mServerLogEntries = new ArrayList<>();
    private SpaceCalculateTask mAsyncTask = null;
    private long mConfigurationSize = 0L;

    public StorageSettingsAdapter(Context context) {
        refreshServerLogs(context);
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
        }

        public void bind() {
            int count = Math.max(1, Math.min(4, mServerLogEntries.size()));
            float[] values = new float[count];
            int[] colors = new int[count];
            colors[0] = mChart.getResources().getColor(R.color.storageSettingsChartFirst);
            if (count > 1)
                colors[1] = mChart.getResources().getColor(R.color.storageSettingsChartSecond);
            if (count > 2)
                colors[2] = mChart.getResources().getColor(R.color.storageSettingsChartThird);
            if (count > 3)
                colors[3] = mChart.getResources().getColor(R.color.storageSettingsChartOthers);
            long total = 0;
            for (int i = mServerLogEntries.size() - 1; i >= 0; --i) {
                long val = mServerLogEntries.get(i).size;
                total += val;
                values[Math.min(i, count - 1)] += (float) (val / 1024.0 / 1024.0);
            }
            mChart.setData(values, colors);
            mTotal.setText(formatFileSize(total));
        }

    }

    private static class ServerLogsEntry {

        String name;
        long size;

        public ServerLogsEntry(String name, long size) {
            this.name = name;
            this.size = size;
        }

    }

    public static class ServerLogsHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public ServerLogsHolder(View view) {
            super(view);
            mText = view.findViewById(R.id.text);
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
        }

    }

    public class ConfigurationSummaryHolder extends RecyclerView.ViewHolder {

        private TextView mTotal;

        public ConfigurationSummaryHolder(View view) {
            super(view);
            mTotal = view.findViewById(R.id.total_value);
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
            for (ServerConfigData data : mServerManager.getServers()) {
                if (mAdapter.get() == null)
                    return null;
                File file = mServerManager.getServerChatLogDir(data.uuid);
                long size = calculateDirectorySize(file, getBlockSize(file));
                if (size == 0L)
                    continue;
                publishProgress(new ServerLogsEntry(data.name, size));
            }
            return null;
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

    private static String formatFileSize(long size) {
        if (size / 1024L >= 128)
            return String.format("%.2f MB", size / 1024.0 / 1024.0);
        else
            return String.format("%.2f KB", size / 1024.0);
    }

}
