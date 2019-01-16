package io.mrarm.irc.util.theme.live;

import android.view.View;
import android.view.ViewParent;

import io.mrarm.irc.R;

public class ThemedAlertDialog {

    public static void applyTheme(View v, LiveThemeManager themeManager) {
        ViewParent p = v.getParent();
        // The root parent will be a DecorView. We can't set the background to the DecorView,
        // so we instead set the background to the DecorView's child.
        while (p.getParent() != null && p.getParent().getParent() != null)
            p = p.getParent();
        ((View) p).setBackgroundColor(themeManager.getColor(R.attr.colorBackgroundFloating));
    }

}
