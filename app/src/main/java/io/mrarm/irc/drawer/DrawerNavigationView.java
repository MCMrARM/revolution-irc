package io.mrarm.irc.drawer;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import com.google.android.material.navigation.NavigationView;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.WindowInsets;

import io.mrarm.irc.R;

public class DrawerNavigationView extends NavigationView {

    private RecyclerView mNavList;

    public DrawerNavigationView(Context context) {
        this(context, null);
    }

    public DrawerNavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawerNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        if (mNavList == null)
            mNavList = findViewById(R.id.nav_list);
        mNavList.setPadding(insets.getSystemWindowInsetLeft(),
                0,
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
        ((DrawerMenuListAdapter) mNavList.getAdapter()).setHeaderPaddingTop(insets.getSystemWindowInsetTop());
        return super.dispatchApplyWindowInsets(insets);
    }

}
