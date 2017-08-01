package io.mrarm.irc.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.SeekBar;
import android.widget.TextView;

import io.mrarm.irc.R;

public class ServerStorageLimitDialog extends AlertDialog {

    public ServerStorageLimitDialog(@NonNull Context context) {
        super(context);

        setButton(AlertDialog.BUTTON_POSITIVE, getContext().getString(R.string.action_ok), (DialogInterface di, int i) -> {
            //
        });
        setView(LayoutInflater.from(context).inflate(R.layout.settings_storage_limit_dialog, null));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SeekBar seekBar = findViewById(R.id.seekbar);
        TextView valueText = findViewById(R.id.value);
        seekBar.setMax(StorageLimitsDialog.SIZES.length - 1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                valueText.setText(StorageLimitsDialog.SIZES[i] + " MB");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBar.setProgress(StorageLimitsDialog.SIZES.length);
    }
}
