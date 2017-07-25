package io.mrarm.irc.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import io.mrarm.irc.R;

public class RecyclerViewScrollbar extends View {

    private Drawable mScrollbarDrawable;
    private RecyclerView mRecyclerView;
    private int mRecyclerViewId;
    private int mItemCount = 0;
    private float mScrollPos = 0.f;
    private float mBottomItemsHeight = -1.f;

    public RecyclerViewScrollbar(Context context) {
        this(context, null);
    }

    public RecyclerViewScrollbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.recyclerViewScrollbarStyle);
    }

    public RecyclerViewScrollbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScrollbarDrawable = context.getResources().getDrawable(R.drawable.recyclerview_scrollbar)
                .getConstantState().newDrawable();

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs,
                new int[] { R.attr.recyclerView }, defStyleAttr, 0);
        mRecyclerViewId = ta.getResourceId(0, 0);
        ta.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mRecyclerViewId != 0) {
            mRecyclerView = (RecyclerView) getRootView().findViewById(mRecyclerViewId);
            mRecyclerView.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    mItemCount = mRecyclerView.getAdapter().getItemCount();
                    mBottomItemsHeight = -1;
                    updateScrollPos();
                    invalidate();
                }
            });
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    updateScrollPos();
                    invalidate();
                }
            });
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMeasureMode != MeasureSpec.EXACTLY) {
            int w = mScrollbarDrawable.getIntrinsicWidth();
            w += getPaddingLeft() + getPaddingRight();
            if (widthMeasureMode == MeasureSpec.AT_MOST)
                w = Math.min(w, MeasureSpec.getSize(widthMeasureSpec));
            setMeasuredDimension(w, getMeasuredHeight());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getActionIndex() == 0) {
            int y = (int) event.getY() - getPaddingTop() - getScrollbarTop();
            if (y < -getPaddingTop() || y > getScrollbarHeight() + getPaddingBottom()) // use padding to expand hitbox by the amount
                return false;
            setPressed(true);
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP && event.getActionIndex() == 0) {
            setPressed(false);
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && isPressed()) {
            float pos = (event.getY() - getPaddingTop()) / getScrollbarHeight();
            ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset((int) pos, 0);
        }
        return false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mScrollbarDrawable.setState(getDrawableState()))
            invalidate();
    }

    private void updateScrollPos() {
        if (mRecyclerView == null)
            return;
        int itemPos = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        mScrollPos = itemPos;
        if (itemPos == -1)
            return;
        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(itemPos);
        mScrollPos -= (float) holder.itemView.getTop() / holder.itemView.getHeight();
    }

    private float getBottomViewCount() {
        int itemCount = mRecyclerView.getAdapter().getItemCount();
        int totalHeight = 0;
        int maxHeight = mRecyclerView.getHeight();
        int i;
        for (i = itemCount - 1; i >= 0; i--) {
            int viewType = mRecyclerView.getAdapter().getItemViewType(i);
            RecyclerView.ViewHolder holder = mRecyclerView.getRecycledViewPool().getRecycledView(viewType);
            if (holder == null)
                holder = mRecyclerView.getAdapter().createViewHolder(mRecyclerView, viewType);
            mRecyclerView.getAdapter().bindViewHolder(holder, i);
            holder.itemView.requestLayout();
            mRecyclerView.getLayoutManager().measureChild(holder.itemView, 0, 0);
            totalHeight += mRecyclerView.getLayoutManager().getDecoratedMeasuredHeight(holder.itemView);
            mRecyclerView.getRecycledViewPool().putRecycledView(holder);
            if (totalHeight >= maxHeight)
                return itemCount - 1 - i - (float) (totalHeight - maxHeight) / holder.itemView.getMeasuredHeight();
        }
        return 0.f;
    }

    private float getScrollbarHeight() {
        float scrollbarHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        scrollbarHeight /= (mItemCount - mBottomItemsHeight);
        return scrollbarHeight;
    }

    private int getScrollbarTop() {
        return (int) (mScrollPos * getScrollbarHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mItemCount == 0)
            return;
        if (mBottomItemsHeight == -1)
            mBottomItemsHeight = getBottomViewCount();
        int scrollbarHeight = (int) getScrollbarHeight();
        int scrollbarTop = getScrollbarTop();
        mScrollbarDrawable.setBounds(getPaddingLeft(),
                getPaddingTop() + scrollbarTop,
                getWidth() - getPaddingRight(),
                getPaddingTop() + scrollbarTop + scrollbarHeight);
        mScrollbarDrawable.draw(canvas);
    }
}
