package io.mrarm.irc.view.theme;

import android.content.Context;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.AttributeSet;

public class ThemedRadioButton extends AppCompatRadioButton {

    public ThemedRadioButton(Context context) {
        super(context);
        ThemedCheckBox.install(this, null);
    }

    public ThemedRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        ThemedCheckBox.install(this, attrs);
    }

    public ThemedRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ThemedCheckBox.install(this, attrs);
    }

}
