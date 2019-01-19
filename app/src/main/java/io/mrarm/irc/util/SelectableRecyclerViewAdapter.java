package io.mrarm.irc.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import io.mrarm.irc.R;

public abstract class SelectableRecyclerViewAdapter<T extends SelectableRecyclerViewAdapter.ViewHolder> extends RecyclerView.Adapter<T> {

    private int mSelection = -1;
    private Drawable mBackground;
    private Drawable mSelectedBackground;

    public SelectableRecyclerViewAdapter(Context context) {
        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(context,
                new int[] { R.attr.selectableItemBackground, R.attr.colorControlHighlight });
        mBackground = ta.getDrawable(R.attr.selectableItemBackground);
        int color = ta.getColor(R.attr.colorControlHighlight, 0);
        color = ColorUtils.setAlphaComponent(color, Color.alpha(color) / 2);
        mSelectedBackground = new ColorDrawable(color);
        ta.recycle();
    }

    public void setSelection(int i) {
        int o = mSelection;
        mSelection = i;
        if (o != -1)
            notifyItemChanged(o);
        if (i != -1)
            notifyItemChanged(i);
    }

    public int getSelection() {
        return mSelection;
    }

    @Override
    public void onBindViewHolder(T holder, int position) {
        holder.itemView.setBackground((mSelection == position ? mSelectedBackground : mBackground)
                .getConstantState().newDrawable());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View view) {
            super(view);
        }

    }

}
