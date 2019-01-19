package io.mrarm.irc.dialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.view.MaterialColorPicker;

public class MaterialColorPickerDialog {

    private Context mContext;
    private CharSequence mTitle;
    private MaterialColorPicker.ColorPickCallback mCallback;

    public MaterialColorPickerDialog(Context ctx) {
        mContext = ctx;
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    public void setColorPickListener(MaterialColorPicker.ColorPickCallback callback) {
        mCallback = callback;
    }

    public void show() {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.dialog_material_color_picker, null);
        MaterialColorPicker picker = view.findViewById(R.id.picker);
        View backButton = view.findViewById(R.id.back);
        TextView title = view.findViewById(R.id.title);
        if (mTitle != null)
            title.setText(mTitle);
        backButton.setOnClickListener((View v) -> {
            picker.closeColor();
        });
        picker.setBackButtonVisibilityCallback((boolean visible) -> {
            boolean isRtl = ViewCompat.getLayoutDirection(title) == ViewCompat.LAYOUT_DIRECTION_RTL;
            if (!visible && backButton.getVisibility() != View.VISIBLE)
                return;
            float titleToXDp = 8.f;
            if (visible) {
                backButton.setVisibility(View.VISIBLE);
                backButton.setAlpha(0.f);
                backButton.animate().setStartDelay(250L).setDuration(500L).alpha(1.f).setListener(null);
                titleToXDp += 48.f;
            } else {
                backButton.animate().setStartDelay(0L).setDuration(500L).alpha(0.f).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        backButton.setVisibility(View.INVISIBLE);
                    }
                });
            }
            float titleToX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, titleToXDp, mContext.getResources().getDisplayMetrics());
            if (isRtl)
                titleToX = ((View) title.getParent()).getWidth() - titleToX - title.getWidth();
            title.animate().setDuration(500L).x(titleToX);
        });
        AlertDialog dialog = new AlertDialog.Builder(mContext, R.style.MaterialColorPickerDialog)
                .setView(view)
                .show();
        picker.setColorPickListener((int color) -> {
            dialog.cancel();
            if (mCallback != null)
                mCallback.onColorPicked(color);
        });
    }

}
