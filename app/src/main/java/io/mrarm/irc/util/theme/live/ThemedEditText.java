package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import io.mrarm.irc.R;

public class ThemedEditText extends AppCompatEditText {

    protected LiveThemeComponent mThemeComponent;

    public ThemedEditText(Context context) {
        this(context, null);
    }

    public ThemedEditText(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }

    public ThemedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        ThemedTextView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedEditText(Context context, AttributeSet attrs, LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

}
