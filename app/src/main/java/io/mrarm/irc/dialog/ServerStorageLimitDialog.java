package io.mrarm.irc.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

import io.mrarm.irc.R;
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;

public class ServerStorageLimitDialog extends AlertDialog {

    private ServerConfigData mServer;
    private SeekBar mSeekBar;

    public ServerStorageLimitDialog(@NonNull Context context, ServerConfigData server) {
        super(context);

        setButton(AlertDialog.BUTTON_POSITIVE, getContext().getString(R.string.action_ok), (DialogInterface di, int i) -> {
            if (mSeekBar.getProgress() == StorageLimitsDialog.SIZES.length)
                mServer.storageLimit = -1L;
            else
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
        mSeekBar.setMax(StorageLimitsDialog.SIZES.length);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                StorageLimitsDialog.updateLabel(mSeekBar, valueText);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        if (mServer.storageLimit == -1L) {
            mSeekBar.setProgress(StorageLimitsDialog.SIZES.length);
        } else if (mServer.storageLimit == 0L) {
            long limit = AppSettings.getStorageLimitGlobal();
            if (limit == -1L)
                mSeekBar.setProgress(StorageLimitsDialog.SIZES.length);
            else
                mSeekBar.setProgress(StorageLimitsDialog.findNearestSizeIndex(limit));
        } else {
            mSeekBar.setProgress(StorageLimitsDialog.findNearestSizeIndex(mServer.storageLimit));
        }
        StorageLimitsDialog.updateLabel(mSeekBar, valueText);
    }
}
