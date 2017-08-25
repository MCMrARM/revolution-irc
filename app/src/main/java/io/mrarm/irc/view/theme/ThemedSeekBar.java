package io.mrarm.irc.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.widget.SeekBar;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.ThemeHelper;

public class ThemedSeekBar extends AppCompatSeekBar {

    public ThemedSeekBar(Context context) {
        super(context);
        install(this, null);
    }

    public ThemedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        install(this, attrs);
    }

    public ThemedSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        install(this, attrs);
    }

    public static void install(SeekBar seekBar, AttributeSet attrs) {
        ThemedView.setupBackground(seekBar, attrs);
        if (ThemeHelper.hasCustomAccentColor(seekBar.getContext())) {
            int accentColor = ThemeHelper.getAccentColor(seekBar.getContext());
            LayerDrawable ld = (LayerDrawable) seekBar.getProgressDrawable();
            ld.findDrawableByLayerId(android.R.id.progress).setColorFilter(accentColor,
                    PorterDuff.Mode.SRC_IN);

            DrawableCompat.setTintList(seekBar.getThumb().mutate(),
                    createSeekBarThumbColorList(seekBar.getContext()));
        }
    }

    private static ColorStateList createSeekBarThumbColorList(Context ctx) {
        int accentColor = ThemeHelper.getAccentColor(ctx);
        int normalColor = StyledAttributesHelper.getColor(ctx, R.attr.colorControlNormal, 0);
        int disabledColor = ColorUtils.setAlphaComponent(normalColor, (int) (255.f *
                StyledAttributesHelper.getFloat(ctx, android.R.attr.disabledAlpha, 1.f)));
        return new ColorStateList(new int[][] {
                new int[] { -android.R.attr.state_enabled },
                new int[] { }
        }, new int[] {
                disabledColor,
                accentColor
        });
    }

}
