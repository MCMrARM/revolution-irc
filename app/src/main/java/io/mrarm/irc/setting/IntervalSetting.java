package io.mrarm.irc.setting;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;

public class IntervalSetting extends SimpleSetting {

    static final int SPINNER_SECONDS = 0;
    static final int SPINNER_MINUTES = 1;
    static final int SPINNER_HOURS = 2;

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    private int mMinDuration;
    private int mDuration;

    public IntervalSetting(String name, int duration) {
        super(name, null);
        mDuration = duration;
    }

    public IntervalSetting(String name) {
        this(name, 0);
    }

    public IntervalSetting linkPreference(SharedPreferences prefs, String pref) {
        setDuration(prefs.getInt(pref, mDuration));
        setAssociatedPreference(prefs, pref);
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    public IntervalSetting linkSetting(SharedPreferences prefs, String pref) {
        mDuration = (Integer) SettingsHelper.getDefaultValue(pref);
        return linkPreference(prefs, pref);
    }

    public IntervalSetting setMinDuration(int duration) {
        mMinDuration = duration;
        onUpdated();
        return this;
    }

    public void setDuration(int duration) {
        if (duration < mMinDuration)
            duration = mMinDuration;
        mDuration = duration;
        if (hasAssociatedPreference())
            mPreferences.edit()
                    .putInt(mPreferenceName, duration)
                    .apply();
        onUpdated();
    }

    public int getDuration() {
        return mDuration;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<IntervalSetting> {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(IntervalSetting entry) {
            super.bind(entry);
            setValueText(getIntervalAsString(mValue.getContext(), entry.getDuration()));
        }

        @Override
        public void onClick(View v) {
            IntervalSetting entry = getEntry();
            View view = LayoutInflater.from(v.getContext()).inflate(R.layout.dialog_interval, null);
            Spinner spinner = view.findViewById(R.id.duration_type);
            ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(itemView.getContext(),
                    R.layout.simple_spinner_item, android.R.id.text1,
                    itemView.getResources().getStringArray(R.array.duration_types));
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);
            EditText text = view.findViewById(R.id.interval);
            setInterval(spinner, text, entry.getDuration());

            new AlertDialog.Builder(v.getContext())
                    .setTitle(entry.mName)
                    .setView(view)
                    .setPositiveButton(R.string.action_ok, (DialogInterface di, int i) -> {
                        entry.setDuration(getInterval(spinner, text));
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        }

    }

    static void setInterval(Spinner spinner, EditText editText, long interval) {
        if (interval != -1) {
            int spinnerItemId = SPINNER_SECONDS;
            if ((interval % 1000) != 0) {
                editText.setText(String.valueOf(interval / 1000.0));
                spinner.setSelection(spinnerItemId);
            } else {
                interval /= 1000;
                if ((interval % 60) == 0) {
                    interval /= 60;
                    spinnerItemId = SPINNER_MINUTES;
                    if ((interval % 60) == 0) {
                        interval /= 60;
                        spinnerItemId = SPINNER_HOURS;
                    }
                }

                editText.setText(String.valueOf(interval));
            }
            spinner.setSelection(spinnerItemId);
        } else {
            editText.setText("");
            spinner.setSelection(SPINNER_SECONDS);
        }
    }

    static int getInterval(Spinner spinner, EditText editText) {
        int mp = 1;
        switch (spinner.getSelectedItemPosition()) {
            case SPINNER_SECONDS:
                mp = 1000; // seconds
                break;
            case SPINNER_MINUTES:
                mp = 1000 * 60; // minutes
                break;
            case SPINNER_HOURS:
                mp = 1000 * 60 * 60; // hours
                break;
        }
        try {
            return (int) (Double.parseDouble(editText.getText().toString()) * mp);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String getIntervalAsString(Context context, int interval) {
        if (interval != -1) {
            int delay = interval / 1000;
            if ((delay % 60) == 0) {
                delay /= 60;
                if ((delay % 60) == 0) {
                    delay /= 60;
                    return context.getResources().getQuantityString(R.plurals.time_hours, delay, delay);
                }
                return context.getResources().getQuantityString(R.plurals.time_minutes, delay, delay);
            }
            return context.getResources().getQuantityString(R.plurals.time_seconds, delay, delay);
        }
        return null;
    }

}
