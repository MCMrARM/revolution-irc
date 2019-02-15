package io.mrarm.irc.setting;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.CompoundButton;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;

public class CheckBoxSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_checkbox_entry);

    private boolean mChecked;

    public CheckBoxSetting(String name, boolean checked) {
        super(name, null);
        mChecked = checked;
    }

    public CheckBoxSetting(String name, String desc, boolean checked) {
        super(name, desc);
        mChecked = checked;
    }

    public CheckBoxSetting(String name, String desc) {
        this(name, desc, false);
    }

    public CheckBoxSetting linkPreference(SharedPreferences prefs, String pref) {
        setChecked(prefs.getBoolean(pref, mChecked));
        setAssociatedPreference(prefs, pref);
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    public CheckBoxSetting linkSetting(SharedPreferences prefs, String pref) {
        mChecked = (Boolean) SettingsHelper.getDefaultValue(pref);
        return linkPreference(prefs, pref);
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (hasAssociatedPreference())
            mPreferences.edit()
                    .putBoolean(mPreferenceName, checked)
                    .apply();
        onUpdated();
    }

    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<CheckBoxSetting>
            implements CompoundButton.OnCheckedChangeListener,
            SettingsListAdapter.SettingChangedListener {

        protected CompoundButton mCheckBox;
        private SimpleSetting oldEntry;

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mCheckBox = itemView.findViewById(R.id.check);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            getEntry().setChecked(!getEntry().isChecked());
        }

        @Override
        public void bind(CheckBoxSetting entry) {
            super.bind(entry);
            if (oldEntry != null)
                oldEntry.removeListener(this);
            oldEntry = entry;
            entry.addListener(this);
            onSettingChanged(entry);
        }

        @Override
        public void onSettingChanged(EntryRecyclerViewAdapter.Entry entry) {
            mCheckBox.setEnabled(getEntry().isEnabled());
            mCheckBox.setOnCheckedChangeListener(null);
            mCheckBox.setChecked(getEntry().isChecked());
            mCheckBox.setOnCheckedChangeListener(this);
        }

        @Override
        public void unbind() {
            super.unbind();
            if (oldEntry != null) {
                oldEntry.removeListener(this);
                oldEntry = null;
            }
        }

        @Override
        public void onClick(View v) {
            mCheckBox.setChecked(!mCheckBox.isChecked());
        }
    }

}
