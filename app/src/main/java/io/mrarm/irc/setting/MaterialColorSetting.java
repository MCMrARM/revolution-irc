package io.mrarm.irc.setting;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;

import io.mrarm.irc.R;
import io.mrarm.irc.dialog.MaterialColorPickerDialog;

public class MaterialColorSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry_color);

    protected int mSelectedColor;
    protected boolean mHasSelectedColor = false;

    public MaterialColorSetting(String name) {
        super(name, null);
    }

    public MaterialColorSetting(String name, int selectedColor) {
        super(name, null);
        mHasSelectedColor = true;
        mSelectedColor = selectedColor;
    }

    public MaterialColorSetting linkPreference(SharedPreferences prefs, String pref) {
        if (prefs.contains(pref))
            setSelectedColor(prefs.getInt(pref, 0));
        setAssociatedPreference(prefs, pref);
        return this;
    }

    public boolean hasSelectedColor() {
        return mHasSelectedColor;
    }

    protected void setSelectedColor(int color, boolean noNotifyRV) {
        mSelectedColor = color;
        mHasSelectedColor = true;
        if (hasAssociatedPreference())
            mPreferences.edit()
                    .putInt(mPreferenceName, color)
                    .apply();
        onUpdated(noNotifyRV);
    }

    public final void setSelectedColor(int color) {
        setSelectedColor(color, false);
    }

    public int getSelectedColor() {
        return mSelectedColor;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<MaterialColorSetting> {

        protected ImageView mColor;

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mColor = itemView.findViewById(R.id.color);
        }

        @Override
        public void bind(MaterialColorSetting entry) {
            super.bind(entry);
            if (entry.hasSelectedColor()) {
                mColor.setColorFilter(entry.getSelectedColor(), PorterDuff.Mode.MULTIPLY);
                setValueText(String.format("#%06x", entry.getSelectedColor() & 0xFFFFFF));
            } else {
                mColor.setColorFilter(0x00000000, PorterDuff.Mode.MULTIPLY);
                setValueText(R.string.value_default);
            }
        }

        @Override
        public void onClick(View v) {
            MaterialColorSetting entry = getEntry();
            MaterialColorPickerDialog dialog = new MaterialColorPickerDialog(v.getContext());
            dialog.setTitle(entry.mName);
            dialog.setColorPickListener(entry::setSelectedColor);
            dialog.show();
        }

    }

}
