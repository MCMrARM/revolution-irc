package io.mrarm.irc.util.theme.live;

import android.content.Context;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import android.util.AttributeSet;

import io.mrarm.irc.R;

public class ThemedCheckedTextView extends AppCompatCheckedTextView {

    protected LiveThemeComponent mThemeComponent;

    public ThemedCheckedTextView(Context context) {
        this(context, null);
    }

    public ThemedCheckedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.checkedTextViewStyle);
    }

    public ThemedCheckedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        ThemedTextView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedCheckedTextView(Context context, AttributeSet attrs,
                                   LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

}
