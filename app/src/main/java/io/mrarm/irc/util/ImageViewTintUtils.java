package io.mrarm.irc.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import androidx.core.graphics.ColorUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

public class ImageViewTintUtils {

    public static void setTint(ImageView view, int color) {
        view.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public static void animateTint(ImageView view, int from, int to, int duration) {
        final float[] fromHSL = new float[3];
        final float[] toHSL = new float[3];
        float[] currentHSL = new float[3];
        ColorUtils.colorToHSL(from, fromHSL);
        ColorUtils.colorToHSL(to, toHSL);
        int fromAlpha = Color.alpha(from);
        int toAlpha = Color.alpha(to);

        final ValueAnimator anim = ObjectAnimator.ofFloat(0f, 1f);
        anim.addUpdateListener((ValueAnimator animation) -> {
            float mul = (Float) animation.getAnimatedValue();
            ColorUtils.blendHSL(fromHSL, toHSL, mul, currentHSL);
            int color = ColorUtils.HSLToColor(currentHSL);
            color = ColorUtils.setAlphaComponent(color, fromAlpha +
                    (int) ((toAlpha - fromAlpha) * mul));
            setTint(view, color);
        });
        anim.setDuration(duration);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
    }

}
