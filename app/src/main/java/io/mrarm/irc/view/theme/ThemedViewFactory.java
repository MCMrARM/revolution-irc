package io.mrarm.irc.view.theme;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class ThemedViewFactory implements LayoutInflater.Factory2 {

    private AppCompatActivity mActivity;

    public ThemedViewFactory(AppCompatActivity activity) {
        mActivity = activity;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (name.equals("Button"))
            return new ThemedButton(context, attrs);
        if (name.equals("EditText"))
            return new ThemedEditText(context, attrs);
        if (name.equals("CheckBox"))
            return new ThemedCheckBox(context, attrs);
        if (name.equals("Spinner"))
            return new ThemedSpinner(context, attrs);
        if (name.equals("TextView"))
            return new ThemedTextView(context, attrs);
        if (name.equals("android.support.design.widget.TextInputLayout"))
            return new ThemedTextInputLayout(context, attrs);
        if (name.equals("android.support.design.widget.TextInputEditText"))
            return new ThemedTextInputEditText(context, attrs);
        if (name.equals("android.support.design.widget.FloatingActionButton"))
            return new ThemedFloatingActionButton(context, attrs);

        View view = mActivity.onCreateView(name, context, attrs);
        if (view != null)
            return view;
        return mActivity.getDelegate().createView(parent, name, context, attrs);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }

}
