package io.mrarm.irc.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.ClickableRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleTextWatcher;

public class ListSearchView extends FrameLayout {

    private Dialog mDialog;
    private View mRootView;
    private RecyclerView mRecyclerView;
    private BackButtonListenerEditText mSearchText;
    private View mSearchTextClear;
    private Toolbar mToolbar;
    private int mStatusBarColor;
    private QueryListener mQueryListener;

    public ListSearchView(Context context) {
        this(context, (AttributeSet) null);
    }

    public ListSearchView(Context context, QueryListener listener) {
        this(context, (AttributeSet) null);
        mQueryListener = listener;
    }

    public ListSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.dialog_search, this);
        setFitsSystemWindows(true);

        mRootView = findViewById(R.id.root);

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener((View v) -> {
            if (mQueryListener != null)
                mQueryListener.onCancelled();
        });
        mSearchText = findViewById(R.id.search_text);
        mSearchText.setBackButtonListener(() -> {
            if (mQueryListener != null)
                mQueryListener.onCancelled();
        });
        mSearchTextClear = findViewById(R.id.search_text_clear);
        mSearchTextClear.setOnClickListener((View v) -> {
            mSearchText.getText().clear();
        });
        mSearchTextClear.setVisibility(View.GONE);

        mRecyclerView = findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setVisibility(View.GONE);
        // mRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        mRecyclerView.addOnLayoutChangeListener((View v, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) -> updateOverscrollMode());

        mSearchText.addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
            if (mQueryListener != null)
                mQueryListener.onQueryTextChange(s.toString());
            mSearchTextClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
        }));
        mSearchText.setOnEditorActionListener((TextView textView, int i, KeyEvent keyEvent) -> {
            InputMethodManager manager = (InputMethodManager) getContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(mSearchText.getApplicationWindowToken(), 0);
            return true;
        });

        mStatusBarColor = getResources().getColor(R.color.searchColorPrimaryDark);
    }

    public void setDialog(Dialog dialog) {
        mDialog = dialog;
    }

    public void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(mSearchText.getApplicationWindowToken(), 0);
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (isVisible)
            mSearchText.requestFocus();
        else
            hideKeyboard();
        if (Build.VERSION.SDK_INT >= 23 & isLightColor(mStatusBarColor)) {
            View decorView = mDialog.getWindow().getDecorView();
            if (mDialog != null)
                decorView = mDialog.getWindow().getDecorView();
            int vis = decorView.getSystemUiVisibility();
            if (isVisible)
                vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else
                vis = vis & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(vis);
        }
        if (mDialog != null && Build.VERSION.SDK_INT >= 21)
            mDialog.getWindow().setStatusBarColor(isVisible ? mStatusBarColor : 0);
    }

    @Override
    public void setBackgroundColor(int color) {
        mRootView.setBackgroundColor(color);
    }

    public String getCurrentQuery() {
        return mSearchText.getText().toString();
    }

    public void setQueryHint(CharSequence hint) {
        mSearchText.setHint(hint);
    }

    public void setSuggestionsAdapter(RecyclerView.Adapter adapter) {
        if (mRecyclerView.getAdapter() != null)
            mRecyclerView.getAdapter().unregisterAdapterDataObserver(mDataObserver);
        mRecyclerView.setAdapter(adapter);
        adapter.registerAdapterDataObserver(mDataObserver);
        mDataObserver.onChanged();
    }

    private void updateOverscrollMode() {
        LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        if (lm.findFirstCompletelyVisibleItemPosition() == 0 &&
                lm.findLastCompletelyVisibleItemPosition() == mRecyclerView.getAdapter().getItemCount() - 1) {
            mRecyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        } else {
            mRecyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_ALWAYS);
        }
    }

    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            if (mRecyclerView.getAdapter().getItemCount() > 0)
                mRecyclerView.setVisibility(View.VISIBLE);
            else
                mRecyclerView.setVisibility(View.GONE);
        }
    };

    public static class SimpleSuggestionsAdapter extends
            ClickableRecyclerViewAdapter<SimpleSuggestionsAdapter.SuggestionHolder, CharSequence> {

        public SimpleSuggestionsAdapter() {
            setViewHolderFactory(SuggestionHolder::new, R.layout.dialog_search_item);
        }

        public class SuggestionHolder extends ClickableRecyclerViewAdapter.ViewHolder<CharSequence> {
            private TextView mText;

            public SuggestionHolder(View itemView) {
                super(itemView);
                mText = itemView.findViewById(R.id.text);
            }

            @Override
            public void bind(CharSequence item) {
                mText.setText(item);
            }
        }

    }

    private static boolean isLightColor(int color) {
        return Color.red(color) > 180 && Color.green(color) > 180 && Color.blue(color) > 180;
    }

    public interface QueryListener {

        void onQueryTextChange(String newQuery);

        void onQueryTextSubmit(String query);

        void onCancelled();

    }

}
