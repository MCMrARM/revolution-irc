package io.mrarm.irc.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import io.mrarm.irc.R;

public class StatusBarColorBottomSheetDialog extends BottomSheetDialog {

    private int mStatusBarColor;

    public StatusBarColorBottomSheetDialog(@NonNull Context context) {
        super(context);
        if (context instanceof Activity)
            setOwnerActivity((Activity) context);
        mStatusBarColor = context.getResources().getColor(R.color.colorPrimaryDark);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        addCallback();
    }

    @Override
    public void setContentView(int layoutResId) {
        super.setContentView(layoutResId);
        addCallback();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        addCallback();
    }

    private void addCallback() {
        BottomSheetBehavior behaviour = BottomSheetBehavior.from(
                findViewById(android.support.design.R.id.design_bottom_sheet));
        behaviour.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    behaviour.setBottomSheetCallback(null);
                    cancel();
                    return;
                }
                WindowManager.LayoutParams params = getWindow().getAttributes();
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if ((params.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0) {
                        params.flags = params.flags & (~WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        getWindow().setAttributes(params);
                    }
                    updateActivityStatusBar(true);
                } else {
                    if ((params.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) == 0) {
                        params.flags = params.flags | WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                        getWindow().setAttributes(params);
                    }
                    updateActivityStatusBar(false);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

    private void updateActivityStatusBar(boolean dialogOpen) {
        if (Build.VERSION.SDK_INT < 21 || getOwnerActivity() == null)
            return;
        if (dialogOpen) {
            getOwnerActivity().getWindow().setStatusBarColor(mStatusBarColor);
        } else {
            getOwnerActivity().getWindow().setStatusBarColor(0);
        }
    }

    public void setStatusBarColor(int color) {
        mStatusBarColor = color;
    }

}
