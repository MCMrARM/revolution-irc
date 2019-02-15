package io.mrarm.irc.setting;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;

public class EditTextSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    private String mText;
    private String mDefaultText;

    public EditTextSetting(String name, String text, String defaultText) {
        super(name, null);
        mText = text;
        mDefaultText = defaultText;
    }

    public EditTextSetting linkPreference(SharedPreferences prefs, String pref) {
        setText(prefs.getString(pref, mText));
        setAssociatedPreference(prefs, pref);
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    public EditTextSetting linkSetting(SharedPreferences prefs, String pref) {
        mText = (String) SettingsHelper.getDefaultValue(pref);
        return linkPreference(prefs, pref);
    }

    public void setText(String text) {
        mText = text;
        if (hasAssociatedPreference())
            mPreferences.edit()
                    .putString(mPreferenceName, text)
                    .apply();
        onUpdated();
    }

    public void setDefaultText(String text) {
        mDefaultText = text;
        onUpdated();
    }

    public String getText() {
        return mText;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<EditTextSetting> {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(EditTextSetting entry) {
            super.bind(entry);
            setValueText(entry.getText() != null && entry.getText().length() > 0 ? entry.mText : entry.mDefaultText);
        }

        @Override
        public void onClick(View v) {
            EditTextSetting entry = getEntry();
            View view = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_edit_text, null);
            EditText text = view.findViewById(R.id.edit_text);
            text.setText(entry.mText);
            text.setHint(entry.mDefaultText);
            new AlertDialog.Builder(v.getContext())
                    .setTitle(entry.mName)
                    .setView(view)
                    .setPositiveButton(R.string.action_ok, (DialogInterface di, int i) -> {
                        entry.setText(text.getText().toString());
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }

    }

}
