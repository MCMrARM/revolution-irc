package io.mrarm.irc.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

public class ViewFadeHelper {

    public static void showView(View view) {
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1.f).setListener(null).start();
    }

    public static void hideView(View view) {
        view.animate().alpha(0.f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.animate().setListener(null);
                view.setVisibility(View.INVISIBLE);
            }
        }).start();
    }

}
