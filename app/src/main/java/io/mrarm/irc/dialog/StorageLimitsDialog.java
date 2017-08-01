package io.mrarm.irc.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import io.mrarm.irc.R;

public class StorageLimitsDialog extends Dialog {

    static final int[] SIZES = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 12, 24, 36, 48, 64, 96, 128, 256, 512, 1024, 2048, 3072, 4096 };

    public StorageLimitsDialog(@NonNull Context context) {
        super(context, R.style.Theme_AppCompat_Light);
        setContentView(R.layout.settings_storage_limits);

        SeekBar globalLimitSeekBar = findViewById(R.id.global_limit_seekbar);
        SeekBar serverLimitSeekBar = findViewById(R.id.server_limit_seekbar);

        TextView globalLimitValue = findViewById(R.id.global_limit_value);
        TextView serverLimitValue = findViewById(R.id.server_limit_value);

        globalLimitSeekBar.setMax(SIZES.length);
        globalLimitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                serverLimitSeekBar.setMax(i);
                if (i >= SIZES.length) {
                    globalLimitValue.setText(R.string.pref_storage_no_limit);
                    return;
                }
                globalLimitValue.setText(SIZES[i] + " MB");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        globalLimitSeekBar.setProgress(SIZES.length);

        serverLimitSeekBar.setMax(SIZES.length);
        serverLimitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i >= SIZES.length) {
                    serverLimitValue.setText(R.string.pref_storage_no_limit);
                    return;
                }
                serverLimitValue.setText(SIZES[i] + " MB");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        serverLimitSeekBar.setProgress(SIZES.length);

        ((Toolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener((View v) -> {
            dismiss();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                TypedArray ta = getContext().obtainStyledAttributes(new int[] { R.attr.colorPrimaryDark });
                window.setStatusBarColor(ta.getColor(0, 0));
                ta.recycle();
            }
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(null);
            window.setWindowAnimations(R.style.Animation_AppCompat_Dialog);
        }
    }
}
