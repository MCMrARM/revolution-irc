package io.mrarm.irc.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import io.mrarm.irc.R;

public class JumpToRecentButton extends RelativeLayout {

    public JumpToRecentButton(Context context) {
        this(context, null);
    }

    public JumpToRecentButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JumpToRecentButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context)
                .inflate(R.layout.jump_to_recent_button, this, true);
    }

}
