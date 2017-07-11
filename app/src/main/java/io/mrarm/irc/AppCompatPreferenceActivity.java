package io.mrarm.irc;

import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;

import io.mrarm.irc.util.ImageViewTintUtils;
import io.mrarm.irc.util.ListAdapterWrapper;

/**
 * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public abstract class AppCompatPreferenceActivity extends PreferenceActivity {

    private AppCompatDelegate mDelegate;
    private int mThemeId = 0;

    private int mFgColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        if (getDelegate().applyDayNight() && mThemeId != 0) {
            if (Build.VERSION.SDK_INT >= 23) {
                onApplyThemeResource(getTheme(), mThemeId, false);
            } else {
                setTheme(mThemeId);
            }
        }
        super.onCreate(savedInstanceState);

        if (getListAdapter() != null) {
            setListAdapter(new TintHeaderAdapterWrapper(getListAdapter(), getForegroundColor()));
        }
    }

    public int getForegroundColor() {
        if (mFgColor == 0) {
            TypedArray ta = obtainStyledAttributes(new int[]{android.R.attr.colorForeground});
            mFgColor = ta.getColor(0, 0);
            ta.recycle();
        }
        return mFgColor;
    }

    @Override
    public void setTheme(int resid) {
        super.setTheme(resid);
        mThemeId = resid;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getDelegate().onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    public AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    private static class TintHeaderAdapterWrapper extends ListAdapterWrapper {

        private int mTintColor;

        public TintHeaderAdapterWrapper(ListAdapter adapter, int tintColor) {
            super(adapter);
            mTintColor = tintColor;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup ret = (ViewGroup) super.getView(position, convertView, parent);
            int cc = ret.getChildCount();
            for (int i = 0; i < cc; i++) {
                View view = ret.getChildAt(i);
                if (view instanceof ImageView) {
                    ImageViewTintUtils.setTint((ImageView) view, mTintColor);
                    return ret;
                }
            }
            return ret;
        }

    }

}
