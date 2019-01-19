package io.mrarm.irc.util.theme.live;

import android.content.Context;
import com.google.android.material.textfield.TextInputLayout;
import android.util.AttributeSet;

public class ThemedTextInputLayout extends TextInputLayout {

    protected LiveThemeComponent mThemeComponent;

    public ThemedTextInputLayout(Context context) {
        this(context, null);
    }

    public ThemedTextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemedTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedTextInputLayout(Context context, AttributeSet attrs,
                                   LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

}
