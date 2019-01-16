package io.mrarm.irc.util.theme.live;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import io.mrarm.irc.R;

public class LiveThemeUtils {

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
            Log.i("LiveThemeUtils", "Test " + c.getContext().getResources().getResourceName(d));
            ThemedColorStateList tintList = getAppCompatDrawableTintList(
                    c.getContext().getResources(), d, c.getTheme());
            if (tintList != null) {
                tintList.attachToComponent(c, () -> applier.onColorStateListChanged(
                        tintList.createColorStateList()));
            }
        } catch (Exception e) {
            Log.w("LiveThemeUtils", "tintAppCompatDrawable error");
            e.printStackTrace();;
        }
    }

    private static ThemedColorStateList getAppCompatDrawableTintList(Resources r, int d,
                                                                     Resources.Theme t)
            throws IOException, XmlPullParserException {
        if (d == R.drawable.abc_edit_text_material)
            return ThemedColorStateList.createFromXml(r, r.getXml(R.color.abc_tint_edittext), t);
        return null;
    }

}
