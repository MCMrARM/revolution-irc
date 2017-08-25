package io.mrarm.irc.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.ThemeHelper;

public class ThemedTextView extends AppCompatTextView {

    private static final int[] THEME_ATTRS = { android.R.attr.textColor,
            android.R.attr.textAppearance, android.R.attr.drawableStart,
            android.R.attr.drawableTop, android.R.attr.drawableEnd,
            android.R.attr.drawableBottom };
    private static final int[] DRAWABLE_ATTRS = { android.R.attr.drawableStart,
            android.R.attr.drawableTop, android.R.attr.drawableEnd,
            android.R.attr.drawableBottom };

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
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(view.getContext(), attrs, THEME_ATTRS);
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

        Drawable[] drawables = TextViewCompat.getCompoundDrawablesRelative(view);
        boolean hasChange = false;
        for (int i = 0; i < 4; i++) {
            Drawable newDrawable = tintDrawable(view.getContext(), r, DRAWABLE_ATTRS[i], drawables[i]);
            if (newDrawable != drawables[i]) {
                drawables[i] = newDrawable;
                hasChange = true;
            }
        }
        if (hasChange)
            TextViewCompat.setCompoundDrawablesRelative(view, drawables[0], drawables[1], drawables[2], drawables[3]);
        r.recycle();
    }

    private static Drawable tintDrawable(Context ctx, StyledAttributesHelper attrs, int attr, Drawable d) {
        int resId = attrs.getResourceId(attr, 0);
        if (d == null)
            return null;
        ColorStateList tint = null;
        if (resId == StyledAttributesHelper.getResourceId(ctx, android.R.attr.listChoiceIndicatorSingle, 0))
            tint = ThemedCheckBox.createCheckBoxTintStateList(ctx);
        if (tint != null) {
            d = DrawableCompat.wrap(d.mutate());
            DrawableCompat.setTintList(d, tint);
        }
        return d;
    }

}
