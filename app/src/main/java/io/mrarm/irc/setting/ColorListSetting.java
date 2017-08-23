package io.mrarm.irc.setting;

import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;

import io.mrarm.irc.R;
import io.mrarm.irc.dialog.ColorListPickerDialog;

public class ColorListSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry_color);

    private int[] mColors;
    private String[] mColorNames;
    private int mSelectedIndex;
    private boolean mHasDefaultOption = false;

    public ColorListSetting(String name, int[] colors, String[] colorNames, int selectedIndex) {
        super(name, null);
        mColors = colors;
        mColorNames = colorNames;
        mSelectedIndex = selectedIndex;
    }

    public void setSelectedColorIndex(int index) {
        mSelectedIndex = index;
        onUpdated();
    }

    public void setSelectedColor(int color) {
        for (int i = mColors.length - 1; i >= 0; i--) {
            if (mColors[i] == color) {
                setSelectedColorIndex(i);
                return;
            }
        }
        if (!mHasDefaultOption)
            throw new IndexOutOfBoundsException();
        setSelectedColorIndex(-1);
    }

    public int getSelectedColorIndex() {
        return mSelectedIndex;
    }

    public int getSelectedColor() {
        return mColors[mSelectedIndex];
    }

    public void setHasDefaultOption(boolean hasDefaultOption) {
        mHasDefaultOption = hasDefaultOption;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<ColorListSetting> {

        private ImageView mColor;

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mColor = itemView.findViewById(R.id.color);
        }

        @Override
        public void bind(ColorListSetting entry) {
            super.bind(entry);
            if (entry.mSelectedIndex == -1) {
                mColor.setColorFilter(0x00000000, PorterDuff.Mode.MULTIPLY);
                setValueText(R.string.value_default);
            } else {
                mColor.setColorFilter(entry.mColors[entry.mSelectedIndex], PorterDuff.Mode.MULTIPLY);
                setValueText(entry.mColorNames[entry.mSelectedIndex]);
            }
        }

        @Override
        public void onClick(View v) {
            ColorListSetting entry = getEntry();
            ColorListPickerDialog dialog = new ColorListPickerDialog(v.getContext());
            dialog.setTitle(entry.mName);
            dialog.setColors(entry.mColors, entry.mSelectedIndex);
            dialog.setPositiveButton(R.string.action_cancel, null);
            dialog.setNeutralButton(R.string.value_default, (DialogInterface d, int which) -> {
                entry.setSelectedColorIndex(-1);
                dialog.cancel();
            });
            dialog.setOnColorChangeListener((ColorListPickerDialog d, int colorIndex, int color) -> {
                entry.setSelectedColorIndex(colorIndex);
                dialog.cancel();
            });
            dialog.show();
        }

    }

}
