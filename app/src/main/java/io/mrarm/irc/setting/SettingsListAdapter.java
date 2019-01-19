package io.mrarm.irc.setting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import io.mrarm.irc.util.AdvancedDividerItemDecoration;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleCounter;

public class SettingsListAdapter extends EntryRecyclerViewAdapter {

    private Activity mActivity;
    private Fragment mFragment;
    private SimpleCounter mRequestCodeCounter;

    public SettingsListAdapter(Activity activity) {
        mActivity = activity;
    }

    public SettingsListAdapter(Fragment fragment) {
        mFragment = fragment;
        mActivity = mFragment.getActivity();
    }

    public ItemDecoration createItemDecoration() {
        return new ItemDecoration(mActivity);
    }

    public Activity getActivity() {
        return mActivity;
    }

    public SimpleCounter getRequestCodeCounter() {
        return mRequestCodeCounter;
    }

    public void setRequestCodeCounter(SimpleCounter counter) {
        mRequestCodeCounter = counter;
    }

    public void startActivityForResult(Intent intent, int requestId) {
        if (mFragment != null)
            mFragment.startActivityForResult(intent, requestId);
        else
            mActivity.startActivityForResult(intent, requestId);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Entry e : mEntries) {
            if (e instanceof ActivityResultCallback)
                ((ActivityResultCallback) e).onActivityResult(mActivity, requestCode, resultCode, data);
        }
    }

    public interface ActivityResultCallback {
        void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data);
    }

    public abstract static class SettingsEntryHolder<T extends Entry> extends EntryHolder<T> {

        protected SettingsListAdapter mAdapter;

        public SettingsEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView);
            mAdapter = adapter;
        }

        public boolean hasDivider() {
            return true;
        }

    }

    public static class ItemDecoration extends AdvancedDividerItemDecoration {

        public ItemDecoration(Context context) {
            super(context);
        }

        @Override
        public boolean hasDivider(RecyclerView parent, View view) {
            return ((SettingsListAdapter.SettingsEntryHolder) parent.getChildViewHolder(view))
                    .hasDivider();
        }
    }

    public interface SettingChangedListener {

        void onSettingChanged(Entry entry);

    }


}
