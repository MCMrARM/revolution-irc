package io.mrarm.irc.view;

import android.content.Context;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class BackButtonListenerEditText extends AppCompatEditText {

    private BackButtonListener mListener;

    public BackButtonListenerEditText(Context context) {
        super(context);
    }

    public BackButtonListenerEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackButtonListenerEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mListener != null)
                mListener.onBackButtonPressed();
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setBackButtonListener(BackButtonListener listener) {
        mListener = listener;
    }

    public interface BackButtonListener {

        void onBackButtonPressed();

    }

}