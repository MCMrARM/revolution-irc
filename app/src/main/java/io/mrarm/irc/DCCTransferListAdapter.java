package io.mrarm.irc;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;
import io.mrarm.irc.util.AdvancedDividerItemDecoration;
import io.mrarm.irc.util.FormatUtils;

public class DCCTransferListAdapter extends RecyclerView.Adapter implements
        DCCServerManager.UploadListener, DCCManager.DownloadListener, DCCHistory.HistoryListener {

    private static final int TYPE_TRANSFER_ACTIVE = 0;
    private static final int TYPE_TRANSFER_PENDING = 1;
    private static final int TYPE_HISTORY_ENTRY = 2;

    private final Activity mActivity;
    private DCCManager mDCCManager;
    private List<DCCServer.UploadSession> mUploadSessions;
    private List<DCCManager.DownloadInfo> mDownloads;
    private List<DCCServerManager.UploadEntry> mPendingUploads;
    private int mHistoryCount;
    private final List<DCCHistory.Entry> mHistoryUploads = new ArrayList<>();

    public DCCTransferListAdapter(Activity activity) {
        mActivity = activity;
        mDCCManager = DCCManager.getInstance(activity);
        mDCCManager.getServer().addUploadListener(this);
        mDCCManager.addDownloadListener(this);
        mUploadSessions = mDCCManager.getUploadSessions();
        mDownloads = mDCCManager.getDownloads();
        mPendingUploads = mDCCManager.getUploads();
        for (DCCServer.UploadSession session : mUploadSessions) {
            DCCServerManager.UploadEntry ent = mDCCManager.getUploadEntry(session.getServer());
            if (ent == null)
                continue;
            mPendingUploads.remove(ent);
        }
        mHistoryCount = mDCCManager.getHistory().getEntryCount();
        mDCCManager.getHistory().addListener(this);
    }

    public void unregisterListeners() {
        mDCCManager.getServer().removeUploadListener(this);
        mDCCManager.removeDownloadListener(this);
        mDCCManager.getHistory().removeListener(this);
    }

    public ItemDecoration createItemDecoration() {
        return new ItemDecoration(mActivity);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_TRANSFER_ACTIVE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dcc_transfer_active_item, parent, false);
            return new ActiveTransferHolder(view);
        } else if (viewType == TYPE_TRANSFER_PENDING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dcc_transfer_pending_item, parent, false);
            return new PendingTransferHolder(view);
        } else if (viewType == TYPE_HISTORY_ENTRY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dcc_transfer_pending_item, parent, false);
            return new HistoryEntryHolder(view);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= 0 && position < mUploadSessions.size())
            ((ActiveTransferHolder) holder).bind(mUploadSessions.get(position));
        else if (position >= getDownloadsStart() &&
                position < getDownloadsStart() + mDownloads.size())
            ((ActiveTransferHolder) holder).bind(mDownloads.get(position - getDownloadsStart()));
        else if (position >= getPendingUploadsStart() &&
                position < getPendingUploadsStart() + mPendingUploads.size())
            ((PendingTransferHolder) holder).bind(
                    mPendingUploads.get(position - getPendingUploadsStart()));
        else if (position >= getHistoryStart())
            ((HistoryEntryHolder) holder).bind(getHistoryEntry(position - getHistoryStart()));

    }

    @Override
    public int getItemCount() {
        return mUploadSessions.size() + mDownloads.size() + mPendingUploads.size() + mHistoryCount;
    }

    private int getDownloadsStart() {
        return mUploadSessions.size();
    }

    private int getPendingUploadsStart() {
        return mUploadSessions.size() + mDownloads.size();
    }

    public int getHistoryStart() {
        return mUploadSessions.size() + mDownloads.size() + mPendingUploads.size();
    }

    private DCCHistory.Entry getHistoryEntry(int index) {
        if (index >= mHistoryUploads.size()) {
            int limit = ((index + 1 - mHistoryUploads.size() + 10 - 1) / 10) * 10;
            mHistoryUploads.addAll(mDCCManager.getHistory()
                    .getEntries(mHistoryUploads.size(), limit));
        }
        if (index >= mHistoryUploads.size())
            return null;
        return mHistoryUploads.get(index);
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= getPendingUploadsStart() &&
                position < getPendingUploadsStart() + mPendingUploads.size())
            return TYPE_TRANSFER_PENDING;
        if (position >= getHistoryStart())
            return TYPE_HISTORY_ENTRY;
        return TYPE_TRANSFER_ACTIVE;
    }

    public void updateActiveProgress(RecyclerView recyclerView) {
        int cnt = recyclerView.getChildCount();
        for (int i = 0; i < cnt; i++) {
            View view = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
            if (holder instanceof ActiveTransferHolder)
                ((ActiveTransferHolder) holder).updateProgress();
        }
    }

    private void addPendingUpload(DCCServerManager.UploadEntry entry) {
        if (entry == null || mPendingUploads.contains(entry))
            return;
        mPendingUploads.add(entry);
        notifyItemInserted(getPendingUploadsStart() + mPendingUploads.size() - 1);
    }

    public void removePendingUpload(DCCServerManager.UploadEntry entry) {
        int idx = mPendingUploads.indexOf(entry);
        if (idx == -1)
            return;
        mPendingUploads.remove(idx);
        notifyItemRemoved(getPendingUploadsStart() + idx);
    }

    @Override
    public void onUploadCreated(DCCServerManager.UploadEntry uploadEntry) {
        mActivity.runOnUiThread(() -> {
            addPendingUpload(uploadEntry);
        });
    }

    @Override
    public void onUploadDestroyed(DCCServerManager.UploadEntry uploadEntry) {
        mActivity.runOnUiThread(() -> {
            removePendingUpload(uploadEntry);
        });
    }

    @Override
    public void onSessionCreated(DCCServer dccServer, DCCServer.UploadSession uploadSession) {
        mActivity.runOnUiThread(() -> {
            if (!mUploadSessions.contains(uploadSession)) {
                mUploadSessions.add(uploadSession);
                notifyItemInserted(mUploadSessions.size() - 1);
            }
            removePendingUpload(mDCCManager.getUploadEntry(uploadSession.getServer()));
        });
    }

    @Override
    public void onSessionDestroyed(DCCServer dccServer, DCCServer.UploadSession uploadSession) {
        mActivity.runOnUiThread(() -> {
            int idx = mUploadSessions.indexOf(uploadSession);
            if (idx == -1)
                return;
            mUploadSessions.remove(idx);
            notifyItemRemoved(idx);
            for (DCCServer.UploadSession ent : mUploadSessions) {
                if (ent.getServer() == uploadSession.getServer())
                    return;
            }
            addPendingUpload(mDCCManager.getUploadEntry(uploadSession.getServer()));
        });
    }

    @Override
    public void onDownloadCreated(DCCManager.DownloadInfo download) {
        mActivity.runOnUiThread(() -> {
            if (!mDownloads.contains(download)) {
                mDownloads.add(download);
                notifyItemInserted(getDownloadsStart() + mDownloads.size() - 1);
            }
        });
    }

    @Override
    public void onDownloadDestroyed(DCCManager.DownloadInfo download) {
        mActivity.runOnUiThread(() -> {
            int idx = mDownloads.indexOf(download);
            if (idx == -1)
                return;
            mDownloads.remove(idx);
            notifyItemRemoved(getDownloadsStart() + idx);
        });
    }

    @Override
    public void onDownloadUpdated(DCCManager.DownloadInfo download) {
        mActivity.runOnUiThread(() -> {
            int idx = mDownloads.indexOf(download);
            if (idx != -1)
                notifyItemChanged(getDownloadsStart() + idx);
        });
    }

    @Override
    public void onHistoryEntryCreated(DCCHistory.Entry entry) {
        mActivity.runOnUiThread(() -> {
            mHistoryCount++;
            mHistoryUploads.add(0, entry);
            notifyItemInserted(getDownloadsStart());
        });
    }

    @Override
    public void onHistoryEntryRemoved(long entryId) {
        mActivity.runOnUiThread(() -> {
            for (int i = 0; i < mHistoryUploads.size(); i++) {
                if (mHistoryUploads.get(i).entryId == entryId) {
                    mHistoryUploads.remove(i);
                    mHistoryCount--;
                    notifyItemRemoved(getDownloadsStart() + i);
                    return;
                }
            }
        });
    }

    public static class ItemDecoration extends AdvancedDividerItemDecoration {

        public ItemDecoration(Context context) {
            super(context);
        }

        @Override
        public boolean hasDivider(RecyclerView parent, View view) {
            return true;
        }

    }

    public static final class ActiveTransferHolder extends RecyclerView.ViewHolder {

        private TextView mName;
        private ProgressBar mProgressBar;
        private ImageView mStatusIcon;
        private TextView mStatus;
        private DCCManager.DownloadInfo mDownload;
        private DCCServer.UploadSession mSession;

        public ActiveTransferHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mProgressBar = itemView.findViewById(R.id.progress);
            mStatusIcon = itemView.findViewById(R.id.status_icon);
            mStatus = itemView.findViewById(R.id.status);
            itemView.setOnClickListener((View v) -> openMenu());
            itemView.setOnLongClickListener((View v) -> { openMenu(); return true; });
        }

        public void openMenu() {
            MenuBottomSheetDialog dialog = new MenuBottomSheetDialog(itemView.getContext());
            dialog.addItem(R.string.action_cancel, R.drawable.ic_close,
                    (MenuBottomSheetDialog.Item item) -> {
                        requestCancel();
                        return true;
                    });
            dialog.show();
        }

        private void requestCancel() {
            if (mDownload != null)
                mDownload.cancel();
            if (mSession != null) {
                DCCManager manager = DCCManager.getInstance(itemView.getContext());
                manager.getServer().cancelUpload(manager.getUploadEntry(mSession.getServer()));
            }
        }

        public void bind(DCCManager.DownloadInfo download) {
            mDownload = download;
            mSession = null;
            mStatusIcon.setImageResource(R.drawable.ic_file_download_white_16dp);
            mName.setText(download.getUnescapedFileName());
            updateProgress();
        }

        public void bind(DCCServer.UploadSession session) {
            mDownload = null;
            mSession = session;
            mStatusIcon.setImageResource(R.drawable.ic_file_upload_white_16dp);
            mName.setText(DCCManager.getInstance(mName.getContext())
                    .getUploadName(session.getServer()));
            updateProgress();
            InetSocketAddress addr = (InetSocketAddress) session.getRemoteAddress();
            mStatus.setText(mStatus.getContext().getString(
                    R.string.dcc_active_upload_transfer_status,
                    addr.getAddress().getHostAddress() + ":" + addr.getPort()));
        }

        public void updateProgress() {
            if (mDownload != null) {
                if (mDownload.isPending()) {
                    mProgressBar.setIndeterminate(true);
                    mStatus.setText(mStatus.getContext().getString(
                            R.string.dcc_active_waiting_for_approval));
                } else if (mDownload.getClient() != null) {
                    mProgressBar.setIndeterminate(false);
                    mProgressBar.setMax(100000);
                    mProgressBar.setProgress((int)
                            (mDownload.getClient().getDownloadedSize() * 100000L /
                                    mDownload.getClient().getExpectedSize()));

                    InetSocketAddress addr = (InetSocketAddress) mDownload.getClient()
                            .getRemoteAddress();
                    mStatus.setText(mStatus.getContext().getString(
                            R.string.dcc_active_download_transfer_status,
                            addr.getAddress().getHostAddress() + ":" + addr.getPort()));
                } else {
                    mProgressBar.setIndeterminate(true);
                    mStatus.setText(mStatus.getContext().getString(
                            R.string.dcc_active_waiting_for_connection));
                }
            }
            if (mSession != null) {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setMax(100000);
                mProgressBar.setProgress((int)
                        (mSession.getAcknowledgedSize() * 100000L / mSession.getTotalSize()));
            }
        }

    }

    public static final class PendingTransferHolder extends RecyclerView.ViewHolder {

        private TextView mName;
        private TextView mStatus;
        private DCCServerManager.UploadEntry mEntry;

        public PendingTransferHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mStatus = itemView.findViewById(R.id.status);
            itemView.setOnClickListener((View v) -> openMenu());
            itemView.setOnLongClickListener((View v) -> { openMenu(); return true; });
        }

        public void openMenu() {
            MenuBottomSheetDialog dialog = new MenuBottomSheetDialog(itemView.getContext());
            dialog.addItem(R.string.action_cancel, R.drawable.ic_close,
                    (MenuBottomSheetDialog.Item item) -> {
                        requestCancel();
                        return true;
                    });
            dialog.show();
        }

        private void requestCancel() {
            DCCManager manager = DCCManager.getInstance(itemView.getContext());
            manager.getServer().cancelUpload(mEntry);
        }

        public void bind(DCCServerManager.UploadEntry entry) {
            mEntry = entry;
            mName.setText(DCCManager.getInstance(mName.getContext())
                    .getUploadName(entry.getServer()));
            mStatus.setText(mStatus.getContext().getString(
                    R.string.dcc_active_waiting_for_connection));
        }

    }

    public static final class HistoryEntryHolder extends RecyclerView.ViewHolder {

        private TextView mName;
        private TextView mStatus;
        private ImageView mStatusIcon;
        private String mFileUri;
        private long mEntryId;

        public HistoryEntryHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mStatus = itemView.findViewById(R.id.status);
            mStatusIcon = itemView.findViewById(R.id.status_icon);
            itemView.setOnClickListener((View v) -> open());
            itemView.setOnLongClickListener((View v) -> { openMenu(); return true; });
        }

        public void open() {
            if (mFileUri != null && mFileUri.length() > 0) {
                Uri uri = Uri.parse(mFileUri);
                if (uri.getScheme().equals("file"))
                    uri = FileProvider.getUriForFile(itemView.getContext(),
                            "io.mrarm.irc.fileprovider", new File(uri.getPath()));
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(uri, mimeType);
                try {
                    itemView.getContext().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(itemView.getContext(), R.string.dcc_error_no_activity,
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                openMenu();
            }
        }

        public void openMenu() {
            MenuBottomSheetDialog dialog = new MenuBottomSheetDialog(itemView.getContext());
            if (mFileUri != null && mFileUri.length() > 0) {
                dialog.addItem(R.string.action_open, R.drawable.ic_open_in_new,
                        (MenuBottomSheetDialog.Item item) -> {
                            open();
                            return true;
                        });
            }
            dialog.addItem(R.string.action_delete, R.drawable.ic_delete,
                    (MenuBottomSheetDialog.Item item) -> {
                        delete();
                        return true;
                    });
            dialog.show();
        }

        private void delete() {
            if (mFileUri != null && mFileUri.length() > 0) {
                Uri uri = Uri.parse(mFileUri);
                DocumentFile file;
                if (uri.getScheme().equals("file"))
                    file = DocumentFile.fromFile(new File(uri.getPath()));
                else
                    file = DocumentFile.fromSingleUri(itemView.getContext(), uri);
                if (file.exists())
                    file.delete();
            }
            DCCManager.getInstance(itemView.getContext()).getHistory().removeEntry(mEntryId);
        }

        public void bind(DCCHistory.Entry entry) {
            mEntryId = entry.entryId;
            mFileUri = entry.fileUri;
            mName.setText(entry.fileName);
            if (entry.entryType == DCCHistory.TYPE_DOWNLOAD) {
                mStatusIcon.setImageResource(R.drawable.ic_file_download_white_16dp);
                if (entry.fileUri != null && entry.remoteAddress != null)
                    mStatus.setText(itemView.getResources().getString(R.string.dcc_history_download,
                            entry.userNick, FormatUtils.formatByteSize(entry.fileSize),
                            entry.remoteAddress));
                else
                    mStatus.setText(itemView.getResources().getString(
                            R.string.dcc_history_download_failed,
                            entry.userNick, FormatUtils.formatByteSize(entry.fileSize)));
            } else {
                mStatusIcon.setImageResource(R.drawable.ic_file_upload_white_16dp);
                mStatus.setText(itemView.getResources().getString(R.string.dcc_history_upload,
                        entry.userNick, FormatUtils.formatByteSize(entry.fileSize),
                        entry.remoteAddress));
            }
        }

    }
}
