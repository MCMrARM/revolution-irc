package io.mrarm.irc.dialog;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.View;
import android.view.ViewGroup;

import io.mrarm.irc.R;

public class ProperHeightBottomSheetDialog extends BottomSheetDialog {

    public ProperHeightBottomSheetDialog(@NonNull Context context) {
        super(context);
    }

    public ProperHeightBottomSheetDialog(@NonNull Context context, int theme) {
        super(context, theme);
    }

    protected ProperHeightBottomSheetDialog(@NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        configureBottomSheetLayout();
    }

    @Override
    public void setContentView(int layoutResId) {
        super.setContentView(layoutResId);
        configureBottomSheetLayout();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        configureBottomSheetLayout();
    }

    protected void configureBottomSheetLayout() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) findViewById(
                R.id.design_bottom_sheet).getLayoutParams();
        ProperHeightBottomSheetBehaviour behaviour = new ProperHeightBottomSheetBehaviour();
        behaviour.setHideable(true);
        behaviour.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    cancel();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        params.setBehavior(new ProperHeightBottomSheetBehaviour());
    }

    private static class ProperHeightBottomSheetBehaviour<T extends View> extends BottomSheetBehavior<T> {

        private int mMinHeight = -1;

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, T child, int layoutDirection) {
            if (mMinHeight == -1)
                mMinHeight = child.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_min_height);
            setPeekHeight(Math.max(parent.getHeight() - parent.getWidth() * 9 / 16, mMinHeight));
            return super.onLayoutChild(parent, child, layoutDirection);
        }

    }

}
