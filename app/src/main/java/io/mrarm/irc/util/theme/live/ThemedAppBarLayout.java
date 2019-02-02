package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.appbar.AppBarLayout;

import io.mrarm.irc.R;

public class ThemedAppBarLayout extends AppBarLayout {

    protected LiveThemeComponent mThemeComponent;

    public ThemedAppBarLayout(Context context) {
        this(context, null);
    }

    public ThemedAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, 0,
                R.style.Widget_Design_AppBarLayout);
    }

    public ThemedAppBarLayout(Context context, AttributeSet attrs, LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }
    
}
