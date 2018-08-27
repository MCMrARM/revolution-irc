package io.mrarm.irc;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.InetSocketAddress;
import java.util.List;

import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;
import io.mrarm.irc.util.AdvancedDividerItemDecoration;

public class DCCTransferListAdapter extends RecyclerView.Adapter implements
        DCCServerManager.UploadListener, DCCManager.DownloadListener {

    private static final int TYPE_TRANSFER_ACTIVE = 0;
    private static final int TYPE_TRANSFER_PENDING = 1;

    private Activity mActivity;
    private DCCManager mDCCManager;
    private List<DCCServer.UploadSession> mUploadSessions;
    private List<DCCManager.DownloadInfo> mDownloads;
    private List<DCCServerManager.UploadEntry> mPendingUploads;

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
    }

    public void unregisterListeners() {
        mDCCManager.getServer().removeUploadListener(this);
        mDCCManager.removeDownloadListener(this);
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
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= 0 && position < mUploadSessions.size())
            ((ActiveTransferHolder) holder).bind(mUploadSessions.get(position));
        if (position >= getDownloadsStart() &&
                position < getDownloadsStart() + mDownloads.size())
            ((ActiveTransferHolder) holder).bind(mDownloads.get(position - getDownloadsStart()));
        if (position >= getPendingUploadsStart() &&
                position < getPendingUploadsStart() + mPendingUploads.size())
            ((PendingTransferHolder) holder).bind(
                    mPendingUploads.get(position - getPendingUploadsStart()));
    }

    @Override
    public int getItemCount() {
        return mUploadSessions.size() + mDownloads.size() + mPendingUploads.size();
    }

    private int getDownloadsStart() {
        return mUploadSessions.size();
    }

    private int getPendingUploadsStart() {
        return mUploadSessions.size() + mDownloads.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= getPendingUploadsStart() &&
                position < getPendingUploadsStart() + mPendingUploads.size())
            return TYPE_TRANSFER_PENDING;
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
            mStatusIcon.setImageResource(R.drawable.ic_file_download_white_24dp);
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
}
