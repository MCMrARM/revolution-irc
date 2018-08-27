package io.mrarm.irc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

public class DCCActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private DCCTransferListAdapter mAdapter;
    private DCCManager.ActivityDialogHandler mDCCDialogHandler =
            new DCCManager.ActivityDialogHandler(this, 1,
                    2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_list);
        mRecyclerView = findViewById(R.id.items);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DCCTransferListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(mAdapter.createItemDecoration());
        updateActiveProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDCCDialogHandler.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDCCDialogHandler.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.unregisterListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dcc_transfers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.dcc_set_download_dir) {
            boolean hasOverrideURI =
                    DCCManager.getInstance(this).getDownloadDirectoryOverrideURI() != null;
            boolean usesSystemDir = DCCManager.getInstance(this).isSystemDownloadDirectoryUsed();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.dcc_set_download_dir)
                    .setSingleChoiceItems(new CharSequence[] {
                            getString(R.string.dcc_download_dir_application),
                            getString(R.string.dcc_download_dir_system),
                            getString(R.string.dcc_download_dir_custom)
                    }, (usesSystemDir ? 1 : (hasOverrideURI ? 2 : 0)),
                            (DialogInterface i, int which) -> {
                        DCCManager dccManager = DCCManager.getInstance(this);
                        if (which == 0) {
                            dccManager.setAlwaysUseApplicationDownloadDirectory(true);
                        } else if (which == 1) {
                            dccManager.setAlwaysUseApplicationDownloadDirectory(false);
                            mDCCDialogHandler.askSystemDownloadsPermission(null);
                        } else if (which == 2) {
                            // TODO:
                        }
                        i.dismiss();
                    })
                    .setNegativeButton(R.string.action_cancel, (DialogInterface i, int w) -> {})
                    .create();
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mDCCDialogHandler.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mDCCDialogHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void updateActiveProgress() {
        mAdapter.updateActiveProgress(mRecyclerView);
        mRecyclerView.postDelayed(this::updateActiveProgress, 500L);
    }

}
