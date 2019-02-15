package io.mrarm.irc.setting;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.config.ChatSettings;
import io.mrarm.irc.config.SettingsHelper;

public class FontSizeSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    private static final int SEEKBAR_OFFSET = 9;

    private int mFontSize;

    public FontSizeSetting(String name, int value) {
        super(name, null);
        setFontSize(value);
    }

    public FontSizeSetting(String name) {
        this(name, -1);
    }

    public FontSizeSetting linkPreference(SharedPreferences prefs, String pref) {
        setFontSize(prefs.getInt(pref, mFontSize));
        setAssociatedPreference(prefs, pref);
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    public FontSizeSetting linkSetting(SharedPreferences prefs, String pref) {
        mFontSize = (Integer) SettingsHelper.getDefaultValue(pref);
        return linkPreference(prefs, pref);
    }

    public void setFontSize(int value) {
        mFontSize = value;
        if (hasAssociatedPreference())
            mPreferences.edit()
                    .putInt(mPreferenceName, mFontSize)
                    .apply();
        onUpdated();
    }

    public int getFontSize() {
        return mFontSize;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<FontSizeSetting> {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(FontSizeSetting entry) {
            super.bind(entry);

            setValueText(entry.getFontSize() >= 0 ? String.valueOf(entry.getFontSize()) + "sp" :
                    itemView.getContext().getString(R.string.value_default));
        }

        @Override
        public void onClick(View v) {
            FontSizeSetting entry = getEntry();
            View view = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_font_size, null);
            TextView sampleText = view.findViewById(R.id.example_text);
            sampleText.setTypeface(ChatSettings.getFont());
            int fontSize = entry.mFontSize;
            if (fontSize < 0)
                fontSize = (int) (sampleText.getTextSize() /
                        v.getContext().getResources().getDisplayMetrics().scaledDensity);

            SeekBar seekBar = view.findViewById(R.id.font_size_seekbar);
            TextView textSize = view.findViewById(R.id.font_size_text);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    sampleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, progress + SEEKBAR_OFFSET);
                    textSize.setText((progress + SEEKBAR_OFFSET) + "sp");
                    textSize.setTag(progress + SEEKBAR_OFFSET);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            seekBar.setProgress(Math.min(Math.max(fontSize - SEEKBAR_OFFSET, 0), seekBar.getMax()));

            new AlertDialog.Builder(v.getContext())
                    .setTitle(entry.mName)
                    .setView(view)
                    .setPositiveButton(R.string.action_ok, (DialogInterface di, int i) -> {
                        entry.setFontSize((int) textSize.getTag());
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }

    }

}
