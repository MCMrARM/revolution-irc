package io.mrarm.irc.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import io.mrarm.irc.view.TextSelectionHandleView;

public class TextSelectionHandlePopup {

    private TextSelectionHandleView mView;
    private PopupWindow mWindow;
    private static int[] mTempLocation = new int[2];

    public TextSelectionHandlePopup(Context ctx, boolean rightHandle) {
        mView = new TextSelectionHandleView(ctx, rightHandle);
        mWindow = new PopupWindow(mView.getContext(), null, android.R.attr.textSelectHandleWindowStyle);
        mWindow.setSplitTouchEnabled(true);
        mWindow.setClippingEnabled(false);
        mWindow.setContentView(mView);
        mWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        mWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mView.measure(0, 0);
    }

    @SuppressLint("RtlHardcoded")
    public void show(View parent, int x, int y) {
        parent.getLocationOnScreen(mTempLocation);
        x = x - mView.getHotspotX() + mTempLocation[0] + parent.getPaddingLeft();
        y = y + mTempLocation[1] + parent.getPaddingTop();
        if (mWindow.isShowing())
            mWindow.update(x, y,
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        else
            mWindow.showAtLocation(parent, Gravity.START | Gravity.TOP, x, y);
    }

    public void hide() {
        mWindow.dismiss();
    }

    public boolean isVisible() {
        return mWindow.isShowing();
    }

    public void setOnMoveListener(TextSelectionHandleView.MoveListener listener) {
        mView.setOnMoveListener(listener);
    }

}
