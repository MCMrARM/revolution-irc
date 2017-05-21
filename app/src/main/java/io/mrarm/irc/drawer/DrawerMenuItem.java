package io.mrarm.irc.drawer;

import android.graphics.drawable.Drawable;
import android.view.View;

public class DrawerMenuItem {

    String mName;
    Drawable mIcon;
    View.OnClickListener mListener;

    public DrawerMenuItem(String name, Drawable icon) {
        this.mName = name;
        this.mIcon = icon;
    }

    public String getName() {
        return mName;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        this.mListener = listener;
    }

}
