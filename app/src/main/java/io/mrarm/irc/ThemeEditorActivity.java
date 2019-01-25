package io.mrarm.irc;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import io.mrarm.irc.setting.fragment.theme.ChatThemeSettings;
import io.mrarm.irc.setting.fragment.theme.CommonThemeSettings;
import io.mrarm.irc.util.AppCompatViewFactory;
import io.mrarm.irc.util.theme.ThemeInfo;
import io.mrarm.irc.util.theme.ThemeManager;
import io.mrarm.irc.util.theme.live.LiveThemeManager;
import io.mrarm.irc.util.theme.live.LiveThemeViewFactory;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.UUID;

public class ThemeEditorActivity extends ThemedActivity {

    public static final String ARG_THEME_UUID = "theme";

    private LiveThemeManager mLiveThemeManager;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    private ThemeInfo mThemeInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLiveThemeManager = new LiveThemeManager(this);
        getLayoutInflater().setFactory2(new LiveThemeViewFactory(mLiveThemeManager,
                new AppCompatViewFactory(this)));

        ThemeManager themeManager = ThemeManager.getInstance(this);
        mThemeInfo = themeManager.getCustomTheme(
                UUID.fromString(getIntent().getStringExtra(ARG_THEME_UUID)));
        if (mThemeInfo == null)
            throw new RuntimeException("Invalid theme UUID");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_editor);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(mThemeInfo.name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mTabLayout = findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager, false);
    }

    @Override
    public void onBackPressed() {
        ThemeManager.getInstance(this).invalidateCurrentCustomTheme();
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            ThemeManager.getInstance(this).saveTheme(getThemeInfo());
        } catch (IOException e) {
            Log.w("ThemeEditorActivity", "Failed to save theme");
        }
    }

    public void notifyThemeNameChanged() {
        mToolbar.setTitle(mThemeInfo.name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ThemeInfo getThemeInfo() {
        return mThemeInfo;
    }

    public LiveThemeManager getLiveThemeManager() {
        return mLiveThemeManager;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
                return new CommonThemeSettings();
            if (position == 1)
                return new ChatThemeSettings();
            return null;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return getString(R.string.theme_category_common);
            if (position == 1)
                return getString(R.string.theme_category_chat);
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
