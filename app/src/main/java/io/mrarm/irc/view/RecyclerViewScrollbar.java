package io.mrarm.irc.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import io.mrarm.irc.R;

public class RecyclerViewScrollbar extends View {

    private Drawable mScrollbarDrawable;
    private RecyclerView mRecyclerView;
    private int mRecyclerViewId;
    private int mItemCount = 0;
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
                    invalidate();
                }
            });
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
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

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        float scroll = 0.f;
        if (mRecyclerView != null) {
            int itemPos = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            scroll += itemPos;
            if (itemPos == -1)
                return;
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(itemPos);
            scroll -= (float) holder.itemView.getTop() / holder.itemView.getHeight();
        }
        if (mItemCount == 0)
            return;
        if (mBottomItemsHeight == -1)
            mBottomItemsHeight = getBottomViewCount();
        float scrollbarHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        scrollbarHeight /= (mItemCount - mBottomItemsHeight);
        scroll *= scrollbarHeight;
        mScrollbarDrawable.setBounds(getPaddingLeft(), getPaddingTop() + (int) scroll,
                getWidth() - getPaddingRight(),
                getPaddingTop() + (int) scroll + (int) scrollbarHeight);
        mScrollbarDrawable.draw(canvas);
    }
}
