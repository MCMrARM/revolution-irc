package io.mrarm.irc;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.config.ServerCertificateManager;

public class CertificateManagerActivity extends ThemedActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";

    private ServerCertificateManager mHelper;
    private List<String> mAliases;
    private CertificateListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate_manager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String serverUUID = getIntent().getStringExtra(ARG_SERVER_UUID);
        mHelper = ServerCertificateManager.get(this, UUID.fromString(serverUUID));
        mAliases = mHelper.getCertificateAliases();

        if (mAliases == null || mAliases.size() == 0) {
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mAdapter = new CertificateListAdapter();
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class CertificateListAdapter extends RecyclerView.Adapter<CertificateHolder> {

        @Override
        public CertificateHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.certificate_manager_item, parent, false);
            return new CertificateHolder(v);
        }

        @Override
        public void onBindViewHolder(CertificateHolder holder, int position) {
            holder.bind(mAliases.get(position));
        }

        @Override
        public int getItemCount() {
            return mAliases.size();
        }

    }

    public class CertificateHolder extends RecyclerView.ViewHolder {

        private TextView mCertificateText;

        public CertificateHolder(View itemView) {
            super(itemView);
            mCertificateText = itemView.findViewById(R.id.certificate);
            itemView.findViewById(R.id.delete).setOnClickListener((View v) -> {
                mHelper.removeCertificate(mAliases.get(getAdapterPosition()));
                mAliases.remove(getAdapterPosition());
                mAdapter.notifyItemRemoved(getAdapterPosition());
            });
        }

        public void bind(String alias) {
            X509Certificate cert = mHelper.getCertificate(alias);
            mCertificateText.setText(ServerCertificateManager.buildCertOverviewString(cert));
        }

    }


}
