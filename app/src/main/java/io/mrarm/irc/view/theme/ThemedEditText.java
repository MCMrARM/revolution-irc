package io.mrarm.irc.view.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;

import io.mrarm.irc.R;
import io.mrarm.irc.util.ColorFilterWorkaroundDrawable;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.ThemeHelper;

public class ThemedEditText extends AppCompatEditText {

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


    public ThemedEditText(Context context) {
        super(context);
        install(this);
    }

    public ThemedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        install(this);
    }

    public ThemedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        install(this);
    }

    public static void install(EditText editText) {
        if (ThemeHelper.hasCustomPrimaryColor(editText.getContext())) {
            int accentColor = ThemeHelper.getAccentColor(editText.getContext());
            editText.setBackground(new ColorFilterWorkaroundDrawable(editText.getBackground()));
            setBackgroundActiveColor(editText, accentColor);
            setCursorDrawableColor(editText, accentColor);
        }
    }

    public static void setBackgroundActiveColor(View editText, int accentColor) {
        int normalColor = StyledAttributesHelper.getColor(editText.getContext(),
                R.attr.colorControlNormal, 0);
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
            drawables[0] = editText.getContext().getResources().getDrawable(res).mutate();
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
                Drawable drawable = editText.getContext().getResources().getDrawable(res).mutate();
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                sFieldEditorSelectHandleDrawables[i].set(editor, drawable);
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }
        }
    }


}
