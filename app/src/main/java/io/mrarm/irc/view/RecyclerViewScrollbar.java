package io.mrarm.irc.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.mrarm.irc.R;

public class RecyclerViewScrollbar extends View {

    private RecyclerView mRecyclerView;
    private int mRecyclerViewId;
    private int mItemCount = 0;
    private float mScrollPos = 0.f;
    private float mBottomItemsHeight = -1.f;
    private int mScrollDragOffset = 0;

    private Drawable mScrollbarDrawable;
    private Drawable mLetterDrawable;
    private TextView mLetterView;
    private int mMinScrollbarHeight;

    public RecyclerViewScrollbar(Context context) {
        this(context, null);
    }

    public RecyclerViewScrollbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.recyclerViewScrollbarStyle);
    }

    public RecyclerViewScrollbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RecyclerViewScrollbar, defStyleAttr, 0);
        mRecyclerViewId = ta.getResourceId(R.styleable.RecyclerViewScrollbar_recyclerView, 0);
        mScrollbarDrawable = ta.getDrawable(R.styleable.RecyclerViewScrollbar_scrollbarDrawable);
        if (mScrollbarDrawable != null) {
            mScrollbarDrawable = DrawableCompat.wrap(mScrollbarDrawable).mutate();
            DrawableCompat.setTintList(mScrollbarDrawable, ta.getColorStateList(R.styleable.RecyclerViewScrollbar_scrollbarTint));
        }
        mLetterDrawable = ta.getDrawable(R.styleable.RecyclerViewScrollbar_letterDrawable);
        if (mLetterDrawable != null) {
            mLetterDrawable = DrawableCompat.wrap(mLetterDrawable).mutate();
            DrawableCompat.setTintList(mLetterDrawable, ta.getColorStateList(R.styleable.RecyclerViewScrollbar_letterTint));
        }
        int letterTextResId = ta.getResourceId(R.styleable.RecyclerViewScrollbar_letterTextAppearance, 0);
        mMinScrollbarHeight = ta.getDimensionPixelOffset(R.styleable.RecyclerViewScrollbar_minScrollbarHeight, 0);
        ta.recycle();

        mLetterView = new TextView(getContext());
        mLetterView.setBackground(mLetterDrawable);
        TextViewCompat.setTextAppearance(mLetterView, letterTextResId);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = GravityCompat.END;
        mLetterView.setLayoutParams(params);
        mLetterView.setGravity(Gravity.CENTER);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mRecyclerViewId != 0) {
            setRecyclerView(getRootView().findViewById(mRecyclerViewId));
        } else {
            setRecyclerView(mRecyclerView);
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

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mLetterView.getLayoutParams();
            params.rightMargin = w;
            mLetterView.setLayoutParams(params);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int y = (int) event.getY() - getPaddingTop() - getScrollbarTop();
            if (y < -getPaddingTop() || y > getScrollbarHeight() + getPaddingBottom()) // use padding to expand hitbox by the amount
                return false;
            mScrollDragOffset = y;
            setPressed(true);
            mLetterView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            updateDragLetterView();
            if (mLetterView.getParent() == null)
                ((RecyclerViewScrollbarLayout) getParent()).addView(mLetterView);
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            setPressed(false);
            if (mLetterView.getParent() != null)
                ((RecyclerViewScrollbarLayout) getParent()).removeView(mLetterView);
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && isPressed()) {
            float pos = (event.getY() - getPaddingTop() - mScrollDragOffset);
            pos /= (getHeight() - getPaddingTop() - getPaddingBottom() - getScrollbarHeight());
            pos *= mItemCount - getBottomViewCount() - 1;
            pos = Math.min(Math.max(pos, 0.f), mItemCount - getBottomViewCount() - 1);
            ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset((int) pos, 0);
            mScrollPos = pos;
            invalidate();
            updateDragLetterView();
            return true;
        }
        return false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mScrollbarDrawable.setState(getDrawableState()))
            invalidate();
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        if (mRecyclerView != null) {
            mRecyclerView.removeOnScrollListener(mScrollListener);
            mRecyclerView.removeOnLayoutChangeListener(mLayoutChangeListener);
        }

        mRecyclerView = recyclerView;

        recyclerView.addOnScrollListener(mScrollListener);
        recyclerView.addOnLayoutChangeListener(mLayoutChangeListener);
        registerAdapterDataObserver();
    }

    private final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (isPressed())
                return;
            updateScrollPos();
            invalidate();
        }
    };

    private final OnLayoutChangeListener mLayoutChangeListener =
            (View view, int l, int t, int r, int b, int ol, int ot, int or, int ob) -> {
                if (l == ol && t == ot && r == or && b == ob)
                    return;
                mItemCount = mRecyclerView.getAdapter().getItemCount();
                mBottomItemsHeight = -1;
                updateScrollPos();
                invalidate();
            };

    public void registerAdapterDataObserver() {
        if (mRecyclerView == null || mRecyclerView.getAdapter() == null)
            return;
        mRecyclerView.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                mItemCount = mRecyclerView.getAdapter().getItemCount();
                mBottomItemsHeight = -1;
                updateScrollPos();
                invalidate();
            }
        });
    }

    private void updateScrollPos() {
        if (mRecyclerView == null)
            return;
        int itemPos = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        mScrollPos = itemPos;
        if (itemPos == -1)
            return;
        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(itemPos);
        if (holder == null)
            return;
        mScrollPos -= (float) holder.itemView.getTop() / holder.itemView.getHeight();
    }

    private void updateDragLetterView() {
        if (isPressed() && mRecyclerView.getAdapter() instanceof LetterAdapter) {
            String lText = ((LetterAdapter) mRecyclerView.getAdapter()).getLetterFor((int) mScrollPos);
            if (lText != null) {
                mLetterView.setVisibility(View.VISIBLE);
                mLetterView.setText(lText);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mLetterView.getLayoutParams();
                params.topMargin = Math.max(getTop() + getScrollbarTop() + getScrollbarHeight()
                        - mLetterView.getMeasuredHeight(), 0) + getPaddingTop();
                mLetterView.setLayoutParams(params);
            } else {
                mLetterView.setVisibility(View.GONE);
            }
        }
    }

    private float getBottomViewCount() {
        if (mBottomItemsHeight != -1.f)
            return mBottomItemsHeight;
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
        mBottomItemsHeight = itemCount;
        return itemCount;
    }

    private int getScrollbarHeight() {
        float scrollbarHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        scrollbarHeight /= (mItemCount - getBottomViewCount());
        return Math.max((int) scrollbarHeight, mMinScrollbarHeight);
    }

    private int getScrollbarTop() {
        return (int) (mScrollPos / (mItemCount - getBottomViewCount() - 1) *
                (getHeight() - getPaddingTop() - getPaddingBottom() - getScrollbarHeight()));
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mItemCount == 0)
            return;
        if (getBottomViewCount() >= mItemCount)
            return;
        int scrollbarHeight = getScrollbarHeight();
        int scrollbarTop = getScrollbarTop();
        mScrollbarDrawable.setBounds(getPaddingLeft(),
                getPaddingTop() + scrollbarTop,
                getWidth() - getPaddingRight(),
                getPaddingTop() + scrollbarTop + scrollbarHeight);
        mScrollbarDrawable.draw(canvas);
    }

    public interface LetterAdapter {

        String getLetterFor(int postition);

    }

}
