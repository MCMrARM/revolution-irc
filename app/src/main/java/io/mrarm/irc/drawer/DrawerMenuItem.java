package io.mrarm.irc.drawer;

import android.view.View;

public class DrawerMenuItem {

    String mName;
    int mIconResId;
    View.OnClickListener mListener;

    public DrawerMenuItem(String name, int iconResId) {
        this.mName = name;
        this.mIconResId = iconResId;
    }

    public String getName() {
        return mName;
    }

    public int getIcon() {
        return mIconResId;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setIcon(int iconResId) {
        this.mIconResId = iconResId;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        this.mListener = listener;
    }

}
