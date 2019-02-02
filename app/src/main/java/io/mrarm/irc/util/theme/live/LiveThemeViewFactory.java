package io.mrarm.irc.util.theme.live;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

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
            if (name.equals("android.support.v7.widget.AlertDialogLayout"))
                ThemedAlertDialog.applyTheme(parent, mLiveThemeManager);
            if (name.equals("TextView"))
                return new ThemedTextView(context, attrs, mLiveThemeManager);
            if (name.equals("EditText"))
                return new ThemedEditText(context, attrs, mLiveThemeManager);
            if (name.equals("Button"))
                return new ThemedButton(context, attrs, mLiveThemeManager);
            if (name.equals("CheckBox"))
                return new ThemedCheckBox(context, attrs, mLiveThemeManager);
            if (name.equals("CheckedTextView"))
                return new ThemedCheckedTextView(context, attrs, mLiveThemeManager);
            if (name.equals("android.support.design.widget.TextInputEditText"))
                return new ThemedTextInputEditText(context, attrs, mLiveThemeManager);
            if (name.equals("android.support.design.widget.ThemedTextInputLayout"))
                return new ThemedTextInputLayout(context, attrs, mLiveThemeManager);
            if (name.equals("android.support.v7.widget.DialogTitle")) {
                View v;
                try {
                    v = (View) Class.forName(name).getConstructor(Context.class, AttributeSet.class)
                            .newInstance(context, attrs);
                } catch (Exception e) {
                    return null;
                }
                int defStyleAttr = android.R.attr.textViewStyle;
                LiveThemeComponent c = new LiveThemeComponent(v.getContext());
                ThemedView.setupTheming(v, c, attrs, defStyleAttr);
                ThemedTextView.setupTheming((TextView) v, c, attrs, defStyleAttr);
                c.setLiveThemeManager(mLiveThemeManager);
                return v;
            }
            if (name.equals("androidx.appcompat.widget.Toolbar"))
                return new ThemedToolbar(context, attrs, mLiveThemeManager);
            if (name.equals("com.google.android.material.appbar.AppBarLayout"))
                return new ThemedAppBarLayout(context, attrs, mLiveThemeManager);
            if (name.equals("com.google.android.material.tabs.TabLayout"))
                return new ThemedTabLayout(context, attrs, mLiveThemeManager);
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
