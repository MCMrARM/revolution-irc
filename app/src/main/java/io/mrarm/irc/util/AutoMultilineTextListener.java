package io.mrarm.irc.util;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * A class that automatically makes the EditText multiline if there is a newline character inside
 * it.
 */
public class AutoMultilineTextListener implements TextWatcher {

    private EditText mEditText;
    private boolean mMultiline = false;
    private boolean mPossiblyNotMultiline = false;
    private boolean mAlwaysMultiline = false;

    public AutoMultilineTextListener(EditText editText) {
        mEditText = editText;
    }

    public void setAlwaysMultiline(boolean multiline) {
        mAlwaysMultiline = multiline;
        updateMultilineStatus();
    }

    private void updateMultilineStatus() {
        int pos = mEditText.getSelectionStart();
        if (mMultiline || mAlwaysMultiline)
            mEditText.setInputType(mEditText.getInputType()
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        else
            mEditText.setInputType(mEditText.getInputType()
                    & (~InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        mEditText.setSelection(pos);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (mMultiline) {
            for (int i = 0; i < count; i++) {
                if (s.charAt(i) == '\n') {
                    mPossiblyNotMultiline = true;
                    break;
                }
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!mMultiline) {
            for (int i = 0; i < count; i++) {
                if (s.charAt(i) == '\n') {
                    mMultiline = true;
                    updateMultilineStatus();
                    break;
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mPossiblyNotMultiline) {
            boolean found = false;
            for (int i = s.length() - 1; i >= 0; --i) {
                if (s.charAt(i) == '\n') {
                    found = true;
                    break;
                }
            }
            if (!found) {
                mMultiline = false;
                updateMultilineStatus();
            }
        }
    }

}
