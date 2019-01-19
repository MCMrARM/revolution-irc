package io.mrarm.irc.util.theme.live;

import android.content.Context;
import androidx.appcompat.widget.AppCompatButton;
import android.util.AttributeSet;

import io.mrarm.irc.R;

public class ThemedButton extends AppCompatButton {

    protected LiveThemeComponent mThemeComponent;

    public ThemedButton(Context context) {
        this(context, null);
    }

    public ThemedButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.buttonStyle);
    }

    public ThemedButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        ThemedTextView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedButton(Context context, AttributeSet attrs, LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

}
