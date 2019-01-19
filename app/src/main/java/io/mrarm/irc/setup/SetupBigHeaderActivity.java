package io.mrarm.irc.setup;

import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.mrarm.irc.R;

public class SetupBigHeaderActivity extends SetupActivity {

    private ViewGroup mLayout;
    private int mCustomContentViewId;
    private View mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mCustomContentViewId != 0)
            setContentView(mCustomContentViewId);
        else
            setContentView(R.layout.activity_setup_big_header);

        AppBarLayout appBar = findViewById(R.id.appbar);

        CollapsingToolbarLayout toolbarLayout = findViewById(R.id.toolbar_layout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLayout = findViewById(R.id.layout);
        mLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int height = mLayout.getHeight();
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                    appBar.getLayoutParams();
            params.height = height / 3;
            appBar.setLayoutParams(params);

            int childHeight = (mContentView != null ? mContentView.getHeight() : 0);
            if (mContentView instanceof NestedScrollView)
                childHeight = ((NestedScrollView) mContentView).getChildAt(0).getHeight();
            boolean needsScroll = (mContentView != null && childHeight > height - params.height);

            AppBarLayout.LayoutParams paramsToolbar = (AppBarLayout.LayoutParams) toolbarLayout.getLayoutParams();
            paramsToolbar.setScrollFlags(needsScroll
                    ? (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED)
                    : 0);
            toolbarLayout.setLayoutParams(paramsToolbar);
        });
    }

    public void setCustomContentView(int resId) {
        mCustomContentViewId = resId;
    }

    public void setSetupContentView(int viewId) {
        mContentView = LayoutInflater.from(this).inflate(viewId, mLayout, false);
        mLayout.addView(mContentView);
    }
}
