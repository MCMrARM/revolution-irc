package io.mrarm.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.ChannelList;

public class ChannelListActivity extends AppCompatActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";

    private ListAdapter mListAdapter;
    private List<ChannelList.Entry> mEntries = new ArrayList<>();
    private final List<ChannelList.Entry> mAppendEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        UUID serverUUID = UUID.fromString(getIntent().getStringExtra(ARG_SERVER_UUID));
        ServerConnectionInfo connectionInfo = ServerConnectionManager.getInstance(this)
                .getConnection(serverUUID);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mListAdapter = new ListAdapter();
        recyclerView.setAdapter(mListAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        connectionInfo.getApiInstance().listChannels((ChannelList list) -> {
            runOnUiThread(() -> {
                mEntries = list.getEntries();
                mListAdapter.notifyDataSetChanged();
            });
        }, (ChannelList.Entry entry) -> {
            synchronized (mAppendEntries) {
                mAppendEntries.add(entry);
                if (mAppendEntries.size() == 1)
                    runOnUiThread(mAppendRunnable);
            }
        }, null);
    }

    private Runnable mAppendRunnable = () -> {
        synchronized (mAppendEntries) {
            mEntries.addAll(mAppendEntries);
            mAppendEntries.clear();
            mListAdapter.notifyDataSetChanged();
        }
    };

    public class ListAdapter extends RecyclerView.Adapter<ListEntry> {

        @Override
        public ListEntry onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.channel_list_item, parent, false);
            return new ListEntry(view);
        }

        @Override
        public void onBindViewHolder(ListEntry holder, int position) {
            holder.bind(mEntries.get(position));
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
        }

    }

    public static class ListEntry extends RecyclerView.ViewHolder {

        private TextView mName;
        private TextView mTopic;

        public ListEntry(View itemView) {
            super(itemView);
            mName = (TextView) itemView.findViewById(R.id.name);
            mTopic = (TextView) itemView.findViewById(R.id.topic);
        }

        public void bind(ChannelList.Entry entry) {
            mName.setText(mName.getResources().getQuantityString(
                    R.plurals.channel_list_title_with_member_count, entry.getMemberCount(),
                    entry.getChannel(), entry.getMemberCount()));
            mTopic.setText(entry.getTopic().trim());
        }

    }

}
