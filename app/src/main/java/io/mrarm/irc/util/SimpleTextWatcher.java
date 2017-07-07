package io.mrarm.irc.util;

public class SimpleTextWatcher extends StubTextWatcher {

    private OnTextChangedListener mListener;

    public SimpleTextWatcher(OnTextChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mListener.onTextChanged(s, start, before, count);
    }

    public interface OnTextChangedListener {

        void onTextChanged(CharSequence s, int start, int before, int count);

    }

}
