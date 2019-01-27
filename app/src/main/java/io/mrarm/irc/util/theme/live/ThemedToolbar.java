package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ThemedToolbar extends Toolbar {

    private static final int[] THEME_ATTRS = { R.attr.titleTextAppearance };


    protected LiveThemeComponent mThemeComponent;

    public ThemedToolbar(Context context) {
        this(context, null);
    }

    public ThemedToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public ThemedToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedToolbar(Context context, AttributeSet attrs, LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof AppCompatImageButton) {
            mThemeComponent.addColorProperty(R.attr.actionBarTextColorPrimary, (c) ->
                    ImageViewCompat.setImageTintList((ImageView) child, ColorStateList.valueOf(c)));
        } else if (child instanceof ActionMenuView) {
            mThemeComponent.addColorProperty(R.attr.actionBarTextColorPrimary, (c) -> {
                ActionMenuView ch = (ActionMenuView) child;
                Drawable d = DrawableCompat.wrap(ch.getOverflowIcon()).mutate();
                DrawableCompat.setTint(d, c);
                ch.setOverflowIcon(d);
            });
        }
    }

    static void setupTheming(Toolbar toolbar, LiveThemeComponent component, AttributeSet attrs, int defStyleAttr) {
        Resources.Theme t = component.getTheme();
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(toolbar.getContext(), t, attrs, THEME_ATTRS, defStyleAttr);
        StyledAttributesHelper textAppearance = r.obtainChildAttrs(t, R.attr.titleTextAppearance, ThemedTextView.TEXT_APPEARANCE_ATTRS);
        component.addColorAttr(textAppearance, android.R.attr.textColor, toolbar::setTitleTextColor, (ColorStateList l) -> toolbar.setTitleTextColor(l.getDefaultColor()));
        textAppearance.recycle();
        r.recycle();
    }

}
