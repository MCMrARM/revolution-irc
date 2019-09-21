package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple RecyclerView decoration which adds gradient-based fake "elevation" to item groups.
 * The shadows are added when two items with different return from isItemElevated are encountered.
 * No padding is added.
 *
 * In this project, you generally will want to use RecyclerViewElevationDecoration instead and
 * implement the callback interface in the adapter.
 */
public abstract class RecyclerViewBaseElevationDecoration extends RecyclerView.ItemDecoration {

    private int mStartHeight;
    private LinearGradient mStartGradient;
    private int mEndHeight;
    private LinearGradient mEndGradient;
    private Paint mPaint;

    public RecyclerViewBaseElevationDecoration(Context context) {
        DisplayMetrics m = context.getResources().getDisplayMetrics();
        mStartHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, m);
        mEndHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, m);
        mStartGradient = new LinearGradient(0, 0, 0, mStartHeight, Color.TRANSPARENT, Color.argb(16, 0, 0, 0), Shader.TileMode.CLAMP);
        mEndGradient = new LinearGradient(0, 0, 0, mEndHeight, Color.argb(40, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP);
        mPaint = new Paint();
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {
        float bgDrawStartY = parent.getBottom();
        boolean bgDrawStartYAssigned = false;
        for (int i = parent.getChildCount() - 1; i >= 0; --i) {
            View v = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(v);
            if (pos != -1 && isItemElevated(pos)) {
                if (pos + 1 >= parent.getAdapter().getItemCount() || !isItemElevated(pos + 1)) {
                    mPaint.setShader(mEndGradient);
                    View v2 = i < parent.getChildCount() - 1 ? parent.getChildAt(i + 1) : null;
                    float ty = v2 != null ? v2.getTranslationY() : 0.f;
                    c.save();
                    c.translate(0, v.getBottom() + ty);
                    c.drawRect(v.getLeft(), 0, v.getRight(), mEndHeight, mPaint);
                    c.restore();

                    bgDrawStartY = v.getBottom() + ty;
                    bgDrawStartYAssigned = true;
                }
                if (pos - 1 < 0 || !isItemElevated(pos - 1)) {
                    mPaint.setShader(mStartGradient);
                    View v2 = i > 0 ? parent.getChildAt(i - 1) : null;
                    float ty = v2 != null ? v2.getTranslationY() : 0.f;
                    c.save();
                    c.translate(0, v.getTop() - mStartHeight + ty);
                    c.drawRect(v.getLeft(), 0, v.getRight(), mStartHeight, mPaint);
                    c.restore();

                    mPaint.setShader(null);
                    mPaint.setColor(0xFFFFFFFF);
                    c.drawRect(0, v.getTop() + ty, parent.getWidth(), bgDrawStartY, mPaint);
                    bgDrawStartYAssigned = false;
                }
            }
        }
        if (bgDrawStartYAssigned) {
            mPaint.setShader(null);
            mPaint.setColor(0xFFFFFFFF);
            c.drawRect(0, 0, parent.getWidth(), bgDrawStartY, mPaint);
        }
    }

    public abstract boolean isItemElevated(int index);

}
