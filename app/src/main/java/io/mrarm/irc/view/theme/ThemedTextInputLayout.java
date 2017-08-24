package io.mrarm.irc.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.design.widget.TextInputLayout;
import android.util.AttributeSet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.mrarm.irc.util.ThemeHelper;

public class ThemedTextInputLayout extends TextInputLayout {

    private static Field sFieldFocusedTextColor;
    private static Method sMethodUpdateLabelState;

    public ThemedTextInputLayout(Context context) {
        this(context, null);
    }

    public ThemedTextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemedTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (ThemeHelper.hasCustomAccentColor(context))
            setHintColor(this, ThemeHelper.getAccentColor(context));
    }

    // https://stackoverflow.com/questions/35683379/programmatically-set-textinputlayout-hint-text-color-and-floating-label-color
    public static void setHintColor(TextInputLayout layout, int color) {
        try {
            if (sFieldFocusedTextColor == null) {
                sFieldFocusedTextColor = TextInputLayout.class.getDeclaredField("mFocusedTextColor");
                sFieldFocusedTextColor.setAccessible(true);
            }
            ColorStateList myList = ColorStateList.valueOf(color);
            sFieldFocusedTextColor.set(layout, myList);

            if (sMethodUpdateLabelState == null) {
                sMethodUpdateLabelState = TextInputLayout.class.getDeclaredMethod("updateLabelState", boolean.class);
                sMethodUpdateLabelState.setAccessible(true);
            }
            sMethodUpdateLabelState.invoke(layout, true);
        } catch (Throwable ignored) {
        }
    }

}
