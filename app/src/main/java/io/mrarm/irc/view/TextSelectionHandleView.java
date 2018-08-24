package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import io.mrarm.irc.util.StyledAttributesHelper;

public class TextSelectionHandleView extends View {

    public static Drawable getDrawable(Context context, boolean rightHandle) {
        int resId = StyledAttributesHelper.getResourceId(context, rightHandle ?
                android.R.attr.textSelectHandleRight : android.R.attr.textSelectHandleLeft, -1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return context.getDrawable(resId);
        else
            return context.getResources().getDrawable(resId);
    }


    private Drawable mDrawable;
    private int mHotspotX;
    private float mMoveOffsetX;
    private float mMoveOffsetY;
    private MoveListener mMoveListener;

    public TextSelectionHandleView(Context context, Drawable drawable, int hotspotX) {
        super(context);
        mDrawable = drawable;
        mHotspotX = hotspotX;
        setFocusableInTouchMode(true);
    }

    public TextSelectionHandleView(Context context, Drawable drawable, boolean rightHandle) {
        super(context);
        mDrawable = drawable;
        int width = drawable.getIntrinsicWidth();
        mHotspotX = rightHandle ? width / 4 : width * 3 / 4;
        setFocusableInTouchMode(true);
    }

    public TextSelectionHandleView(Context context, boolean rightHandle) {
        this(context, getDrawable(context, rightHandle), rightHandle);
    }

    public int getHotspotX() {
        return mHotspotX;
    }

    public void setOnMoveListener(MoveListener listener) {
        mMoveListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mDrawable.setBounds(0, 0, getWidth(), getHeight());
        mDrawable.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mMoveOffsetX = mHotspotX - event.getX();
                mMoveOffsetY = -event.getY();
                if (mMoveListener != null)
                    mMoveListener.onMoveStarted();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mMoveListener != null)
                    mMoveListener.onMoved(event.getRawX() + mMoveOffsetX,
                            event.getRawY() + mMoveOffsetY);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mMoveListener != null)
                    mMoveListener.onMoveFinished();
                return true;
        }
        return false;
    }

    public interface MoveListener {

        void onMoveStarted();

        void onMoveFinished();

        void onMoved(float x, float y);

    }

}
