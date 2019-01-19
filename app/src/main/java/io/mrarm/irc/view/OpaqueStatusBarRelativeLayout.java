package io.mrarm.irc.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import io.mrarm.irc.R;

public class OpaqueStatusBarRelativeLayout extends RelativeLayout {

    private Drawable mInsetDrawable;
    private int mTopInset;
    private Rect mTempRect = new Rect();

    public OpaqueStatusBarRelativeLayout(Context context) {
        this(context, null);
    }

    public OpaqueStatusBarRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OpaqueStatusBarRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.OpaqueStatusBarRelativeLayout, defStyleAttr, 0);
        mInsetDrawable =
                ta.getDrawable(R.styleable.OpaqueStatusBarRelativeLayout_colorPrimaryDark);
        ta.recycle();

        setWillNotDraw(false);

        ViewCompat.setOnApplyWindowInsetsListener(this, (View v, WindowInsetsCompat insets) -> {
            mTopInset = insets.getSystemWindowInsetTop();
            setPadding(0, mTopInset, 0, 0);
            ViewCompat.postInvalidateOnAnimation(this);
            return insets.consumeSystemWindowInsets();
        });
        setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTopInset != 0) {
            int width = getWidth();

            canvas.save();
            canvas.translate(getScrollX(), getScrollY());

            mTempRect.set(0, 0, width, mTopInset);
            mInsetDrawable.setBounds(mTempRect);
            mInsetDrawable.draw(canvas);

            canvas.restore();
        }
    }
}
