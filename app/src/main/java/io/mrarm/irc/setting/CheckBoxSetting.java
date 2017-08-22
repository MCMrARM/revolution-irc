package io.mrarm.irc.setting;

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import io.mrarm.irc.R;

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

    public void setChecked(boolean checked) {
        mChecked = checked;
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
            implements CompoundButton.OnCheckedChangeListener {

        private CheckBox mCheckBox;

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
            mCheckBox.setEnabled(entry.isEnabled());
            mCheckBox.setOnCheckedChangeListener(null);
            mCheckBox.setChecked(entry.isChecked());
            mCheckBox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onClick(View v) {
            mCheckBox.setChecked(!mCheckBox.isChecked());
        }

    }

}
