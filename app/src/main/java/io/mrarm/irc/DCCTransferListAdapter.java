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

public class DCCTransferListAdapter extends RecyclerView.Adapter {

    private static final int TYPE_TRANSFER_ACTIVE = 0;

    private List<DCCServer.UploadSession> mUploadSessions;

    public DCCTransferListAdapter(Context context) {
        mUploadSessions = DCCManager.getInstance(context).getSessions();
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
    public int getItemCount() {
        return mUploadSessions.size();
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_TRANSFER_ACTIVE;
    }


    public static final class ActiveTransferHolder extends RecyclerView.ViewHolder {

        private TextView mName;
        private ProgressBar mProgressBar;
        private TextView mStatus;

        public ActiveTransferHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mProgressBar = itemView.findViewById(R.id.progress);
            mStatus = itemView.findViewById(R.id.status);
        }

        public void bind(DCCServer.UploadSession session) {
            mName.setText(DCCManager.getInstance(mName.getContext())
                    .getUploadName(session.getServer()));
            mProgressBar.setMax(100000);
            mProgressBar.setProgress((int)
                    (session.getAcknowledgedSize() * 100000L / session.getTotalSize()));
            InetSocketAddress addr = (InetSocketAddress) session.getRemoteAddress();
            mStatus.setText(mStatus.getContext().getString(
                    R.string.dcc_active_upload_transfer_status,
                    addr.getAddress().getHostAddress() + ":" + addr.getPort()));
        }

    }
}
