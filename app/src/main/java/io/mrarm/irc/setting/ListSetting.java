package io.mrarm.irc.setting;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.view.View;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;

public class ListSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class, R.layout.settings_list_entry);

    private String[] mOptions;
    private int mSelectedOption;

    private String[] mPrefOptionValues;

    public ListSetting(String name, String[] options, int selectedOption) {
        super(name, null);
        mOptions = options;
        mSelectedOption = selectedOption;
        if (mSelectedOption != -1)
            mValue = mOptions[mSelectedOption];
    }

    public ListSetting(String name, String[] options, String[] values, String selectedOption) {
        super(name, null);
        mOptions = options;
        mPrefOptionValues = values;
        mSelectedOption = getPrefValueIndex(selectedOption);
        if (mSelectedOption != -1)
            mValue = mOptions[mSelectedOption];
    }

    public ListSetting(String name, String[] options, String[] values) {
        this(name, options, values, null);
    }

    public ListSetting linkPreference(SharedPreferences prefs, String pref, String[] optionValues) {
        mPrefOptionValues = optionValues;
        return linkPreference(prefs, pref);
    }

    public ListSetting linkPreference(SharedPreferences prefs, String pref) {
        int i = getPrefValueIndex(prefs.getString(pref, null));
        if (i != -1)
            setSelectedOption(i);
        setAssociatedPreference(prefs, pref);
        return this;
    }

    public ListSetting linkSetting(SharedPreferences prefs, String pref) {
        mSelectedOption = getPrefValueIndex((String) SettingsHelper.getDefaultValue(pref));
        if (mSelectedOption != -1)
            mValue = mOptions[mSelectedOption];
        return linkPreference(prefs, pref);
    }

    protected int getPrefValueIndex(String val) {
        if (val == null)
            return -1;
        for (int i = mPrefOptionValues.length - 1; i >= 0; --i) {
            if (mPrefOptionValues[i].equals(val))
                return i;
        }
        return -1;
    }

    public void setSelectedOption(int index) {
        mSelectedOption = index;
        if (index != -1) {
            mValue = mOptions[mSelectedOption];
            if (hasAssociatedPreference())
                mPreferences.edit()
                        .putString(mPreferenceName, mPrefOptionValues[index])
                        .apply();
        } else {
            mValue = null;
        }
        onUpdated();
    }

    public String[] getOptions() {
        return mOptions;
    }

    public int getSelectedOption() {
        return mSelectedOption;
    }

    public String getSelectedOptionValue() {
        if (mSelectedOption == -1)
            return null;
        return mPrefOptionValues[mSelectedOption];
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
                    .show();
        }

    }

}
