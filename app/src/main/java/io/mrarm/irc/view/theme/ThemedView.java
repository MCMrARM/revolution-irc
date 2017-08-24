package io.mrarm.irc.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.ThemeHelper;

public class ThemedView {

    private static final int[] ATTRS_BG = { android.R.attr.background };

    public static void setupBackground(View view, AttributeSet attrs) {
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(view.getContext(), attrs, ATTRS_BG);
        int bgResId = r.getResourceId(android.R.attr.background, 0);
        if (bgResId == R.color.colorPrimary)
            view.setBackgroundColor(ThemeHelper.getPrimaryColor(view.getContext()));
        else if (bgResId == R.color.colorAccent)
            view.setBackgroundColor(ThemeHelper.getAccentColor(view.getContext()));
        else if (bgResId == R.drawable.colored_button)
            ViewCompat.setBackgroundTintList(view, createColoredButtonColorStateList(view.getContext()));
        r.recycle();
    }

    private static ColorStateList createColoredButtonColorStateList(Context ctx) {
        int accentColor = ThemeHelper.getAccentColor(ctx);
        int disabledColor = StyledAttributesHelper.getColor(ctx, R.attr.colorButtonNormal, 0);
        return new ColorStateList(new int[][] {
                new int[] { -android.R.attr.state_enabled },
                new int[] { }
        }, new int[] {
                disabledColor,
                accentColor
        });
    }

}
