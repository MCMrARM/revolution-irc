package io.mrarm.irc.util;

import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

import io.mrarm.irc.R;

public class ColorListAdapter extends RecyclerView.Adapter {

    private static final int TYPE_COLOR = 0;
    private static final int TYPE_RESET_COLOR = 1;

    private Integer mResetColor;
    private List<Integer> mColors;

    public ColorListAdapter(Integer resetColor, List<Integer> colors) {
        mResetColor = resetColor;
        mColors = colors;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_RESET_COLOR) {
            View ret =  LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.color_list_reset_color, parent, false);
            return new ViewHolder(ret);
        }
        View ret = new View(parent.getContext());
        ret.setBackgroundResource(R.drawable.color_list_color);
        return new ViewHolder(ret);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mResetColor != null) {
            if (position == 0) {
                ((ViewHolder) holder).bind(mResetColor);
                return;
            }
            --position;
        }
        ((ViewHolder) holder).bind(mColors.get(position));
    }

    @Override
    public int getItemCount() {
        return mColors.size() + (mResetColor != null ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mResetColor != null)
            return TYPE_RESET_COLOR;
        return TYPE_COLOR;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void bind(int color) {
            ViewCompat.setBackgroundTintList(itemView, ColorStateList.valueOf(color));
        }

    }
}
