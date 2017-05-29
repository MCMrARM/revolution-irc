package io.mrarm.irc.util;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

public class ExpandIconStateHelper {

    public static void setExpanded(View view, boolean expanded) {
        view.setRotation(expanded ? 180.f : 0.f);
    }

    public static void animateSetExpanded(View view, boolean expanded) {
        view.setRotation(expanded ? 180.f : 0.f);
        int toDegrees = (expanded ? 0 : 360);
        RotateAnimation rotate = new RotateAnimation(180, toDegrees,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(250);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        view.startAnimation(rotate);
    }

}
