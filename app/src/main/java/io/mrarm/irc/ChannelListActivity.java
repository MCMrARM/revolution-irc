package io.mrarm.irc;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.ChannelList;

public class ChannelListActivity extends AppCompatActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";

    private ServerConnectionInfo mConnection;
    private View mMainAppBar;
    private View mSearchAppBar;
    private SearchView mSearchView;
    private ListAdapter mListAdapter;
    private List<ChannelList.Entry> mEntries = new ArrayList<>();
    private String mFilterQuery;
    private List<ChannelList.Entry> mFilteredEntries = null;
    private final List<ChannelList.Entry> mAppendEntries = new ArrayList<>();
    private FilterAsyncTask mFilterAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Toolbar searchToolbar = (Toolbar) findViewById(R.id.search_toolbar);
        searchToolbar.setNavigationOnClickListener((View v) -> {
            setSearchMode(false);
        });

        mMainAppBar = findViewById(R.id.appbar);
        mSearchAppBar = findViewById(R.id.search_appbar);
        mSearchView = (SearchView) findViewById(R.id.search_view);

        UUID serverUUID = UUID.fromString(getIntent().getStringExtra(ARG_SERVER_UUID));
        mConnection = ServerConnectionManager.getInstance(this).getConnection(serverUUID);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mListAdapter = new ListAdapter();
        recyclerView.setAdapter(mListAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mFilterQuery = newText.toLowerCase();
                if (mFilterQuery.isEmpty()) {
                    mFilterQuery = null;
                    mFilteredEntries = null;
                    mListAdapter.notifyDataSetChanged();
                } else if (mFilterAsyncTask == null) {
                    mFilterAsyncTask = new FilterAsyncTask(mFilterQuery);
                    mFilterAsyncTask.execute();
                }
                return true;
            }
        });

        mConnection.getApiInstance().listChannels((ChannelList list) -> {
            runOnUiThread(() -> {
                mEntries = list.getEntries();
                if (mFilterAsyncTask == null && mFilterQuery != null) {
                    mFilterAsyncTask = new FilterAsyncTask(mFilterQuery);
                    mFilterAsyncTask.execute();
                }
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

            if (mFilterQuery == null) {
                mListAdapter.notifyDataSetChanged();
            } else if (mFilterAsyncTask == null) {
                mFilterAsyncTask = new FilterAsyncTask(mFilterQuery);
                mFilterAsyncTask.execute();
            }
        }
    };

    private boolean filterEntry(ChannelList.Entry entry, String query) {
        return mFilterQuery == null || entry.getChannel().toLowerCase().contains(query);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            finish();
            return true;
        } else if (id == R.id.action_search) {
            setSearchMode(true);
            mSearchView.setIconified(false); // This will cause the search view to be focused and show the keyboard
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mSearchAppBar.getVisibility() == View.VISIBLE) {
            setSearchMode(false);
            return;
        }
        super.onBackPressed();
    }

    public void setSearchMode(boolean searchMode) {
        mMainAppBar.setVisibility(searchMode ? View.GONE : View.VISIBLE);
        mSearchAppBar.setVisibility(searchMode ? View.VISIBLE : View.GONE);

        if (Build.VERSION.SDK_INT >= 21) {
            if (searchMode)
                getWindow().setStatusBarColor(getResources().getColor(R.color.searchColorPrimaryDark));
            else
                getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 23) {
            if (searchMode)
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            else
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (!searchMode) {
            mFilterQuery = null;
            mFilteredEntries = null;
            mSearchView.setQuery(null, false);
            mListAdapter.notifyDataSetChanged();
        }
    }

    public class ListAdapter extends RecyclerView.Adapter<ListEntry> {

        @Override
        public ListEntry onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.channel_list_item, parent, false);
            return new ListEntry(view);
        }

        @Override
        public void onBindViewHolder(ListEntry holder, int position) {
            holder.bind(mFilteredEntries == null ? mEntries.get(position)
                    : mFilteredEntries.get(position));
        }

        @Override
        public int getItemCount() {
            return mFilteredEntries == null ? mEntries.size() : mFilteredEntries.size();
        }

    }

    public class ListEntry extends RecyclerView.ViewHolder {

        private TextView mName;
        private TextView mTopic;

        public ListEntry(View itemView) {
            super(itemView);
            mName = (TextView) itemView.findViewById(R.id.name);
            mTopic = (TextView) itemView.findViewById(R.id.topic);
            itemView.setOnClickListener((View view) -> {
                List<String> channels = new ArrayList<>();
                channels.add((String) mName.getTag());
                mConnection.getApiInstance().joinChannels(channels, (Void v) -> {
                    runOnUiThread(() -> {
                        startActivity(MainActivity.getLaunchIntent(ChannelListActivity.this,
                                mConnection, channels.get(0)));
                    });
                }, null);
            });
        }

        public void bind(ChannelList.Entry entry) {
            mName.setText(mName.getResources().getQuantityString(
                    R.plurals.channel_list_title_with_member_count, entry.getMemberCount(),
                    entry.getChannel(), entry.getMemberCount()));
            mName.setTag(entry.getChannel());
            mTopic.setText(entry.getTopic().trim());
            mTopic.setVisibility(mTopic.getText().length() > 0 ? View.VISIBLE : View.GONE);
        }

    }

    private class FilterAsyncTask extends AsyncTask<Void, Void, List<ChannelList.Entry>> {

        private String mQueryString;

        public FilterAsyncTask(String queryString) {
            mQueryString = queryString;
        }

        @Override
        protected List<ChannelList.Entry> doInBackground(Void... voids) {
            List<ChannelList.Entry> ret = new ArrayList<>();
            for (ChannelList.Entry entry : mEntries) {
                if (filterEntry(entry, mQueryString))
                    ret.add(entry);
            }
            return ret;
        }

        @Override
        protected void onPostExecute(List<ChannelList.Entry> aVoid) {
            mFilteredEntries = aVoid;
            mListAdapter.notifyDataSetChanged();
            if (!mQueryString.equals(mFilterQuery)) {
                mFilterAsyncTask = new FilterAsyncTask(mFilterQuery);
                mFilterAsyncTask.execute();
            } else {
                mFilterAsyncTask = null;
            }
        }
    }

}
