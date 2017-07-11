package io.mrarm.irc.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.SettingsHelper;

public class FontSizePickerPreference extends DialogPreference {

    private static final int SEEKBAR_OFFSET = 9;

    private int mDialogFontSize = -1;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FontSizePickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public FontSizePickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FontSizePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FontSizePickerPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.dialog_font_size);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView sampleText = (TextView) view.findViewById(R.id.example_text);
        sampleText.setTypeface(SettingsHelper.getInstance(getContext()).getChatFont());
        int fontSize = getPersistedInt((int) (sampleText.getTextSize() /
                getContext().getResources().getDisplayMetrics().scaledDensity));

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.font_size_seekbar);
        TextView textSize = (TextView) view.findViewById(R.id.font_size_text);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sampleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, progress + SEEKBAR_OFFSET);
                textSize.setText((progress + SEEKBAR_OFFSET) + "sp");
                mDialogFontSize = progress + SEEKBAR_OFFSET;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBar.setProgress(Math.min(Math.max(fontSize - SEEKBAR_OFFSET, 0), seekBar.getMax()));

        mDialogFontSize = -1;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mDialogFontSize != -1) {
            if (callChangeListener(mDialogFontSize))
                persistInt(mDialogFontSize);
        }
    }
}
