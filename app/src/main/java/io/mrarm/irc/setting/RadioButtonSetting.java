package io.mrarm.irc.setting;

import android.view.View;
import android.widget.CompoundButton;

import io.mrarm.irc.R;

public class RadioButtonSetting extends CheckBoxSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_checkbox_entry);

    private Group group;

    public RadioButtonSetting(String name, Group group) {
        super(name, false);
        this.group = group;
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (checked) {
            if (group.selected != null && group.selected != this)
                group.selected.setChecked(false);
            group.selected = this;
        } else if (group.selected == this) {
            group.selected = null;
        }
    }

    public static class Holder extends CheckBoxSetting.Holder {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked)
                getEntry().setChecked(true);
        }

        @Override
        public void onClick(View v) {
            mCheckBox.setChecked(true);
        }
    }


    public static class Group {

        private RadioButtonSetting selected;

    }

}
