package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.TextViewCompat;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ThemedTextView extends AppCompatTextView {

    private static final int[] THEME_ATTRS = { android.R.attr.textColor,
            android.R.attr.textColorLink, android.R.attr.textColorHint,
            android.R.attr.textColorHighlight, android.R.attr.textAppearance,
            R.attr.colorControlActivated, R.attr.colorControlNormal,
            android.R.attr.drawableStart, android.R.attr.drawableTop, android.R.attr.drawableEnd,
            android.R.attr.drawableBottom};

    static final int[] TEXT_APPEARANCE_ATTRS = {android.R.attr.textColor,
            android.R.attr.textColorLink, android.R.attr.textColorHint,
            android.R.attr.textColorHighlight};

    private static final int[] DRAWABLE_ATTRS = { android.R.attr.drawableStart,
            android.R.attr.drawableTop, android.R.attr.drawableEnd,
            android.R.attr.drawableBottom };

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
        if (!component.addColorAttr(r, android.R.attr.textColorHighlight, textView::setHighlightColor, (c) -> textView.setHighlightColor(c.getDefaultColor())))
            component.addColorAttr(textAppearance, android.R.attr.textColorHighlight, textView::setHighlightColor, (c) -> textView.setHighlightColor(c.getDefaultColor()));
        for (int i = 0; i < 4; i++)
            handleDrawable(textView, r, DRAWABLE_ATTRS[i], i, component);
        textAppearance.recycle();
        r.recycle();
    }

    private static void handleDrawable(TextView textView, StyledAttributesHelper r, int attr, int index, LiveThemeComponent component) {
        int resId = r.getResourceId(attr, 0);
        if (resId == StyledAttributesHelper.getResourceId(textView.getContext(), android.R.attr.listChoiceIndicatorSingle, 0)) {
            LiveThemeManager.ColorPropertyApplier applier = (c) -> updateCompoundDrawable(textView, index, ThemedCheckBox.createCheckBoxTintStateList(textView.getContext(), component));
            component.addColorAttr(r, R.attr.colorControlActivated, applier);
            component.addColorAttr(r, R.attr.colorControlNormal, applier);
        }
    }

    private static void updateCompoundDrawable(TextView view, int index, ColorStateList tint) {
        Drawable[] drawables = TextViewCompat.getCompoundDrawablesRelative(view);
        drawables[index] = DrawableCompat.wrap(drawables[index].mutate());
        DrawableCompat.setTintList(drawables[index], tint);
        TextViewCompat.setCompoundDrawablesRelative(view, drawables[0], drawables[1], drawables[2], drawables[3]);
    }

}

