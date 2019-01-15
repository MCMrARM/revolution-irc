package io.mrarm.irc.util;

import android.content.res.ColorStateList;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

import io.mrarm.irc.R;

public class ColorListAdapter extends RecyclerView.Adapter {

    private static final int TYPE_COLOR = 0;
    private static final int TYPE_RESET_COLOR = 1;

    private Integer mResetColor;
    private List<Integer> mColors;
    private ColorResetSelectListener mResetListener;
    private ColorSelectListener mListener;

    public ColorListAdapter(List<Integer> colors) {
        mColors = colors;
    }

    public void setResetColor(Integer resetColor, ColorResetSelectListener listener) {
        this.mResetColor = resetColor;
        this.mResetListener = listener;
    }

    public void setListener(ColorSelectListener listener) {
        this.mListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_RESET_COLOR) {
            View ret =  LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.color_list_reset_color, parent, false);
            return new ResetViewHolder(ret);
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

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private int mColor;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        public void bind(int color) {
            mColor = color;
            ViewCompat.setBackgroundTintList(itemView, ColorStateList.valueOf(color));
        }

        @Override
        public void onClick(View v) {
            mListener.onColorSelected(mColor);
        }
    }

    public class ResetViewHolder extends ViewHolder {

        private ImageView mIcon;

        public ResetViewHolder(View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.icon);
        }

        @Override
        public void bind(int color) {
            super.bind(color);
            if (ColorUtils.calculateLuminance(color) < 0.6)
                ImageViewCompat.setImageTintList(mIcon, ColorStateList.valueOf(0xFFFFFFFF));
            else
                ImageViewCompat.setImageTintList(mIcon, ColorStateList.valueOf(0xFF000000));
        }

        @Override
        public void onClick(View v) {
            mResetListener.onColorResetSelected();
        }
    }


    public interface ColorResetSelectListener {

        void onColorResetSelected();

    }

    public interface ColorSelectListener {

        void onColorSelected(int color);

    }

}
