package io.mrarm.irc.dialog;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.ClickableRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleTextWatcher;

public abstract class SearchDialog extends AppCompatDialog {

    private int mStatusBarColor;
    private View mRootView;
    private RecyclerView mRecyclerView;
    private AutoCloseDialogEditText mSearchText;
    private View mSearchTextClear;

    public SearchDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_search);

        mStatusBarColor = context.getResources().getColor(R.color.searchColorPrimaryDark);
        if (context instanceof Activity)
            setOwnerActivity((Activity) context);

        mRootView = findViewById(R.id.root);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener((View v) -> {
            cancel();
        });
        mSearchText = findViewById(R.id.search_text);
        mSearchText.setDialog(this);
        mSearchTextClear = findViewById(R.id.search_text_clear);
        mSearchTextClear.setOnClickListener((View v) -> {
            mSearchText.getText().clear();
        });
        mSearchTextClear.setVisibility(View.GONE);

        mRecyclerView = findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setVisibility(View.GONE);
        // mRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        mRecyclerView.addOnLayoutChangeListener((View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) -> updateOverscrollMode());

        mSearchText.addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
            onQueryTextChange(s.toString());
            mSearchTextClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
        }));
        mSearchText.setOnEditorActionListener((TextView textView, int i, KeyEvent keyEvent) -> {
            InputMethodManager manager = (InputMethodManager) getContext().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(mSearchText.getApplicationWindowToken(), 0);
            return true;
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }
    }

    @Override
    public void show() {
        super.show();
        if (Build.VERSION.SDK_INT >= 23 & isLightColor(mStatusBarColor)) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (getOwnerActivity() != null && Build.VERSION.SDK_INT >= 21) {
            getOwnerActivity().getWindow().setStatusBarColor(mStatusBarColor);
        }
        mSearchText.requestFocus();
    }

    @Override
    public void dismiss() {
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(mSearchText.getApplicationWindowToken(), 0);
        super.dismiss();
        if (getOwnerActivity() != null && Build.VERSION.SDK_INT >= 21) {
            getOwnerActivity().getWindow().setStatusBarColor(0);
        }
    }

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

    public abstract void onQueryTextChange(String newQuery);

    public void onQueryTextSubmit(String query) {
        //
    }


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

}
