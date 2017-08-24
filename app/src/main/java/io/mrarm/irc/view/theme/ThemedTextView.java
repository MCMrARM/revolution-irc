package io.mrarm.irc.view.theme;

import android.util.AttributeSet;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.ThemeHelper;

public class ThemedTextView {

    private static final int[] ATTRS_COLOR = { android.R.attr.textColor };

    public static void setupTextColor(TextView view, AttributeSet attrs) {
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(view.getContext(), attrs, ATTRS_COLOR);
        int colorResId = r.getResourceId(android.R.attr.textColor, 0);
        if (colorResId == R.color.colorPrimary)
            view.setTextColor(ThemeHelper.getPrimaryColor(view.getContext()));
        else if (colorResId == R.color.colorAccent)
            view.setTextColor(ThemeHelper.getAccentColor(view.getContext()));
        r.recycle();
    }

}
