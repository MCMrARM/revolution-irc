package io.mrarm.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.mrarm.irc.util.ThemeHelper;

public class ThemedActivity extends AppCompatActivity implements ThemeHelper.ThemeChangeListener {

    private boolean mThemeChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper helper = ThemeHelper.getInstance(this);
        helper.addThemeChangeListener(this);
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
        ThemeHelper helper = ThemeHelper.getInstance(this);
        helper.applyThemeToActivity(this);
        super.setTheme(helper.getThemeIdToApply(resid));
    }

    @Override
    public void onThemeChanged() {
        mThemeChanged = true;
    }

}
