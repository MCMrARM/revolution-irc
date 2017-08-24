package io.mrarm.irc;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import io.mrarm.irc.util.ThemeHelper;
import io.mrarm.irc.view.theme.ThemedViewFactory;

public class ThemedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new ThemedViewFactory(this));
        super.onCreate(savedInstanceState);
        applyStatusBarColor();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyStatusBarColor();
        applyActionBarColor();
    }

    private void applyActionBarColor() {
        if (getSupportActionBar() != null && ThemeHelper.hasCustomPrimaryColor(this))
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(
                    ThemeHelper.getPrimaryColor(this)));
    }

    private void applyStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getWindow() != null && getWindow().getStatusBarColor() != Color.TRANSPARENT)
                getWindow().setStatusBarColor(ThemeHelper.getPrimaryDarkColor(this));
        }

        ViewGroup root = findViewById(android.R.id.content);
        if (root != null) {
            View view = root.getChildAt(0);
            if (view != null && view instanceof DrawerLayout)
                ((DrawerLayout) view).setStatusBarBackgroundColor(ThemeHelper.getPrimaryDarkColor(this));
        }
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        if (ThemeHelper.hasCustomPrimaryColor(this)) {
            toolbar.setBackgroundColor(ThemeHelper.getPrimaryColor(this));
            if (toolbar.getParent() instanceof AppBarLayout)
                ((View) toolbar.getParent()).setBackgroundColor(ThemeHelper.getPrimaryColor(this));
        }
    }

}
