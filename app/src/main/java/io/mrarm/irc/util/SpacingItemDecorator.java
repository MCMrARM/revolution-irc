package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Rect;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;
import android.view.View;

public class SpacingItemDecorator extends ItemDecoration {

    private int spacing;

    public SpacingItemDecorator(int spacing) {
        this.spacing = spacing;
    }

    public static SpacingItemDecorator fromResDimension(Context context, int dimenId) {
        return new SpacingItemDecorator(context.getResources().getDimensionPixelSize(dimenId));
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        if (parent.getChildLayoutPosition(view) != 0) {
            if (ViewCompat.getLayoutDirection(parent) ==
                    ViewCompat.LAYOUT_DIRECTION_RTL)
                outRect.right += spacing;
            else
                outRect.left += spacing;
        }
    }

}
