package io.mrarm.irc;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

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
