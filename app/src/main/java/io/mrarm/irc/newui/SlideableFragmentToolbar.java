package io.mrarm.irc.newui;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.mrarm.irc.R;

public class SlideableFragmentToolbar extends ViewGroup
        implements SlideableFragmentContainer.DragListener {

    private DrawerArrowDrawable mDrawerDrawable;
    private ToolbarHolder mFragmentToolbar;
    private ToolbarHolder mParentFragmentToolbar;
    private ViewGroup mToolbarContainer;
    private AppCompatImageButton mNavigationButton;
    private SlideableFragmentContainer mContainer;
    private final Map<Fragment, ToolbarHolder> mFragmentToolbarMap = new HashMap<>();
    private boolean mInDrag = false;

    public SlideableFragmentToolbar(Context context) {
        this(context, null);
    }

    public SlideableFragmentToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideableFragmentToolbar(Context context, @Nullable AttributeSet attrs,
                                    int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mToolbarContainer = new FrameLayout(context);

        mNavigationButton = new AppCompatImageButton(context, null,
                R.attr.toolbarNavigationButtonStyle);
        mDrawerDrawable = new DrawerArrowDrawable(mNavigationButton.getContext());
        mNavigationButton.setImageDrawable(mDrawerDrawable);
        ViewCompat.setElevation(mNavigationButton, TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()));

        addView(mToolbarContainer);
        addView(mNavigationButton);
    }

    public void setContainer(FragmentManager fm, SlideableFragmentContainer container) {
        mContainer = container;
        mContainer.addDragListener(this);
        fm.registerFragmentLifecycleCallbacks(new FragmentLifecycleWatcher(), false);
    }

    public void setNavigationButtonAction(Runnable r) {
        mNavigationButton.setOnClickListener((v) -> r.run());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mToolbarContainer.layout(0, 0, getWidth(), getHeight());
        mNavigationButton.layout(0, 0,
                mNavigationButton.getMeasuredWidth(), mNavigationButton.getMeasuredHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mToolbarContainer.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mToolbarContainer.getMeasuredWidth(),
                mToolbarContainer.getMeasuredHeight());

        mNavigationButton.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }

    @Override
    public void onDragStarted(View dragView, View parentView) {
        if (mParentFragmentToolbar != null) {
            mParentFragmentToolbar.getView().setVisibility(View.VISIBLE);
            mParentFragmentToolbar.onAnimateStart(true);
        }
        mFragmentToolbar.onAnimateStart(false);
        mInDrag = true;
    }

    @Override
    public void onDragValueChanged(float value) {
        float iv = value / mContainer.getWidth();
        mDrawerDrawable.setProgress(1.f - iv);

        if (mParentFragmentToolbar != null)
            mParentFragmentToolbar.onAnimateValue(value, mContainer.getWidth(), true);
        mFragmentToolbar.onAnimateValue(value, mContainer.getWidth(), false);
    }

    @Override
    public void onDragEnded() {
        if (mParentFragmentToolbar != null) {
            mParentFragmentToolbar.getView().setVisibility(View.GONE);
            mParentFragmentToolbar.onAnimateEnd(true);
        }
        mFragmentToolbar.onAnimateEnd(false);
        mInDrag = false;
    }

    private class FragmentLifecycleWatcher extends FragmentManager.FragmentLifecycleCallbacks {

        @Override
        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                          @NonNull View v, @Nullable Bundle savedInstanceState) {
            ToolbarHolder c = ((FragmentToolbarCallback) f).onCreateToolbar(
                    LayoutInflater.from(getContext()), mToolbarContainer);
            c.getView().setTag(new FragmentAndToolbarPair(f, c));
            mToolbarContainer.addView(c.getView());
            mFragmentToolbarMap.put(f, c);
            if (mFragmentToolbar != null) {
                mParentFragmentToolbar = mFragmentToolbar;
                mParentFragmentToolbar.getView().setVisibility(View.GONE);
            }
            mFragmentToolbar = c;
            if (mInDrag)
                onDragStarted(null, null);
        }

        @Override
        public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            ToolbarHolder v = mFragmentToolbarMap.remove(f);
            if (v != null)
                mToolbarContainer.removeView(v.getView());

            mFragmentToolbar = mFragmentToolbarMap.get(mContainer.getCurrentFragment());
            if (mFragmentToolbar != null) {
                mFragmentToolbar.getView().setVisibility(View.VISIBLE);
                if (mContainer.getFragmentCount() > 1) {
                    mParentFragmentToolbar = mFragmentToolbarMap.get(
                            mContainer.getFragment(1));
                }
            }
        }
    }


    public interface FragmentToolbarCallback {

        ToolbarHolder onCreateToolbar(@NonNull LayoutInflater inflater,
                                      @Nullable ViewGroup container);

    }

    private static class FragmentAndToolbarPair {

        Fragment mFragment;
        ToolbarHolder mToolbar;

        FragmentAndToolbarPair(Fragment fragment, ToolbarHolder toolbar) {
            mFragment = fragment;
            mToolbar = toolbar;
        }

    }

    public interface ToolbarHolder {

        View getView();

        void onAnimateStart(boolean isParent);

        /**
         * @param srcValue the animation value in the range [0, width] (in the primary fragment
         *                 container space)
         * @param srcValueMax the max value (container width)
         */
        void onAnimateValue(float srcValue, float srcValueMax, boolean isParent);

        void onAnimateEnd(boolean isParent);

    }

    public static class SimpleToolbarHolder implements ToolbarHolder {

        private static class AnimationEntry {
            private View mView;
            private float mScalePrimary;
            private float mScaleParent;
        }

        private final View mView;
        private final List<AnimationEntry> mAnimationList = new ArrayList<>();

        public SimpleToolbarHolder(View view) {
            mView = view;
        }

        public View getView() {
            return mView;
        }

        @Override
        public void onAnimateStart(boolean isParent) {
            if (!isParent)
                getView().setBackground(((View) getView().getParent().getParent())
                        .getBackground().getConstantState().newDrawable());
            else
                getView().setAlpha(1.f);
        }

        @Override
        public void onAnimateValue(float srcValue, float srcValueMax, boolean isParent) {
            if (isParent) {
                for (AnimationEntry e : mAnimationList)
                    e.mView.setTranslationX((srcValueMax - srcValue) * e.mScaleParent);
            } else {
                getView().setAlpha(1.f - srcValue / srcValueMax);
                for (AnimationEntry e : mAnimationList)
                    e.mView.setTranslationX(srcValue * e.mScalePrimary);
            }
        }

        @Override
        public void onAnimateEnd(boolean isParent) {
            if (!isParent)
                getView().setAlpha(1.f);
            if (!isParent)
                getView().setBackground(null);
        }

        public void addAnimationElement(View view, float scalePrimary, float scaleParent) {
            AnimationEntry e = new AnimationEntry();
            e.mView = view;
            e.mScalePrimary = scalePrimary;
            e.mScaleParent = scaleParent;
            mAnimationList.add(e);
        }

        public void addMenu(int viewId, Fragment f) {
            ActionMenuView actionMenuView = mView.findViewById(viewId);
            f.onCreateOptionsMenu(actionMenuView.getMenu(), f.getActivity().getMenuInflater());
        }

    }

    public static class TextToolbarHolder extends SimpleToolbarHolder {

        private TextView mTitle;

        public TextToolbarHolder(Fragment f, ViewGroup parentView) {
            super(LayoutInflater.from(f.getContext()).inflate(
                    R.layout.activity_newui_simple_toolbar, parentView, false));
            mTitle = getView().findViewById(R.id.title);
            addMenu(R.id.action_menu_view, f);
            addAnimationElement(mTitle, 0.3f, -0.2f);
        }

        public void setTitle(CharSequence text) {
            mTitle.setText(text);
        }

    }

}
