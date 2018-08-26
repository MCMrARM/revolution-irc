package io.mrarm.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class DCCActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private DCCTransferListAdapter mAdapter;
    private DCCManager.ActivityDialogHandler mDCCDialogHandler =
            new DCCManager.ActivityDialogHandler(this);

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

    private void updateActiveProgress() {
        mAdapter.updateActiveProgress(mRecyclerView);
        mRecyclerView.postDelayed(this::updateActiveProgress, 500L);
    }

}
