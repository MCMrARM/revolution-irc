package io.mrarm.irc.dialog;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import io.mrarm.irc.R;
import io.mrarm.irc.view.ListSearchView;

public abstract class SearchDialog extends AppCompatDialog implements ListSearchView.QueryListener {

    private ListSearchView mSearchView;

    public SearchDialog(@NonNull Context context) {
        super(context, R.style.ThemeOverlay_AppCompat);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        mSearchView = new ListSearchView(context, this);
        mSearchView.setDialog(this);
        setContentView(mSearchView);

        if (context instanceof Activity)
            setOwnerActivity((Activity) context);
    }

    public ListSearchView getSearchView() {
        return mSearchView;
    }

    public void setSuggestionsAdapter(RecyclerView.Adapter adapter) {
        getSearchView().setSuggestionsAdapter(adapter);
    }

    public void setQueryHint(String hint) {
        getSearchView().setQueryHint(hint);
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
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }
    }

    @Override
    public void dismiss() {
        if (mSearchView != null)
            mSearchView.hideKeyboard();
        super.dismiss();
    }

    @Override
    public void onQueryTextSubmit(String query) {
    }

    @Override
    public void onCancelled() {
        cancel();
    }

}
