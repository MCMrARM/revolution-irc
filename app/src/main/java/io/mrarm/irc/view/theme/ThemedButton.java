package io.mrarm.irc.view.theme;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.widget.Button;

public class ThemedButton extends AppCompatButton {

    public ThemedButton(Context context) {
        super(context);
    }

    public ThemedButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        install(this, attrs);
    }

    public ThemedButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        install(this, attrs);
    }

    public static void install(Button button, AttributeSet attrs) {
        ThemedView.setupBackground(button, attrs);
        ThemedTextView.setupTextColor(button, attrs);
    }

}
