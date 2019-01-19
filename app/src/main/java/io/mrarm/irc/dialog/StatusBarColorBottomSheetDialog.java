package io.mrarm.irc.dialog;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import android.view.View;
import android.view.WindowManager;

import io.mrarm.irc.R;

public class StatusBarColorBottomSheetDialog extends ProperHeightBottomSheetDialog {

    private int mStatusBarColor;

    public StatusBarColorBottomSheetDialog(@NonNull Context context) {
        super(context);
        if (context instanceof Activity)
            setOwnerActivity((Activity) context);
        mStatusBarColor = context.getResources().getColor(R.color.colorPrimaryDark);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        updateActivityStatusBar(false);
    }

    @Override
    protected void configureBottomSheetLayout() {
        super.configureBottomSheetLayout();
        BottomSheetBehavior behaviour = BottomSheetBehavior.from(
                findViewById(R.id.design_bottom_sheet));
        behaviour.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
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
