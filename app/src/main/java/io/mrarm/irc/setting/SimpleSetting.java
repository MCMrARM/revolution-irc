package io.mrarm.irc.setting;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;

public class SimpleSetting extends SettingsListAdapter.Entry {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    protected String mName;
    protected CharSequence mValue;
    protected boolean mEnabled = true;
    private List<SettingsListAdapter.SettingChangedListener> mListeners = new ArrayList<>();

    protected SharedPreferences mPreferences;
    protected String mPreferenceName;

    public SimpleSetting(String name, CharSequence value) {
        mName = name;
        mValue = value;
    }

    public SimpleSetting requires(CheckBoxSetting setting) {
        setEnabled(setting.isChecked());
        setting.addListener((EntryRecyclerViewAdapter.Entry entry) -> {
            setEnabled(((CheckBoxSetting) entry).isChecked());
        });
        return this;
    }

    protected void setAssociatedPreference(SharedPreferences prefs, String pref) {
        mPreferences = prefs;
        mPreferenceName = pref;
    }

    protected boolean hasAssociatedPreference() {
        return mPreferences != null;
    }

    public String getName() {
        return mName;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
        onUpdated();
    }

    public SimpleSetting addListener(SettingsListAdapter.SettingChangedListener listener) {
        mListeners.add(listener);
        return this;
    }

    public void removeListener(SettingsListAdapter.SettingChangedListener listener) {
        mListeners.remove(listener);
    }

    @Override
    protected void onUpdated(boolean doNotNotifyRV) {
        super.onUpdated(doNotNotifyRV);
        for (SettingsListAdapter.SettingChangedListener listener : mListeners)
            listener.onSettingChanged(this);
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder<T extends SimpleSetting>
            extends SettingsListAdapter.SettingsEntryHolder<T> implements View.OnClickListener {

        protected TextView mName;
        protected TextView mValue;

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);

            mName = itemView.findViewById(R.id.name);
            mValue = itemView.findViewById(R.id.value);
            itemView.setOnClickListener(this);
        }

        @Override
        public void bind(T entry) {
            itemView.setEnabled(entry.mEnabled);
            mName.setEnabled(entry.mEnabled);
            mName.setText(entry.mName);
            if (mValue != null) {
                mValue.setEnabled(entry.mEnabled);
                setValueText(entry.mValue);
            }
        }

        protected void setValueText(CharSequence text) {
            if (mValue == null)
                return;
            mValue.setVisibility(text == null ? View.GONE : View.VISIBLE);
            mValue.setText(text);
        }

        protected void setValueText(int textId) {
            if (mValue == null)
                return;
            mValue.setVisibility(View.VISIBLE);
            mValue.setText(textId);
        }

        @Override
        public void onClick(View v) {
            //
        }

    }

}
