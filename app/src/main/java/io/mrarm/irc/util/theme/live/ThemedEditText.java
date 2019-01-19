package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;

import io.mrarm.irc.R;
import io.mrarm.irc.util.ColorFilterWorkaroundDrawable;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ThemedEditText extends AppCompatEditText {

    private static final int[] THEME_ATTRS = { R.attr.colorControlActivated,
            R.attr.colorControlNormal };

    private static Field sFieldTextViewCursorDrawableRes;
    private static Field sFieldTextViewEditor;
    private static Field sFieldEditorCursorDrawable;

    private static final String[] EDITOR_SELECT_HANDLES_DRAWABLE_FIELDS = new String[] {
            "mSelectHandleCenter", "mSelectHandleLeft", "mSelectHandleRight"
    };
    private static final String[] TEXTVIEW_SELECT_HANDLES_RES_FIELDS = new String[] {
            "mTextSelectHandleRes", "mTextSelectHandleLeftRes", "mTextSelectHandleRightRes"
    };

    private static Field[] sFieldEditorSelectHandleDrawables = new Field[EDITOR_SELECT_HANDLES_DRAWABLE_FIELDS.length];
    private static Field[] sFieldTextViewSelectHandleDrawablesRes = new Field[TEXTVIEW_SELECT_HANDLES_RES_FIELDS.length];

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
        setupTheming(this, mThemeComponent, attrs, defStyleAttr);
    }

    public ThemedEditText(Context context, AttributeSet attrs, LiveThemeManager liveThemeManager) {
        this(context, attrs);
        setLiveThemeManager(liveThemeManager);
    }

    public void setLiveThemeManager(LiveThemeManager manager) {
        mThemeComponent.setLiveThemeManager(manager);
    }

    public static void setupTheming(EditText editText, LiveThemeComponent component, AttributeSet attrs, int defStyleAttr) {
        Resources.Theme t = component.getTheme();
        StyledAttributesHelper r = StyledAttributesHelper.obtainStyledAttributes(editText.getContext(), t, attrs, THEME_ATTRS, defStyleAttr);
        component.addColorAttr(r, R.attr.colorControlActivated, (c) -> setCursorDrawableColor(editText, c));
        component.addColorAttr(r, R.attr.colorControlActivated, (c) -> setBackgroundActiveColor(editText, component));
        component.addColorAttr(r, R.attr.colorControlNormal, (c) -> setBackgroundActiveColor(editText, component));
        r.recycle();
    }

    public static void setBackgroundActiveColor(View editText, LiveThemeComponent component) {
        LiveThemeManager lmgr = component.getLiveThemeManager();
        if (editText.getBackground() == null || lmgr == null)
            return;
        int accentColor = lmgr.getColor(/* R.attr.colorControlActivated */ R.attr.colorAccent);
        int normalColor = lmgr.getColor(/* R.attr.colorControlNormal */ android.R.attr.textColorSecondary);
        int disabledColor = ColorUtils.setAlphaComponent(normalColor, (int) (255.f *
                StyledAttributesHelper.getFloat(editText.getContext(), android.R.attr.disabledAlpha, 1.f)));
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{-android.R.attr.state_pressed, -android.R.attr.state_focused},
                new int[]{}
        };
        int[] colors = new int[]{
                disabledColor,
                normalColor,
                accentColor
        };
        ColorStateList list = new ColorStateList(states, colors);
        editText.setBackground(new ColorFilterWorkaroundDrawable(editText.getBackground()));
        ViewCompat.setBackgroundTintList(editText, list);
    }

    // https://stackoverflow.com/questions/11554078/set-textcursordrawable-programmatically/#26544231
    // https://stackoverflow.com/questions/40889455/how-to-change-color-of-the-bubbleunder-cursor-on-editview-programatically/#44333069
    public static void setCursorDrawableColor(EditText editText, int color) {
        Object editor;
        try {
            if (sFieldTextViewEditor == null) {
                sFieldTextViewEditor = TextView.class.getDeclaredField("mEditor");
                sFieldTextViewEditor.setAccessible(true);
            }
            editor = sFieldTextViewEditor.get(editText);
        } catch (Throwable ignored) {
            return;
        }
        try {
            if (sFieldTextViewCursorDrawableRes == null) {
                sFieldTextViewCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
                sFieldTextViewCursorDrawableRes.setAccessible(true);
            }
            int res = sFieldTextViewCursorDrawableRes.getInt(editText);
            if (sFieldEditorCursorDrawable == null) {
                sFieldEditorCursorDrawable = editor.getClass().getDeclaredField("mCursorDrawable");
                sFieldEditorCursorDrawable.setAccessible(true);
            }
            Drawable[] drawables = new Drawable[2];
            drawables[0] = ContextCompat.getDrawable(editText.getContext(), res).mutate();
            drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
            drawables[1] = drawables[0];
            sFieldEditorCursorDrawable.set(editor, drawables);
        } catch (Throwable ignored) {
        }
        for (int i = 0; i < EDITOR_SELECT_HANDLES_DRAWABLE_FIELDS.length; i++) {
            try {
                if (sFieldTextViewSelectHandleDrawablesRes[i] == null) {
                    sFieldTextViewSelectHandleDrawablesRes[i] = TextView.class.getDeclaredField(
                            TEXTVIEW_SELECT_HANDLES_RES_FIELDS[i]);
                    sFieldTextViewSelectHandleDrawablesRes[i].setAccessible(true);
                }
                int res = sFieldTextViewSelectHandleDrawablesRes[i].getInt(editText);
                if (sFieldEditorSelectHandleDrawables[i] == null) {
                    sFieldEditorSelectHandleDrawables[i] = editor.getClass().getDeclaredField(
                            EDITOR_SELECT_HANDLES_DRAWABLE_FIELDS[i]);
                    sFieldEditorSelectHandleDrawables[i].setAccessible(true);
                }
                Drawable drawable = ContextCompat.getDrawable(editText.getContext(), res);
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }
        }
    }

}
