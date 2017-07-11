package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.util.AttributeSet;

public class FormattableMultiAutoCompleteEditText extends AppCompatMultiAutoCompleteTextView {

    private TextFormatBar mFormatBar;

    public FormattableMultiAutoCompleteEditText(Context context) {
        super(context);
    }

    public FormattableMultiAutoCompleteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FormattableMultiAutoCompleteEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused)
            mFormatBar.setEditText(this);
        else
            mFormatBar.setEditText(null);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mFormatBar != null)
            mFormatBar.updateFormattingAtCursor();

    }

    public void setFormatBar(TextFormatBar formatBar) {
        mFormatBar = formatBar;
    }

}
