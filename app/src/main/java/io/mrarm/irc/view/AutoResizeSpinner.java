package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Rect;
import androidx.appcompat.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SpinnerAdapter;

public class AutoResizeSpinner extends AppCompatSpinner {

    private final Rect mTempRect = new Rect();
    private final LayoutParams mDefaultLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

    public AutoResizeSpinner(Context context) {
        super(context);
    }

    public AutoResizeSpinner(Context context, int mode) {
        super(context, mode);
    }

    public AutoResizeSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoResizeSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        SpinnerAdapter adapter = getAdapter();
        if (adapter != null && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
            View view = adapter.getView(getSelectedItemPosition(), null, this);
            if (view.getLayoutParams() == null)
                view.setLayoutParams(mDefaultLayoutParams);
            view.measure(widthMeasureSpec, heightMeasureSpec);
            int width = view.getMeasuredWidth();
            if (getBackground() != null) {
                getBackground().getPadding(mTempRect);
                width += mTempRect.left + mTempRect.right;
            }

            this.setMeasuredDimension(Math.min(width, MeasureSpec.getSize(widthMeasureSpec)), this.getMeasuredHeight());
        }

    }

}
