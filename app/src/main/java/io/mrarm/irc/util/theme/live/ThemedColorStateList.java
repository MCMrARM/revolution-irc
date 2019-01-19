/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mrarm.irc.util.theme.live;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;
import android.util.AttributeSet;
import android.util.StateSet;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;

import io.mrarm.irc.R;

public class ThemedColorStateList {

    private static final int DEFAULT_COLOR = Color.RED;

    private static Field sColorStateListDefColorField;

    private int mDefaultColor;
    private int mDefaultColorI;
    private int[] mColors;
    private int[] mColorAttrs;
    private float[] mAlpha;
    private int[][] mStateSpecs;

    /**
     * Creates a ColorStateList from an XML document using given a set of
     * {@link Resources} and a {@link Resources.Theme}.
     *
     * @param r Resources against which the ColorStateList should be inflated.
     * @param parser Parser for the XML document defining the ColorStateList.
     * @param theme Optional theme to apply to the color state list, may be
     *              {@code null}.
     * @return A new color state list.
     */
    @NonNull
    public static ThemedColorStateList createFromXml(
            @NonNull Resources r, @NonNull XmlPullParser parser, @Nullable Resources.Theme theme)
            throws XmlPullParserException, IOException {
        final AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Seek parser to start tag.
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        return createFromXmlInner(r, parser, attrs, theme);
    }

    /**
     * Create from inside an XML document. Called on a parser positioned at a
     * tag in an XML document, tries to create a ColorStateList from that tag.
     *
     * @throws XmlPullParserException if the current tag is not &lt;selector>
     * @return A new color state list for the current tag.
     */
    @NonNull
    static ThemedColorStateList createFromXmlInner(
            @NonNull Resources r, @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
            @Nullable Resources.Theme theme) throws XmlPullParserException, IOException {
        final String name = parser.getName();
        if (!name.equals("selector")) {
            throw new XmlPullParserException(
                    parser.getPositionDescription() + ": invalid color state list tag " + name);
        }

        final ThemedColorStateList colorStateList = new ThemedColorStateList();
        colorStateList.inflate(r, parser, attrs, theme);
        return colorStateList;
    }


    /**
     * Fill in this object based on the contents of an XML "selector" element.
     */
    private void inflate(@NonNull Resources r, @NonNull XmlPullParser parser,
                         @NonNull AttributeSet attrs, @Nullable Resources.Theme theme)
            throws XmlPullParserException, IOException {
        final int innerDepth = parser.getDepth()+1;
        int depth;
        int type;

//        int changingConfigurations = 0;
        int defaultColor = DEFAULT_COLOR;

//        boolean hasUnresolvedAttrs = false;

        int[][] stateSpecList = new int[20][];
//        int[][] themeAttrsList = new int[stateSpecList.length][];
        int[] colorList = new int[stateSpecList.length];
        int[] colorAttrList = new int[stateSpecList.length];
        float[] alphaList = new float[stateSpecList.length];
        int listSize = 0;

        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG || depth > innerDepth
                    || !parser.getName().equals("item")) {
                continue;
            }

            final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.ColorStateListItem);
//            final int[] themeAttrs = a.extractThemeAttrs();
            final int baseColor = a.getColor(R.styleable.ColorStateListItem_android_color, Color.MAGENTA);
            final int baseColorResId = a.getResourceId(R.styleable.ColorStateListItem_android_color, 0);
            final float alphaMod = a.getFloat(R.styleable.ColorStateListItem_android_alpha, 1.0f);

//            changingConfigurations |= a.getChangingConfigurations();
            a.recycle();


            // Parse all unrecognized attributes as state specifiers.
            int j = 0;
            final int numAttrs = attrs.getAttributeCount();
            int[] stateSpec = new int[numAttrs];
            for (int i = 0; i < numAttrs; i++) {
                final int stateResId = attrs.getAttributeNameResource(i);
                switch (stateResId) {
                    case android.R.attr.color:
                    case android.R.attr.alpha:
                        // Recognized attribute, ignore.
                        break;
                    default:
                        stateSpec[j++] = attrs.getAttributeBooleanValue(i, false)
                                ? stateResId : -stateResId;
                }
            }
            stateSpec = StateSet.trimStateSet(stateSpec, j);

            // Apply alpha modulation. If we couldn't resolve the color or
            // alpha yet, the default values leave us enough information to
            // modulate again during applyTheme().
            final int color = modulateColorAlpha(baseColor, alphaMod);
            if (listSize == 0 || stateSpec.length == 0) {
                defaultColor = color;
                mDefaultColorI = listSize;
            }

//            if (themeAttrs != null) {
//                hasUnresolvedAttrs = true;
//            }

            colorList = AndroidArrayUtils.append(colorList, listSize, color);
            colorAttrList = AndroidArrayUtils.append(colorAttrList, listSize, baseColorResId);
            alphaList = AndroidArrayUtils.append(alphaList, listSize, alphaMod);
//            themeAttrsList = AndroidArrayUtils.append(themeAttrsList, listSize, themeAttrs);
            stateSpecList = AndroidArrayUtils.append(stateSpecList, listSize, stateSpec);
            listSize++;
        }

//        mChangingConfigurations = changingConfigurations;
        mDefaultColor = defaultColor;

//        if (hasUnresolvedAttrs) {
//            mThemeAttrs = new int[listSize][];
//            System.arraycopy(themeAttrsList, 0, mThemeAttrs, 0, listSize);
//        } else {
//            mThemeAttrs = null;
//        }

        mColors = new int[listSize];
        mColorAttrs = new int[listSize];
        mAlpha = new float[listSize];
        mStateSpecs = new int[listSize][];
        System.arraycopy(colorList, 0, mColors, 0, listSize);
        System.arraycopy(colorAttrList, 0, mColorAttrs, 0, listSize);
        System.arraycopy(alphaList, 0, mAlpha, 0, listSize);
        System.arraycopy(stateSpecList, 0, mStateSpecs, 0, listSize);
    }

    public ColorStateList createColorStateList() {
        ColorStateList ret = new ColorStateList(mStateSpecs, mColors);
        if (sColorStateListDefColorField == null) {
            try {
                sColorStateListDefColorField = ColorStateList.class.getDeclaredField("mDefaultColor");
                sColorStateListDefColorField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        if (sColorStateListDefColorField != null) {
            try {
                sColorStateListDefColorField.setInt(ret, mDefaultColor);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public void attachToComponent(LiveThemeComponent component, Runnable changeCb) {
        for (int i = 0; i < mColorAttrs.length; i++) {
            if (mColorAttrs[i] == 0)
                continue;
            final int attrI = i;
            component.addColorProperty(mColorAttrs[i], (c) -> {
                mColors[attrI] = modulateColorAlpha(c, mAlpha[attrI]);
                if (attrI == mDefaultColorI)
                    mDefaultColor = mColors[attrI];
                changeCb.run();
            });
        }
    }

    private int modulateColorAlpha(int baseColor, float alphaMod) {
        if (alphaMod == 1.0f) {
            return baseColor;
        }

        final int baseAlpha = Color.alpha(baseColor);
        final int alpha = MathUtils.clamp((int) (baseAlpha * alphaMod + 0.5f), 0, 255);
        return (baseColor & 0xFFFFFF) | (alpha << 24);
    }

    private static TypedArray obtainAttributes(
            Resources res, Resources.Theme theme, AttributeSet set, int[] attrs) {
        if (theme == null) {
            return res.obtainAttributes(set, attrs);
        }
        return theme.obtainStyledAttributes(set, attrs, 0, 0);
    }

}
