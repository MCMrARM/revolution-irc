package io.mrarm.irc.view.theme;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.util.AttributeSet;
import android.widget.CheckedTextView;

public class ThemedCheckedTextView extends AppCompatCheckedTextView {

    public ThemedCheckedTextView(Context context) {
        super(context);
        install(this, null);
    }

    public ThemedCheckedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        install(this, attrs);
    }

    public ThemedCheckedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        install(this, attrs);
    }

    public static void install(CheckedTextView view, AttributeSet attrs) {
        ThemedTextView.install(view, attrs);
    }

}
