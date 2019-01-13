package io.mrarm.irc;

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

    @Override
    public void setTheme(int resid) {
        ThemeManager helper = ThemeManager.getInstance(this);
        helper.applyThemeToActivity(this);
        super.setTheme(helper.getThemeIdToApply(resid));
    }

    @Override
    public void onThemeChanged() {
        mThemeChanged = true;
    }

}
