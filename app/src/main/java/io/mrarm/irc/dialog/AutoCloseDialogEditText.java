package io.mrarm.irc.dialog;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class AutoCloseDialogEditText extends android.support.v7.widget.AppCompatEditText {

    private Dialog mDialog;

    public AutoCloseDialogEditText(Context context) {
        super(context);
    }

    public AutoCloseDialogEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoCloseDialogEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mDialog.dismiss();
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setDialog(Dialog dialog) {
        mDialog = dialog;
    }

}
