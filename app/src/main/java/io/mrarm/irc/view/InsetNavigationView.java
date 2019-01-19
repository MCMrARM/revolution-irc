package io.mrarm.irc.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import com.google.android.material.navigation.NavigationView;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;

public class InsetNavigationView extends NavigationView {

    private View mView;
    private int basePaddingTop;
    private int basePaddingBottom;

    public InsetNavigationView(Context context) {
        this(context, null);
    }

    public InsetNavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InsetNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof RecyclerView) {
            mView = child;
            basePaddingTop = mView.getPaddingTop();
            basePaddingBottom = mView.getPaddingBottom();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        mView.setPadding(insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop() + basePaddingTop,
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom() + basePaddingBottom);
        return super.dispatchApplyWindowInsets(insets);
    }

}
