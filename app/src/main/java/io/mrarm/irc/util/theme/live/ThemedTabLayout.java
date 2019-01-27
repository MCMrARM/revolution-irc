package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;

import com.google.android.material.tabs.TabLayout;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ThemedTabLayout extends TabLayout {

    private static final int[] THEME_ATTRS = { R.attr.tabTextColor, R.attr.tabTextAppearance,
            R.attr.tabIndicatorColor };
    
    protected LiveThemeComponent mThemeComponent;

    public ThemedTabLayout(Context context) {
        this(context, null);
    }

    public ThemedTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.tabStyle);
    }

    public ThemedTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedTabLayout(Context context, AttributeSet attrs, LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

    static void setupTheming(TabLayout tabLayout, LiveThemeComponent component, AttributeSet attrs, int defStyleAttr) {
        Resources.Theme t = component.getTheme();
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(tabLayout.getContext(), t, attrs, THEME_ATTRS, defStyleAttr, R.style.Widget_Design_TabLayout);
        StyledAttributesHelper textAppearance = r.obtainChildAttrs(t, R.attr.tabTextAppearance, ThemedTextView.TEXT_APPEARANCE_ATTRS);
        if (!component.addColorAttr(r, R.attr.tabTextColor, null, tabLayout::setTabTextColors))
            component.addColorAttr(textAppearance, android.R.attr.textColor, null, tabLayout::setTabTextColors);
        component.addColorAttr(r, R.attr.tabIndicatorColor, tabLayout::setSelectedTabIndicatorColor, null);
        textAppearance.recycle();
        r.recycle();
    }
    
}
