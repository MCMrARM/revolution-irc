/*
 * Copyright (C) 2014 The Android Open Source Project
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
 *
 * Based on ActionMenuPresenter.OverflowMenuButton
 */

package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import io.mrarm.irc.R;

public class OverflowButton extends AppCompatImageView {

    private final float[] mTempPts = new float[2];

    public OverflowButton(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.actionOverflowButtonStyle);

        setClickable(true);
        setFocusable(true);
        setVisibility(VISIBLE);
        setEnabled(true);

        TooltipCompat.setTooltipText(this, getContentDescription());
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        final boolean changed = super.setFrame(l, t, r, b);

        // Set up the hotspot bounds to be centered on the image.
        final Drawable d = getDrawable();
        final Drawable bg = getBackground();
        if (d != null && bg != null) {
            final int width = getWidth();
            final int height = getHeight();
            final int halfEdge = Math.max(width, height) / 2;
            final int offsetX = getPaddingLeft() - getPaddingRight();
            final int offsetY = getPaddingTop() - getPaddingBottom();
            final int centerX = (width + offsetX) / 2;
            final int centerY = (height + offsetY) / 2;
            DrawableCompat.setHotspotBounds(bg, centerX - halfEdge, centerY - halfEdge,
                    centerX + halfEdge, centerY + halfEdge);
        }

        return changed;
    }
}