package io.mrarm.irc.view.theme;


import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;

public class ThemedTextInputEditText extends TextInputEditText {

    public ThemedTextInputEditText(Context context) {
        super(context);
        ThemedEditText.install(this);
    }

    public ThemedTextInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        ThemedEditText.install(this);
    }

    public ThemedTextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ThemedEditText.install(this);
    }

}
