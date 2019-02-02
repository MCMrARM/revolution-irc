package io.mrarm.irc.util.theme.live;

import android.content.res.Resources;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ThemedView {

    private static final int[] THEME_ATTRS = { android.R.attr.background, R.attr.backgroundTint };

    static void setupTheming(View view, LiveThemeComponent component, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        Resources.Theme t = component.getTheme();
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(view.getContext(), t, attrs, THEME_ATTRS, defStyleAttr, defStyleRes);
        component.addColorAttr(r, android.R.attr.background, view::setBackgroundColor);
        if (!component.addColorAttr(r, R.attr.backgroundTint, null, (c) -> ViewCompat.setBackgroundTintList(view, c)))
            LiveThemeUtils.tintAppCompatDrawable(component, r.getResourceId(android.R.attr.background, 0), (c) -> ViewCompat.setBackgroundTintList(view, c));
        r.recycle();
    }

    static void setupTheming(View view, LiveThemeComponent component, AttributeSet attrs, int defStyleAttr) {
        setupTheming(view, component, attrs, defStyleAttr, 0);
    }

}
