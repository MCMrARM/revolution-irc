package io.mrarm.irc.view.theme;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.widget.Spinner;

import io.mrarm.irc.util.ThemeHelper;

public class ThemedSpinner extends AppCompatSpinner {

    public ThemedSpinner(Context context) {
        super(context);
        install(this);
    }

    public ThemedSpinner(Context context, int mode) {
        super(context, mode);
        install(this);
    }

    public ThemedSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        install(this);
    }

    public ThemedSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        install(this);
    }

    public ThemedSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
        install(this);
    }

    public ThemedSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode, Resources.Theme popupTheme) {
        super(context, attrs, defStyleAttr, mode, popupTheme);
        install(this);
    }

    public static void install(Spinner spinner) {
        if (ThemeHelper.hasCustomPrimaryColor(spinner.getContext())) {
            int accentColor = ThemeHelper.getAccentColor(spinner.getContext());
            ThemedEditText.setBackgroundActiveColor(spinner, accentColor);
        }
    }

}
