package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class LiveThemeViewFactory implements LayoutInflater.Factory2 {

    private LiveThemeManager mLiveThemeManager;
    private LayoutInflater.Factory2 mParentFactory;

    public LiveThemeViewFactory(LiveThemeManager liveThemeManager,
                                LayoutInflater.Factory2 parentFactory) {
        mLiveThemeManager = liveThemeManager;
        mParentFactory = parentFactory;
    }

    public void setLiveThemeManager(LiveThemeManager liveThemeManager) {
        this.mLiveThemeManager = liveThemeManager;
    }

    public LiveThemeManager getLiveThemeManager() {
        return mLiveThemeManager;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (mLiveThemeManager != null) {
            if (name.equals("TextView"))
                return new ThemedTextView(context, attrs, mLiveThemeManager);
        }
        if (mParentFactory == null)
            return null;
        return mParentFactory.onCreateView(parent, name, context, attrs);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }

}
