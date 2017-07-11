package io.mrarm.irc.util;

import android.text.Editable;
import android.text.TextWatcher;

public class SimpleTextWatcher implements TextWatcher {

    private OnTextChangedListener mListener;

    public SimpleTextWatcher(OnTextChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        mListener.afterTextChanged(s);
    }

    public interface OnTextChangedListener {

        void afterTextChanged(Editable s);

    }

}
