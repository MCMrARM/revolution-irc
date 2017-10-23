package io.mrarm.irc.view.theme;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;

public class ThemedTextInputEditText extends TextInputEditText {

    public ThemedTextInputEditText(Context context) {
        super(context);
        ThemedEditText.install(this, null);
    }

    public ThemedTextInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        ThemedEditText.install(this, attrs);
    }

    public ThemedTextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ThemedEditText.install(this, attrs);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.O)
    public int getAutofillType() {
        return AUTOFILL_TYPE_NONE;
    }

}
