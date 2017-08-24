package io.mrarm.irc.view.theme;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.ThemeHelper;

public class ThemedTextView extends AppCompatTextView {

    private static final int[] ATTRS_COLOR = { android.R.attr.textColor, android.R.attr.textAppearance };

    public ThemedTextView(Context context) {
        super(context);
        install(this, null);
    }

    public ThemedTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        install(this, attrs);
    }

    public ThemedTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        install(this, attrs);
    }

    public static void install(TextView view, AttributeSet attrs) {
        ThemedView.setupBackground(view, attrs);
        setupTextColor(view, attrs);
    }

    public static void setupTextColor(TextView view, AttributeSet attrs) {
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(view.getContext(), attrs, ATTRS_COLOR);
        int colorResId = r.getResourceId(android.R.attr.textColor, 0);
        if (colorResId == 0) {
            int appearanceRes = r.getResourceId(android.R.attr.textAppearance, 0);
            if (appearanceRes != 0) {
                StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(
                        view.getContext(), appearanceRes, new int[] { android.R.attr.textColor });
                colorResId = ta.getResourceId(android.R.attr.textColor, 0);
                ta.recycle();
            }
        }
        if (colorResId == R.color.colorPrimary)
            view.setTextColor(ThemeHelper.getPrimaryColor(view.getContext()));
        else if (colorResId == R.color.colorAccent)
            view.setTextColor(ThemeHelper.getAccentColor(view.getContext()));
        r.recycle();
    }

}
