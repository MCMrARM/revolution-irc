package io.mrarm.irc.newui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import io.mrarm.irc.R;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ServerIconListIndicator extends RecyclerView.ItemDecoration
        implements ViewPager.OnPageChangeListener {

    private final Paint mPaint = new Paint();
    private final float mIndicatorHeight;
    private int mActiveIndex = -1;
    private float mActiveOffset = 0;
    private RecyclerView mRecyclerView;

    public ServerIconListIndicator(Context ctx) {
        mIndicatorHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                ctx.getResources().getDisplayMetrics());
        mPaint.setColor(StyledAttributesHelper.getColor(ctx, R.attr.colorAccent, 0));
    }

    public void setRecyclerView(RecyclerView rv) {
        mRecyclerView = rv;
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent,
                           @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        RecyclerView.ViewHolder vh = parent.findViewHolderForAdapterPosition(mActiveIndex);
        int xo = (int) (vh.itemView.getWidth() * mActiveOffset);
        c.drawRect(vh.itemView.getLeft() + xo, vh.itemView.getBottom() - mIndicatorHeight,
                vh.itemView.getRight() + xo, vh.itemView.getBottom(), mPaint);
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mActiveIndex = position;
        mActiveOffset = positionOffset;
        if (mRecyclerView != null)
            mRecyclerView.invalidateItemDecorations();
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
