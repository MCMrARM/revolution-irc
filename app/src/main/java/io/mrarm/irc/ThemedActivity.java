package io.mrarm.irc;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.mrarm.irc.util.theme.ThemeManager;

public class ThemedActivity extends AppCompatActivity implements ThemeManager.ThemeChangeListener {

    private boolean mThemeChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager helper = ThemeManager.getInstance(this);
        helper.addThemeChangeListener(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        ThemeManager.getInstance(this).removeThemeChangeListener(this);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mThemeChanged)
            recreate();
    }

    public boolean hasThemeChanged() {
        return mThemeChanged;
    }

    @Override
    public void setTheme(int resid) {
        ThemeManager helper = ThemeManager.getInstance(this);
        helper.applyThemeToActivity(this);
        super.setTheme(helper.getThemeIdToApply(resid));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        ThemeManager helper = ThemeManager.getInstance(this);
        helper.applyThemeToActivity(this);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onThemeChanged() {
        mThemeChanged = true;
    }

}
