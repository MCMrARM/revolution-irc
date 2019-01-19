package io.mrarm.irc;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.ChannelList;
import io.mrarm.irc.view.RecyclerViewScrollbar;

public class ChannelListActivity extends ThemedActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";

    public static final int SORT_UNSORTED = 0;
    public static final int SORT_NAME = 1;
    public static final int SORT_MEMBER_COUNT = 2;

    private ServerConnectionInfo mConnection;
    private View mMainAppBar;
    private View mSearchAppBar;
    private SearchView mSearchView;
    private ListAdapter mListAdapter;

    private String mFilterQuery;
    private int mSortMode = SORT_NAME;

    private UpdateListAsyncTask mUpdateListAsyncTask;

    private List<ChannelList.Entry> mEntries = new ArrayList<>();
    private List<ChannelList.Entry> mFilteredEntries = new ArrayList<>();

    private final List<ChannelList.Entry> mAppendEntries = new ArrayList<>();
    private List<ChannelList.Entry> mAssignEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Toolbar searchToolbar = findViewById(R.id.search_toolbar);
        searchToolbar.setNavigationOnClickListener((View v) -> {
            setSearchMode(false);
        });

        mMainAppBar = findViewById(R.id.appbar);
        mSearchAppBar = findViewById(R.id.search_appbar);
        mSearchView = findViewById(R.id.search_view);

        UUID serverUUID = UUID.fromString(getIntent().getStringExtra(ARG_SERVER_UUID));
        mConnection = ServerConnectionManager.getInstance(this).getConnection(serverUUID);

        RecyclerView recyclerView = findViewById(R.id.list);
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
                requestListUpdate();
                return true;
            }
        });

        mConnection.getApiInstance().listChannels((ChannelList list) -> {
            synchronized (mAppendEntries) {
                mAppendEntries.clear();
                mAssignEntries = list.getEntries();
            }
            runOnUiThread(this::requestListUpdate);
        }, (ChannelList.Entry entry) -> {
            synchronized (mAppendEntries) {
                mAppendEntries.add(entry);
            }
            runOnUiThread(this::requestListUpdate);
        }, null);
    }

    private static boolean filterEntry(ChannelList.Entry entry, String query) {
        return query == null || query.length() == 0 ||
                entry.getChannel().toLowerCase().contains(query);
    }

    private void requestListUpdate() {
        if (mUpdateListAsyncTask == null) {
            mUpdateListAsyncTask = new UpdateListAsyncTask(this);
            mUpdateListAsyncTask.execute();
        }
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
        } else if (id == R.id.action_sort_none || id == R.id.action_sort_name || id == R.id.action_sort_member_count) {
            if (id == R.id.action_sort_name)
                mSortMode = SORT_NAME;
            else if (id == R.id.action_sort_member_count)
                mSortMode = SORT_MEMBER_COUNT;
            else
                mSortMode = SORT_UNSORTED;
            requestListUpdate();
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

    public class ListAdapter extends RecyclerView.Adapter<ListEntry>
            implements RecyclerViewScrollbar.LetterAdapter {

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
        public String getLetterFor(int position) {
            if (mSortMode != SORT_NAME)
                return null;
            String channel = mFilteredEntries.get(position).getChannel();
            return channel.length() >= 2 ? channel.substring(1, 2).toUpperCase() : "?";
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
            mName = itemView.findViewById(R.id.name);
            mTopic = itemView.findViewById(R.id.topic);
            itemView.setOnClickListener((View view) -> {
                List<String> channels = new ArrayList<>();
                channels.add((String) mName.getTag());
                mConnection.getApiInstance().joinChannels(channels, (Void v) -> {
                    runOnUiThread(() -> {
                        finish();
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

    private static class UpdateListAsyncTask extends AsyncTask<Void, Void, List<ChannelList.Entry>> {

        private WeakReference<ChannelListActivity> mActivity;
        private String mStartFilterQuery;
        private int mStartSortMode;

        public UpdateListAsyncTask(ChannelListActivity activity) {
            mActivity = new WeakReference<>(activity);
            mStartFilterQuery = activity.mFilterQuery;
            mStartSortMode = activity.mSortMode;
        }

        @Override
        protected List<ChannelList.Entry> doInBackground(Void... voids) {
            ChannelListActivity activity = mActivity.get();
            if (activity == null)
                return null;
            synchronized (activity.mAppendEntries) {
                activity.mEntries.addAll(activity.mAppendEntries);
                activity.mAppendEntries.clear();
                if (activity.mAssignEntries != null) {
                    activity.mEntries = activity.mAssignEntries;
                    activity.mAssignEntries = null;
                }
            }
            List<ChannelList.Entry> ret = new ArrayList<>();
            for (ChannelList.Entry entry : activity.mEntries) {
                if (filterEntry(entry, mStartFilterQuery))
                    ret.add(entry);
            }
            if (mStartSortMode == SORT_NAME) {
                Collections.sort(ret, (ChannelList.Entry l, ChannelList.Entry r) ->
                        l.getChannel().compareToIgnoreCase(r.getChannel()));
            } else if (mStartSortMode == SORT_MEMBER_COUNT) {
                Collections.sort(ret, (ChannelList.Entry l, ChannelList.Entry r) ->
                        Integer.compare(r.getMemberCount(), l.getMemberCount()));
            }
            return ret;
        }

        @Override
        protected void onPostExecute(List<ChannelList.Entry> ret) {
            ChannelListActivity activity = mActivity.get();
            if (activity == null || ret == null)
                return;
            activity.mFilteredEntries = ret;
            activity.mListAdapter.notifyDataSetChanged();
            activity.mUpdateListAsyncTask = null;
            if ((mStartFilterQuery != null && !mStartFilterQuery.equals(activity.mFilterQuery)) ||
                    mStartSortMode != activity.mSortMode) {
                activity.requestListUpdate();
            }
            synchronized (activity.mAppendEntries) {
                if (activity.mAppendEntries.size() > 0)
                    activity.requestListUpdate();
            }
        }
    }

}
