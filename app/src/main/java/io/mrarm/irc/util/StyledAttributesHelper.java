package io.mrarm.irc.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import java.util.Arrays;

public class StyledAttributesHelper {

    public static StyledAttributesHelper obtainStyledAttributes(Context ctx, int[] attributes) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx.obtainStyledAttributes(attributes), attributes);
    }

    public static StyledAttributesHelper obtainStyledAttributes(
            Context ctx, AttributeSet attributeSet, int[] attributes) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx.obtainStyledAttributes(attributeSet, attributes),
                attributes);
    }

    public static StyledAttributesHelper obtainStyledAttributes(Context ctx, int resid,
                                                                int[] attributes) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx.obtainStyledAttributes(resid, attributes),
                attributes);
    }

    public static int getColor(Context ctx, int attribute, int def) {
        TypedArray ta = ctx.obtainStyledAttributes(new int[] { attribute });
        int ret = ta.getColor(0, def);
        ta.recycle();
        return ret;
    }

    private TypedArray mArray;
    private int[] mAttributes;

    public StyledAttributesHelper(TypedArray ta, int[] attributes) {
        mArray = ta;
        mAttributes = attributes;
    }

    public void recycle() {
        mArray.recycle();
    }

    private int getAttributeIndex(int attr) {
        for (int i = 0; i < mAttributes.length; i++) {
            if (mAttributes[i] == attr) {
                return i;
            }
        }
        throw new RuntimeException("Attribute not found");
    }

    public int getResourceId(int attr, int def) {
        return mArray.getResourceId(getAttributeIndex(attr), def);
    }

    public String getString(int attr) {
        return mArray.getString(getAttributeIndex(attr));
    }

    public boolean getBoolean(int attr, boolean def) {
        return mArray.getBoolean(getAttributeIndex(attr), def);
    }

    public int getColor(int attr, int def) {
        return mArray.getColor(getAttributeIndex(attr), def);
    }

    public Drawable getDrawable(int attr) {
        return mArray.getDrawable(getAttributeIndex(attr));
    }

    public int getDimensionPixelSize(int attr, int def) {
        return mArray.getDimensionPixelSize(getAttributeIndex(attr), def);
    }

}
