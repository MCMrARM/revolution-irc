package io.mrarm.irc.setting;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Arrays;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.view.ChipsEditText;

public class ChipsEditTextSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    private String[] mItems;
    private String mDefaultText;

    public ChipsEditTextSetting(String name, String[] items, String defaultText) {
        super(name, null);
        mItems = items;
        mDefaultText = defaultText;
    }

    public ChipsEditTextSetting linkPreference(SharedPreferences prefs, String pref) {
        String itms = prefs.getString(pref, null);
        if (itms != null && itms.length() > 0)
            setItems(itms.split(String.valueOf(ChipsEditText.SEPARATOR)));
        setAssociatedPreference(prefs, pref);
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    public ChipsEditTextSetting linkSetting(SharedPreferences prefs, String pref) {
        mItems = (String[]) SettingsHelper.getDefaultValue(pref);
        return linkPreference(prefs, pref);
    }

    public void setItems(String[] items) {
        mItems = items;
        if (hasAssociatedPreference()) {
            StringBuilder b = new StringBuilder();
            for (String item : items) {
                if (b.length() > 0)
                    b.append(ChipsEditText.SEPARATOR);
                b.append(item);
            }
            mPreferences.edit()
                    .putString(mPreferenceName, b.toString())
                    .apply();
        }
        onUpdated();
    }

    public void setDefaultText(String text) {
        mDefaultText = text;
        onUpdated();
    }

    public String[] getItems() {
        return mItems;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<ChipsEditTextSetting> {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(ChipsEditTextSetting entry) {
            super.bind(entry);
            if (entry.getItems() != null && entry.getItems().length > 0) {
                StringBuilder b = new StringBuilder();
                for (String s : entry.getItems()) {
                    if (b.length() > 0)
                        b.append(itemView.getContext().getString(R.string.text_comma));
                    b.append(s);
                }
                setValueText(b.toString());
            } else {
                setValueText(entry.mDefaultText);
            }
        }

        @Override
        public void onClick(View v) {
            ChipsEditTextSetting entry = getEntry();
            View view = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_chip_edit_text, null);
            ChipsEditText text = view.findViewById(R.id.chip_edit_text);
            if (entry.mItems != null)
                text.setItems(Arrays.asList(entry.mItems));
            text.setHint(entry.mDefaultText);
            new AlertDialog.Builder(v.getContext())
                    .setTitle(entry.mName)
                    .setView(view)
                    .setPositiveButton(R.string.action_ok, (DialogInterface di, int i) -> {
                        entry.setItems(text.getItems());
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }

    }

}
