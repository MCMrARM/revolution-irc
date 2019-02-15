package io.mrarm.irc.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.util.StyledAttributesHelper;

public class StorageLimitsDialog extends Dialog {

    public static final long DEFAULT_LIMIT_GLOBAL = 24L * 1024L * 1024L;
    public static final long DEFAULT_LIMIT_SERVER = 24L * 1024L * 1024L;

    static final int[] SIZES = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 12, 24, 36, 48, 64, 96, 128, 256, 512, 1024, 2048, 3072, 4096 };

    private SeekBar mGlobalLimitSeekBar;
    private SeekBar mServerLimitSeekBar;

    public StorageLimitsDialog(@NonNull Context context) {
        super(context, R.style.Theme_AppCompat_Light);
        setContentView(R.layout.settings_storage_limits);

        mGlobalLimitSeekBar = findViewById(R.id.global_limit_seekbar);
        mServerLimitSeekBar = findViewById(R.id.server_limit_seekbar);

        TextView globalLimitValue = findViewById(R.id.global_limit_value);
        TextView serverLimitValue = findViewById(R.id.server_limit_value);

        mGlobalLimitSeekBar.setMax(SIZES.length);
        mGlobalLimitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mServerLimitSeekBar.setMax(i);
                updateLabel(mGlobalLimitSeekBar, globalLimitValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mServerLimitSeekBar.setMax(SIZES.length);
        mServerLimitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateLabel(mServerLimitSeekBar, serverLimitValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        ((Toolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener((View v) -> {
            dismiss();
        });

        long v = AppSettings.getStorageLimitGlobal();
        if (v == -1L)
            mGlobalLimitSeekBar.setProgress(SIZES.length);
        else
            mGlobalLimitSeekBar.setProgress(findNearestSizeIndex(v));
        mServerLimitSeekBar.setMax(mGlobalLimitSeekBar.getProgress());
        v = AppSettings.getStorageLimitServer();
        if (v == -1L)
            mServerLimitSeekBar.setProgress(SIZES.length);
        else
            mServerLimitSeekBar.setProgress(findNearestSizeIndex(v));
        updateLabel(mGlobalLimitSeekBar, globalLimitValue);
        updateLabel(mServerLimitSeekBar, serverLimitValue);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= 21) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                window.setStatusBarColor(StyledAttributesHelper.getColor(getContext(), R.attr.colorPrimaryDark, 0));
            }
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(null);
            window.setWindowAnimations(R.style.Animation_AppCompat_Dialog);
        }
    }

    @Override
    public void dismiss() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        if (mGlobalLimitSeekBar.getProgress() == SIZES.length)
            editor.putLong(AppSettings.PREF_STORAGE_LIMIT_GLOBAL, -1L);
        else
            editor.putLong(AppSettings.PREF_STORAGE_LIMIT_GLOBAL, SIZES[mGlobalLimitSeekBar.getProgress()] * 1024L * 1024L);
        if (mServerLimitSeekBar.getProgress() == SIZES.length)
            editor.putLong(AppSettings.PREF_STORAGE_LIMIT_SERVER, -1L);
        else
            editor.putLong(AppSettings.PREF_STORAGE_LIMIT_SERVER, SIZES[mServerLimitSeekBar.getProgress()] * 1024L * 1024L);
        editor.commit();

        super.dismiss();
    }

    public static void updateLabel(SeekBar seekBar, TextView label) {
        if (seekBar.getProgress() >= SIZES.length) {
            label.setText(R.string.pref_storage_no_limit);
            return;
        }
        label.setText(SIZES[seekBar.getProgress()] + " MB");
    }

    public static int findNearestSizeIndex(long val) {
        long nearestVal = -1L;
        int nearestI = -1;
        for (int i = 0; i < SIZES.length; i++) {
            long v = SIZES[i] * 1024L * 1024L;
            if (nearestVal == -1L || Math.abs(v - val) < Math.abs(nearestVal - val)) {
                nearestVal = v;
                nearestI = i;
            }
        }
        return nearestI;
    }

}
