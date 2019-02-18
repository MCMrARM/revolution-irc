package io.mrarm.irc.newui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.mrarm.irc.R;
import io.mrarm.irc.ThemedActivity;

public class MainActivity extends ThemedActivity implements SlideableFragmentContainer.DragListener {

    private SlideableFragmentContainer mContainer;
    private DrawerArrowDrawable mDrawerDrawable;
    private ViewGroup mToolbarContainer;
    private View mToolbarFragmentView;
    private View mToolbarParentFragmentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newui_main);

        mToolbarContainer = findViewById(R.id.toolbar);

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentLifecycleWatcher(), false);

        mContainer = findViewById(R.id.container);
        mContainer.setFragmentManager(getSupportFragmentManager());

        AppCompatImageButton btn = findViewById(R.id.nav_button);
        mDrawerDrawable = new DrawerArrowDrawable(btn.getContext());
        btn.setImageDrawable(mDrawerDrawable);
        mContainer.addDragListener(this);

        btn.setOnClickListener((v) -> {
            mContainer.popAnim();
        });

        mContainer.push(new MainFragment());
    }

    public SlideableFragmentContainer getContainer() {
        return mContainer;
    }

    @Override
    public void onDragStarted(View dragView, View parentView) {
        if (mToolbarParentFragmentView != null)
            mToolbarParentFragmentView.setVisibility(View.VISIBLE);
        mToolbarFragmentView.setBackground(mToolbarContainer.getBackground()
                .getConstantState().newDrawable());
    }

    @Override
    public void onDragValueChanged(float value) {
        float iv = value / mContainer.getWidth();
        mDrawerDrawable.setProgress(1.f - iv);

        mToolbarFragmentView.setAlpha(1.f - iv);
    }

    @Override
    public void onDragEnded() {
        if (mToolbarParentFragmentView != null)
            mToolbarParentFragmentView.setVisibility(View.GONE);
        mToolbarFragmentView.setBackground(null);
    }

    private class FragmentLifecycleWatcher extends FragmentManager.FragmentLifecycleCallbacks {

        @Override
        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                          @NonNull View v, @Nullable Bundle savedInstanceState) {
            View c = ((FragmentToolbarCallback) f).onCreateToolbar(getLayoutInflater(),
                    mToolbarContainer);
            c.setTag(f);
            mToolbarContainer.addView(c);
            if (mToolbarFragmentView != null &&
                    mToolbarFragmentView.getTag() != mContainer.getCurrentFragment()) {
                mToolbarParentFragmentView = mToolbarFragmentView;
                mToolbarFragmentView.setVisibility(View.GONE);
            }
            mToolbarFragmentView = c;
        }

        @Override
        public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            View v = mToolbarContainer.findViewWithTag(f);
            if (v != null)
                mToolbarContainer.removeView(v);

            mToolbarFragmentView = mToolbarContainer.findViewWithTag(
                    mContainer.getCurrentFragment());
            if (mToolbarFragmentView != null) {
                mToolbarFragmentView.setVisibility(View.VISIBLE);
                if (mContainer.getFragmentCount() > 1) {
                    mToolbarParentFragmentView = mToolbarFragmentView.findViewWithTag(
                            mContainer.getFragment(1));
                }
            }
        }
    }


    public interface FragmentToolbarCallback {

        View onCreateToolbar(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);
    }

}
