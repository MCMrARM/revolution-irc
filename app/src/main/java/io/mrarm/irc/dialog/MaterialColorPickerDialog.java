package io.mrarm.irc.dialog;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import io.mrarm.irc.R;
import io.mrarm.irc.view.MaterialColorPicker;

public class MaterialColorPickerDialog {

    private Context mContext;

    public MaterialColorPickerDialog(Context ctx) {
        mContext = ctx;
    }

    public void show() {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.dialog_material_color_picker, null);
        MaterialColorPicker picker = view.findViewById(R.id.picker);
        view.findViewById(R.id.back).setOnClickListener((View v) -> {
            picker.closeColor();
        });
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setView(view)
                .show();

    }

}
