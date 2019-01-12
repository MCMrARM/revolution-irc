package io.mrarm.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.mrarm.irc.util.ThemeHelper;

public class ThemedActivity extends AppCompatActivity implements ThemeHelper.ThemeChangeListener {

    private int mAppTheme;
    private boolean mThemeChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper helper = ThemeHelper.getInstance(this);
        helper.addThemeChangeListener(this);
        helper.applyThemeToActivity(this, mAppTheme);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        ThemeHelper.getInstance(this).removeThemeChangeListener(this);
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
        super.setTheme(resid);
        mAppTheme = resid;
    }

    @Override
    public void onThemeChanged() {
        mThemeChanged = true;
    }

}
