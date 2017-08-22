package io.mrarm.irc.setting;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;

import io.mrarm.irc.R;

public class ListSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class, R.layout.settings_list_entry);

    private String[] mOptions;
    private int mSelectedOption;

    public ListSetting(String name, String[] options, int selectedOption) {
        super(name, null);
        mOptions = options;
        mSelectedOption = selectedOption;
        mValue = mOptions[mSelectedOption];
    }

    public void setSelectedOption(int index) {
        mSelectedOption = index;
        if (index == -1)
            mValue = null;
        else
            mValue = mOptions[mSelectedOption];
        onUpdated();
    }

    public String[] getOptions() {
        return mOptions;
    }

    public int getSelectedOption() {
        return mSelectedOption;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<ListSetting> {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void onClick(View v) {
            ListSetting listEntry = getEntry();
            new AlertDialog.Builder(v.getContext())
                    .setTitle(listEntry.mName)
                    .setSingleChoiceItems(listEntry.mOptions, listEntry.mSelectedOption,
                            (DialogInterface i, int which) -> {
                                listEntry.setSelectedOption(which);
                                i.cancel();
                            })
                    .setPositiveButton(R.string.action_cancel, null)
                    .show();
        }

    }

}
