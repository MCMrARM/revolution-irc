package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.TextView;

import io.mrarm.irc.util.StyledAttributesHelper;

public class ThemedTextView extends AppCompatTextView {

    private static final int[] THEME_ATTRS = { android.R.attr.textColor,
            android.R.attr.textColorLink, android.R.attr.textColorHint,
            android.R.attr.textColorHighlight, android.R.attr.textAppearance };

    private static final int[] TEXT_APPEARANCE_ATTRS = {android.R.attr.textColor,
            android.R.attr.textColorLink, android.R.attr.textColorHint,
            android.R.attr.textColorHighlight};

    protected LiveThemeComponent mThemeComponent;

    public ThemedTextView(Context context) {
        this(context, null);
    }

    public ThemedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public ThemedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedTextView(Context context, AttributeSet attrs, LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

    static void setupTheming(TextView textView, LiveThemeComponent component, AttributeSet attrs, int defStyleAttr) {
        Resources.Theme t = component.getTheme();
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(textView.getContext(), t, attrs, THEME_ATTRS, defStyleAttr);
        StyledAttributesHelper textAppearance = r.obtainChildAttrs(t, android.R.attr.textAppearance, TEXT_APPEARANCE_ATTRS);
        if (!component.addColorAttr(r, android.R.attr.textColor, textView::setTextColor, textView::setTextColor))
            component.addColorAttr(textAppearance, android.R.attr.textColor, textView::setTextColor, textView::setTextColor);
        if (!component.addColorAttr(r, android.R.attr.textColorLink, textView::setLinkTextColor, textView::setLinkTextColor))
            component.addColorAttr(textAppearance, android.R.attr.textColorLink, textView::setLinkTextColor, textView::setLinkTextColor);
        if (!component.addColorAttr(r, android.R.attr.textColorHint, textView::setHintTextColor, textView::setHintTextColor))
            component.addColorAttr(textAppearance, android.R.attr.textColorHint, textView::setHintTextColor, textView::setHintTextColor);
        if (!component.addColorAttr(r, android.R.attr.textColorHighlight, textView::setHighlightColor))
            component.addColorAttr(textAppearance, android.R.attr.textColorHighlight, textView::setHighlightColor);
        textAppearance.recycle();
        r.recycle();
    }

}

