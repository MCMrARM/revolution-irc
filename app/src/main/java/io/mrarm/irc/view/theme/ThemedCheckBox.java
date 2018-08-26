package io.mrarm.irc.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.ThemeHelper;

public class ThemedCheckBox extends AppCompatCheckBox {

    public ThemedCheckBox(Context context) {
        super(context);
        install(this, null);
    }

    public ThemedCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        install(this, attrs);
    }

    public ThemedCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        install(this, attrs);
    }

    public static void install(CompoundButton checkBox, AttributeSet attrs) {
        ThemedView.setupBackground(checkBox, attrs);
        if (ThemeHelper.hasCustomAccentColor(checkBox.getContext())) {
            CompoundButtonCompat.setButtonTintList(checkBox,
                    createCheckBoxTintStateList(checkBox.getContext()));
            if (Build.VERSION.SDK_INT >= 21 && checkBox.getBackground() instanceof RippleDrawable) {
                ((RippleDrawable) checkBox.getBackground()).setColor(
                        createCheckBoxRippleTintStateList(checkBox.getContext()));
            }
        }
    }

    static ColorStateList createCheckBoxTintStateList(Context ctx) {
        int accentColor = ThemeHelper.getAccentColor(ctx);
        int normalColor = StyledAttributesHelper.getColor(ctx, R.attr.colorControlNormal, 0);
        int disabledColor = ColorUtils.setAlphaComponent(normalColor, (int) (255.f *
                StyledAttributesHelper.getFloat(ctx, android.R.attr.disabledAlpha, 1.f)));
        return new ColorStateList(new int[][] {
                new int[] { -android.R.attr.state_enabled },
                new int[] { android.R.attr.state_checked },
                new int[] { }
        }, new int[] {
                disabledColor,
                accentColor,
                normalColor
        });
    }

    static ColorStateList createCheckBoxRippleTintStateList(Context ctx) {
        int accentColor = ThemeHelper.getAccentColor(ctx);
        int normalColor = StyledAttributesHelper.getColor(ctx, R.attr.colorControlHighlight, 0);
        int accentAlphaColor = ColorUtils.setAlphaComponent(accentColor, (int) (255.f * 0.26f)); // highlight_alpha_material_colored
        return new ColorStateList(new int[][] {
                new int[] { android.R.attr.state_enabled, android.R.attr.state_checked },
                new int[] { }
        }, new int[] {
                accentAlphaColor,
                normalColor
        });
    }

}
