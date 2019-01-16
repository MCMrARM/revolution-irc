package io.mrarm.irc.util.theme.live;

import android.content.res.TypedArray;
import android.util.TypedValue;

public class LiveThemeUtils {

    public static int getAttribute(TypedArray attrs, int attr) {
        TypedValue typedValue = new TypedValue();
        if (!attrs.getValue(attr, typedValue))
            return 0;
        if (typedValue.type == TypedValue.TYPE_ATTRIBUTE)
            return typedValue.data;
        return 0;
    }

}
