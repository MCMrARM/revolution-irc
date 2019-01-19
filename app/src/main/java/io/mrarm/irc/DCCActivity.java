package io.mrarm.irc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class DCCActivity extends ThemedActivity {

    private static final int REQUEST_CODE_PICK_CUSTOM_DIRECTORY = 3;

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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                                startActivityForResult(intent, REQUEST_CODE_PICK_CUSTOM_DIRECTORY);
                            } else {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("*/*");
                                startActivityForResult(intent, REQUEST_CODE_PICK_CUSTOM_DIRECTORY);
                            }
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
        if (requestCode == REQUEST_CODE_PICK_CUSTOM_DIRECTORY && data != null &&
                data.getData() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getContentResolver().takePersistableUriPermission(data.getData(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            Log.d("DCCActivity", "Picked custom directory: " + data.getData());
            DCCManager dccManager = DCCManager.getInstance(this);
            dccManager.setAlwaysUseApplicationDownloadDirectory(false);
            dccManager.setOverrideDownloadDirectory(data.getData(), false);
            return;
        }
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
