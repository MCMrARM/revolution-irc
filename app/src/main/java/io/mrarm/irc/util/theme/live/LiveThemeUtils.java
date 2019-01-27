package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.mrarm.irc.R;

public class LiveThemeUtils {

    private static Method sMethodGetThemeResId;

    public static int getAttribute(TypedArray attrs, int attr) {
        TypedValue typedValue = new TypedValue();
        if (!attrs.getValue(attr, typedValue))
            return 0;
        if (typedValue.type == TypedValue.TYPE_ATTRIBUTE)
            return typedValue.data;
        return 0;
    }

    public static void tintAppCompatDrawable(LiveThemeComponent c, int d,
                                             LiveThemeComponent.ColorStateListApplier applier) {
        try {
            ThemedColorStateList tintList = getAppCompatDrawableTintList(
                    c.getContext().getResources(), d, c.getTheme());
            if (tintList != null) {
                tintList.attachToComponent(c, () -> applier.onColorStateListChanged(
                        tintList.createColorStateList()));
            }
        } catch (Exception e) {
            Log.w("LiveThemeUtils", "tintAppCompatDrawable error");
            e.printStackTrace();
        }
    }

    private static ThemedColorStateList getAppCompatDrawableTintList(Resources r, int d,
                                                                     Resources.Theme t)
            throws IOException, XmlPullParserException {
        if (d == R.drawable.abc_edit_text_material)
            return ThemedColorStateList.createFromXml(r, r.getXml(R.color.abc_tint_edittext), t);
        return null;
    }

    static int getContextThemeWrapperResId(ContextThemeWrapper wrapper) {
        if (sMethodGetThemeResId == null) {
            try {
                sMethodGetThemeResId = Context.class.getDeclaredMethod("getThemeResId");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return (int) sMethodGetThemeResId.invoke(wrapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
