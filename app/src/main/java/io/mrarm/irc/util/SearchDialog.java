package io.mrarm.irc.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import java.util.List;

import io.mrarm.irc.R;

public abstract class SearchDialog extends AppCompatDialog implements SearchView.OnQueryTextListener {

    private int mStatusBarColor;
    private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private SuggestionsAdapter mAdapter;
    private List<CharSequence> mSuggestions;

    public SearchDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_search);
        mStatusBarColor = context.getResources().getColor(R.color.searchColorPrimaryDark);
        if (context instanceof Activity)
            setOwnerActivity((Activity) context);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener((View v) -> {
            cancel();
        });
        mSearchView = (SearchView) findViewById(R.id.search_view);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mAdapter = new SuggestionsAdapter(mRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(mAdapter);
        // mRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        mSearchView.setOnQueryTextListener(this);
        mSearchView.setImeOptions((mSearchView.getImeOptions() & ~EditorInfo.IME_MASK_ACTION) |
                EditorInfo.IME_ACTION_GO);
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
            window.setBackgroundDrawable(null);

            WindowManager.LayoutParams params = window.getAttributes();
            params.flags = params.flags & (~WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setAttributes(params);

            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }
    }

    @Override
    public void show() {
        super.show();
        if (Build.VERSION.SDK_INT >= 23) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (getOwnerActivity() != null && Build.VERSION.SDK_INT >= 21) {
            getOwnerActivity().getWindow().setStatusBarColor(mStatusBarColor);
        }
        mSearchView.requestFocus();
    }

    @Override
    public void cancel() {
        super.cancel();
        if (getOwnerActivity() != null && Build.VERSION.SDK_INT >= 21) {
            getOwnerActivity().getWindow().setStatusBarColor(0);
        }
    }

    public void setQueryHint(CharSequence hint) {
        mSearchView.setQueryHint(hint);
    }

    public void setSuggestions(List<CharSequence> suggestions) {
        mSuggestions = suggestions;
        if (suggestions != null && suggestions.size() > 0)
            mRecyclerView.setVisibility(View.VISIBLE);
        else
            mRecyclerView.setVisibility(View.GONE);
        mAdapter.notifyDataSetChanged();
    }

    public void onSuggestionClicked(int index, CharSequence suggestion) {
        onQueryTextSubmit(suggestion.toString());
    }


    public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionHolder> {

        public SuggestionsAdapter(RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
            mRecyclerView.setVisibility(View.GONE);
        }

        @Override
        public SuggestionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dialog_search_item, parent, false);
            return new SuggestionHolder(v);
        }

        @Override
        public void onBindViewHolder(SuggestionHolder holder, int position) {
            holder.bind(mSuggestions.get(position));
        }

        @Override
        public int getItemCount() {
            return mSuggestions == null ? 0 : mSuggestions.size();
        }

    }

    public class SuggestionHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public SuggestionHolder(View itemView) {
            super(itemView);
            mText = (TextView) itemView.findViewById(R.id.text);
            itemView.setOnClickListener((View v) -> {
                onSuggestionClicked(getAdapterPosition(), mText.getText());
            });
        }

        public void bind(CharSequence text) {
            mText.setText(text);
        }

    }

}
