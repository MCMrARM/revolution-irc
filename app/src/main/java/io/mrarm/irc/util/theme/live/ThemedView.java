package io.mrarm.irc.util.theme.live;

import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import io.mrarm.irc.util.StyledAttributesHelper;

public class ThemedView {

    private static final int[] THEME_ATTRS = { android.R.attr.background };

    static void setupTheming(View view, LiveThemeComponent component, AttributeSet attrs, int defStyleAttr) {
        Resources.Theme t = component.getTheme();
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(view.getContext(), t, attrs, THEME_ATTRS, defStyleAttr);
        component.addColorAttr(r, android.R.attr.background, view::setBackgroundColor);
        r.recycle();
    }

}
