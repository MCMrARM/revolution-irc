// This file is under the public domain. If you want to use it in your project, go ahead.
package io.mrarm.irc.util;

import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * StateListDrawable (or more generally, DrawableContainer) has a bug where it is impossible to use
 * a tint list after ever setting a color filter, even if clearColorFilter is called before setting
 * the tint list. This happens because clearColorFilter calls setColorFilter(null) which always sets
 * the mHasColorFilter member to true (see [1]), and setting the tint list on other drawables is
 * dependent on this being false (see [2]).
 *
 * This workarounds this bug by creating a new drawable when setting a color filter, and restoring
 * the original one when clearing the color filter.
 *
 * [1] https://github.com/android/platform_frameworks_base/blob/4a81674b45b7250c4e2a80330371f7aa1c066d05/graphics/java/android/graphics/drawable/DrawableContainer.java#L173
 * [2] https://github.com/android/platform_frameworks_base/blob/4a81674b45b7250c4e2a80330371f7aa1c066d05/graphics/java/android/graphics/drawable/DrawableContainer.java#L538
 *     https://github.com/android/platform_frameworks_base/blob/4a81674b45b7250c4e2a80330371f7aa1c066d05/graphics/java/android/graphics/drawable/DrawableContainer.java#L543
 */
public class ColorFilterWorkaroundDrawable extends DrawableWrapper {

    private ConstantState mConstantState;
    private Drawable mOriginalDrawable;
    private Drawable mMutatedDrawable;

    public ColorFilterWorkaroundDrawable(Drawable drawable) {
        super(drawable);
        mConstantState = new ConstantState(drawable);
        mOriginalDrawable = drawable;
        mMutatedDrawable = drawable.getConstantState().newDrawable().mutate();
    }

    private void beforeFilterSet(boolean hasFilter) {
        if (hasFilter) {
            if (getWrappedDrawable() == mOriginalDrawable) {
                mMutatedDrawable.setBounds(mOriginalDrawable.getBounds());
                mMutatedDrawable.setState(mOriginalDrawable.getState());
                mMutatedDrawable.setVisible(mOriginalDrawable.isVisible(), true);
                mOriginalDrawable.setVisible(false, false);
                setWrappedDrawable(mMutatedDrawable);
                invalidateSelf();
            }
        } else {
            if (getWrappedDrawable() != mOriginalDrawable) {
                mOriginalDrawable.setBounds(mMutatedDrawable.getBounds());
                mOriginalDrawable.setState(mMutatedDrawable.getState());
                mOriginalDrawable.setVisible(mMutatedDrawable.isVisible(), true);
                mMutatedDrawable.setVisible(false, false);
                setWrappedDrawable(mOriginalDrawable);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mOriginalDrawable.setAlpha(alpha);
        mMutatedDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        beforeFilterSet(filter != null);
        if (filter != null)
            super.setColorFilter(filter);
    }

    @Override
    public void setColorFilter(int color, @NonNull PorterDuff.Mode mode) {
        beforeFilterSet(true);
        super.setColorFilter(color, mode);
    }

    @Nullable
    @Override
    public Drawable.ConstantState getConstantState() {
        return mConstantState;
    }

    public static class ConstantState extends Drawable.ConstantState {

        private Drawable mDrawable;

        public ConstantState(Drawable wrappedDrawable) {
            mDrawable = wrappedDrawable;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new ColorFilterWorkaroundDrawable(mDrawable);
        }

        @Override
        public int getChangingConfigurations() {
            return mDrawable.getChangingConfigurations();
        }

    }

}
