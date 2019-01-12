package io.mrarm.irc;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.mrarm.irc.util.ThemeHelper;

public class ThemedActivity extends AppCompatActivity {

    private int mCurrentPrimaryColor;
    private int mCurrentAccentColor;
    private int mAppTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mCurrentPrimaryColor = ThemeHelper.getPrimaryColor(this);
        mCurrentAccentColor = ThemeHelper.getAccentColor(this);
        ThemeHelper.applyThemeToActivity(this, mAppTheme);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ThemeHelper.getPrimaryColor(this) != mCurrentPrimaryColor ||
                ThemeHelper.getAccentColor(this) != mCurrentAccentColor)
            recreate();
    }

    @Override
    public void setTheme(int resid) {
        super.setTheme(resid);
        mAppTheme = resid;
    }

}
