package io.mrarm.irc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.InetSocketAddress;
import java.util.List;

import io.mrarm.chatlib.irc.dcc.DCCServer;
import io.mrarm.chatlib.irc.dcc.DCCServerManager;

public class DCCTransferListAdapter extends RecyclerView.Adapter implements DCCServerManager.UploadListener {

    private static final int TYPE_TRANSFER_ACTIVE = 0;

    private List<DCCServer.UploadSession> mUploadSessions;

    public DCCTransferListAdapter(Context context) {
        DCCManager.getInstance(context).getServer().addUploadListener(this);
        mUploadSessions = DCCManager.getInstance(context).getSessions();
    }

    public void unregisterListeners() {
        DCCManager.getInstance(null).getServer().removeUploadListener(this);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dcc_transfer_active_item, parent, false);
        return new ActiveTransferHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ActiveTransferHolder) holder).bind(mUploadSessions.get(position));
    }

    @Override
    public synchronized int getItemCount() {
        return mUploadSessions.size();
    }

    @Override
    public int getItemViewType(int position) {
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

    @Override
    public void onUploadCreated(DCCServerManager.UploadEntry uploadEntry) {
    }

    @Override
    public void onUploadDestroyed(DCCServerManager.UploadEntry uploadEntry) {
    }

    @Override
    public void onSessionCreated(DCCServer dccServer, DCCServer.UploadSession uploadSession) {
        synchronized (this) {
            if (!mUploadSessions.contains(uploadSession)) {
                mUploadSessions.add(uploadSession);
                notifyItemInserted(mUploadSessions.size() - 1);
            }
        }
    }

    @Override
    public void onSessionDestroyed(DCCServer dccServer, DCCServer.UploadSession uploadSession) {
        synchronized (this) {
            int idx = mUploadSessions.indexOf(uploadSession);
            if (idx == -1)
                return;
            mUploadSessions.remove(idx);
            notifyItemRemoved(idx);
        }
    }


    public static final class ActiveTransferHolder extends RecyclerView.ViewHolder {

        private TextView mName;
        private ProgressBar mProgressBar;
        private TextView mStatus;
        private DCCServer.UploadSession mSession;

        public ActiveTransferHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mProgressBar = itemView.findViewById(R.id.progress);
            mStatus = itemView.findViewById(R.id.status);
        }

        public void bind(DCCServer.UploadSession session) {
            mSession = session;
            mName.setText(DCCManager.getInstance(mName.getContext())
                    .getUploadName(session.getServer()));
            updateProgress();
            InetSocketAddress addr = (InetSocketAddress) session.getRemoteAddress();
            mStatus.setText(mStatus.getContext().getString(
                    R.string.dcc_active_upload_transfer_status,
                    addr.getAddress().getHostAddress() + ":" + addr.getPort()));
        }

        public void updateProgress() {
            mProgressBar.setMax(100000);
            mProgressBar.setProgress((int)
                    (mSession.getAcknowledgedSize() * 100000L / mSession.getTotalSize()));
        }

    }
}
