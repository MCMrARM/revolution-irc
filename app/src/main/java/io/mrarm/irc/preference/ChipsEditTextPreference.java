package io.mrarm.irc.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.view.ChipsEditText;

public class ChipsEditTextPreference extends DialogPreference {

    private ChipsEditText mEditText;

    public ChipsEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ChipsEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    @Override
    protected View onCreateDialogView() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_chip_edit_text, null);
        mEditText = v.findViewById(R.id.chip_edit_text);
        return v;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mEditText.setItems(getValue());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mEditText.clearFocus();
            setValue(Arrays.asList(mEditText.getItems()));
        }
        mEditText = null;
    }

    public List<String> getValue() {
        String ret = getPersistedString(null);
        if (ret == null)
            return null;
        return Arrays.asList(ret.split("\n"));
    }

    public void setValue(List<String> value) {
        if (!callChangeListener(value))
            return;
        StringBuilder builder = new StringBuilder();
        for (String v : value) {
            if (builder.length() > 0)
                builder.append('\n');
            builder.append(v);
        }
        persistString(builder.toString());
    }

}
