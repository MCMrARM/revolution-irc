package io.mrarm.irc.util.theme.live;

import android.content.Context;
import com.google.android.material.textfield.TextInputEditText;
import android.util.AttributeSet;

import io.mrarm.irc.R;

public class ThemedTextInputEditText extends TextInputEditText {

    protected LiveThemeComponent mThemeComponent;

    public ThemedTextInputEditText(Context context) {
        this(context, null);
    }

    public ThemedTextInputEditText(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }

    public ThemedTextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        ThemedTextView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        ThemedEditText.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedTextInputEditText(Context context, AttributeSet attrs,
                                   LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

}
