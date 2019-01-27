package io.mrarm.irc.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.appcompat.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.util.TypedValue;

import java.util.Arrays;

public class StyledAttributesHelper {

    public static StyledAttributesHelper obtainStyledAttributes(Context ctx, int[] attributes) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx, ctx.obtainStyledAttributes(attributes), attributes);
    }

    public static StyledAttributesHelper obtainStyledAttributes(
            Context ctx, AttributeSet attributeSet, int[] attributes) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx, ctx.obtainStyledAttributes(attributeSet, attributes),
                attributes);
    }

    public static StyledAttributesHelper obtainStyledAttributes(
            Context ctx, AttributeSet attributeSet, int[] attributes, int defStyleAttr,
            int defStyleRes) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx, ctx.obtainStyledAttributes(attributeSet, attributes,
                defStyleAttr, defStyleRes), attributes);
    }

    public static StyledAttributesHelper obtainStyledAttributes(Context ctx, int resid,
                                                                int[] attributes) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx, ctx.obtainStyledAttributes(resid, attributes),
                attributes);
    }

    public static StyledAttributesHelper obtainStyledAttributes(Context ctx, Resources.Theme th,
                                                                int resid, int[] attributes) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx, th.obtainStyledAttributes(resid, attributes),
                attributes);
    }

    public static StyledAttributesHelper obtainStyledAttributes(
            Context ctx, Resources.Theme th, AttributeSet attributeSet, int[] attributes,
            int defStyleAttr) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx, th.obtainStyledAttributes(attributeSet,
                attributes, defStyleAttr, 0), attributes);
    }

    public static StyledAttributesHelper obtainStyledAttributes(
            Context ctx, Resources.Theme th, AttributeSet attributeSet, int[] attributes,
            int defStyleAttr, int defStyleResId) {
        Arrays.sort(attributes);
        return new StyledAttributesHelper(ctx, th.obtainStyledAttributes(attributeSet,
                attributes, defStyleAttr, defStyleResId), attributes);
    }

    public static int getColor(Context ctx, int attribute, int def) {
        TypedArray ta = ctx.obtainStyledAttributes(new int[] { attribute });
        int ret = ta.getColor(0, def);
        ta.recycle();
        return ret;
    }

    public static int getResourceId(Context ctx, int attribute, int def) {
        TypedArray ta = ctx.obtainStyledAttributes(new int[] { attribute });
        int ret = ta.getResourceId(0, def);
        ta.recycle();
        return ret;
    }

    public static float getFloat(Context ctx, int attribute, float def) {
        TypedArray ta = ctx.obtainStyledAttributes(new int[] { attribute });
        float ret = ta.getFloat(0, def);
        ta.recycle();
        return ret;
    }

    public static int getDimensionPixelSize(Context ctx, int attribute, int def) {
        TypedArray ta = ctx.obtainStyledAttributes(new int[] { attribute });
        int ret = ta.getDimensionPixelSize(0, def);
        ta.recycle();
        return ret;
    }

    private Context mContext;
    private TypedArray mArray;
    private int[] mAttributes;

    public StyledAttributesHelper(Context context, TypedArray ta, int[] attributes) {
        mContext = context;
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
        throw new RuntimeException("Attribute not found: " +
                mContext.getResources().getResourceName(attr));
    }

    public boolean hasValue(int attr) {
        return mArray.hasValue(getAttributeIndex(attr));
    }

    public int getResourceId(int attr, int def) {
        return mArray.getResourceId(getAttributeIndex(attr), def);
    }

    public boolean getValue(int attr, TypedValue def) {
        return mArray.getValue(getAttributeIndex(attr), def);
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

    public ColorStateList getColorStateList(int attr) {
        int index = getAttributeIndex(attr);
        int res = mArray.getResourceId(index, 0);
        if (res != 0) {
            ColorStateList v = AppCompatResources.getColorStateList(mContext, res);
            if (v != null)
                return v;
        }
        return mArray.getColorStateList(index);
    }

    public StyledAttributesHelper obtainChildAttrs(int attr, int[] attrs) {
        return obtainStyledAttributes(mContext, getResourceId(attr, 0), attrs);
    }

    public StyledAttributesHelper obtainChildAttrs(Resources.Theme t, int attr, int[] attrs) {
        Arrays.sort(attrs);
        return new StyledAttributesHelper(mContext, t.obtainStyledAttributes(
                getResourceId(attr, 0), attrs), attrs);
    }

}
