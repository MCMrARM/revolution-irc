package io.mrarm.irc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import io.mrarm.irc.setting.fragment.theme.ChatThemeSettings;
import io.mrarm.irc.setting.fragment.theme.CommonThemeSettings;
import io.mrarm.irc.setup.BackupProgressActivity;
import io.mrarm.irc.util.AppCompatViewFactory;
import io.mrarm.irc.util.theme.ThemeInfo;
import io.mrarm.irc.util.theme.ThemeManager;
import io.mrarm.irc.util.theme.live.LiveThemeManager;
import io.mrarm.irc.util.theme.live.LiveThemeViewFactory;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class ThemeEditorActivity extends ThemedActivity {

    public static final String ARG_THEME_UUID = "theme";

    public static final int REQUEST_CODE_EXPORT = 100;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings_theme, menu);
        if (Build.VERSION.SDK_INT < 19)
            menu.findItem(R.id.action_export).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_rename) {
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_edit_text, null);
            EditText text = view.findViewById(R.id.edit_text);
            text.setText(getThemeInfo().name);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.action_rename)
                    .setView(view)
                    .setPositiveButton(R.string.action_ok, (dialog1, which) -> {
                        getThemeInfo().name = text.getText().toString();
                        notifyThemeNameChanged();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return true;
        } else if (item.getItemId() == R.id.action_export) {
            if (Build.VERSION.SDK_INT >= 19) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/x-mrarm-irc-theme");
                intent.putExtra(Intent.EXTRA_TITLE, getThemeInfo().name + ".irctheme");
                startActivityForResult(intent, REQUEST_CODE_EXPORT);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXPORT) {
            if (data == null || data.getData() == null)
                return;
            try {
                Uri uri = data.getData();
                ParcelFileDescriptor desc = getContentResolver().openFileDescriptor(uri, "w");
                BufferedWriter wr = new BufferedWriter(new FileWriter(desc.getFileDescriptor()));
                ThemeManager.getInstance(this).exportTheme(getThemeInfo(), wr);
                wr.close();
                desc.close();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
