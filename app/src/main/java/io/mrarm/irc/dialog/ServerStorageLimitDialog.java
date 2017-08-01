package io.mrarm.irc.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

import io.mrarm.irc.R;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.SettingsHelper;

public class ServerStorageLimitDialog extends AlertDialog {

    private ServerConfigData mServer;
    private SeekBar mSeekBar;

    public ServerStorageLimitDialog(@NonNull Context context, ServerConfigData server) {
        super(context);

        setButton(AlertDialog.BUTTON_POSITIVE, getContext().getString(R.string.action_ok), (DialogInterface di, int i) -> {
            mServer.storageLimit = StorageLimitsDialog.SIZES[mSeekBar.getProgress()] * 1024L * 1024L;
            try {
                ServerConfigManager.getInstance(getContext()).saveServer(server);
            } catch (IOException ignored) {
            }
        });

        mServer = server;
        setView(LayoutInflater.from(context).inflate(R.layout.settings_storage_limit_dialog, null));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSeekBar = findViewById(R.id.seekbar);
        TextView valueText = findViewById(R.id.value);
        mSeekBar.setMax(StorageLimitsDialog.SIZES.length - 1);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        if (mServer.storageLimit == 0L) {
            SettingsHelper settings = SettingsHelper.getInstance(getContext());
            mSeekBar.setProgress(StorageLimitsDialog.findNearestSizeIndex(settings.getStorageLimitGlobal()));
        } else {
            mSeekBar.setProgress(StorageLimitsDialog.findNearestSizeIndex(mServer.storageLimit));
        }
        valueText.setText(StorageLimitsDialog.SIZES[mSeekBar.getProgress()] + " MB");
    }
}
