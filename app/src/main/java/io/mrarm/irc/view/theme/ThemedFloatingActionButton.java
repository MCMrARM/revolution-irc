package io.mrarm.irc.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import io.mrarm.irc.util.ThemeHelper;

public class ThemedFloatingActionButton extends FloatingActionButton {

    public ThemedFloatingActionButton(Context context) {
        this(context, null);
    }

    public ThemedFloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemedFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (ThemeHelper.hasCustomAccentColor(context))
            setBackgroundTintList(ColorStateList.valueOf(ThemeHelper.getAccentColor(context)));
    }

}
