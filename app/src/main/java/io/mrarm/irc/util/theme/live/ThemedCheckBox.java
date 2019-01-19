package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.CompoundButtonCompat;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ThemedCheckBox extends AppCompatCheckBox {

    private static final int[] THEME_ATTRS = { R.attr.colorControlActivated,
            R.attr.colorControlNormal };

    protected LiveThemeComponent mThemeComponent;

    public ThemedCheckBox(Context context) {
        this(context, null);
    }

    public ThemedCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.checkboxStyle);
    }

    public ThemedCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mThemeComponent = new LiveThemeComponent(context);
        ThemedView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        ThemedTextView.setupTheming(this, mThemeComponent, attrs, defStyleAttr);
        setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedCheckBox(Context context, AttributeSet attrs, LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }


    public static void setupTheming(CompoundButton checkBox, LiveThemeComponent component, AttributeSet attrs, int defStyleAttr) {
        Resources.Theme t = component.getTheme();
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(checkBox.getContext(), t, attrs, THEME_ATTRS, defStyleAttr);
        component.addColorAttr(r, R.attr.colorControlActivated, (c) -> updateCheckBoxColor(checkBox, component));
        component.addColorAttr(r, R.attr.colorControlNormal, (c) -> updateCheckBoxColor(checkBox, component));
        r.recycle();
    }

    static void updateCheckBoxColor(CompoundButton checkBox, LiveThemeComponent component) {
        CompoundButtonCompat.setButtonTintList(checkBox,
                createCheckBoxTintStateList(checkBox.getContext(), component));
        if (Build.VERSION.SDK_INT >= 21 && checkBox.getBackground() instanceof RippleDrawable) {
            ((RippleDrawable) checkBox.getBackground()).setColor(
                    createCheckBoxRippleTintStateList(checkBox.getContext(), component));
        }
    }

    static ColorStateList createCheckBoxTintStateList(Context ctx, LiveThemeComponent component) {
        LiveThemeManager lmgr = component.getLiveThemeManager();
        int accentColor = lmgr.getColor(/* R.attr.colorControlActivated */ R.attr.colorAccent);
        int normalColor = lmgr.getColor(/* R.attr.colorControlNormal */ android.R.attr.textColorSecondary);
        int disabledColor = ColorUtils.setAlphaComponent(normalColor, (int) (255.f *
                StyledAttributesHelper.getFloat(ctx, android.R.attr.disabledAlpha, 1.f)));
        return new ColorStateList(new int[][] {
                new int[] { -android.R.attr.state_enabled },
                new int[] { android.R.attr.state_checked },
                new int[] { }
        }, new int[] {
                disabledColor,
                accentColor,
                normalColor
        });
    }

    static ColorStateList createCheckBoxRippleTintStateList(Context ctx, LiveThemeComponent component) {
        LiveThemeManager lmgr = component.getLiveThemeManager();
        int accentColor = lmgr.getColor(/* R.attr.colorControlActivated */ R.attr.colorAccent);
        int normalColor = StyledAttributesHelper.getColor(ctx, R.attr.colorControlHighlight, 0);
        int accentAlphaColor = ColorUtils.setAlphaComponent(accentColor, (int) (255.f * 0.26f)); // highlight_alpha_material_colored
        return new ColorStateList(new int[][] {
                new int[] { android.R.attr.state_enabled, android.R.attr.state_checked },
                new int[] { }
        }, new int[] {
                accentAlphaColor,
                normalColor
        });
    }


}
