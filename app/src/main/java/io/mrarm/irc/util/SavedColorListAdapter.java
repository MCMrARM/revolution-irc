package io.mrarm.irc.util;

import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Collections;
import java.util.List;

import io.mrarm.irc.R;

public class SavedColorListAdapter extends RecyclerView.Adapter {

    private static final int TYPE_COLOR = 0;
    private static final int TYPE_ADD_COLOR = 1;
    private static final int TYPE_RESET_COLOR = 2;

    private Integer mResetColor;
    private List<Integer> mColors;
    private Runnable mAddListener;
    private ColorResetSelectListener mResetListener;
    private ColorSelectListener mListener;

    public SavedColorListAdapter(List<Integer> colors) {
        mColors = colors;
    }

    public void setResetColor(Integer resetColor, ColorResetSelectListener listener) {
        this.mResetColor = resetColor;
        this.mResetListener = listener;
    }

    public void setListener(ColorSelectListener listener) {
        this.mListener = listener;
    }

    public void setAddColorListener(Runnable listener) {
        this.mAddListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD_COLOR) {
            View ret =  LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.color_list_add_color, parent, false);
            return new AddViewHolder(ret);
        }
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
        if (holder.getItemViewType() == TYPE_ADD_COLOR)
            return;
        if (mResetColor != null) {
            if (position == 0) {
                ((ViewHolder) holder).bind(mResetColor);
                return;
            }
            --position;
        }
        ((ViewHolder) holder).bind(mColors.get(position));
    }

    public int getColorsStart() {
        return (mResetColor != null ? 1 : 0);
    }

    @Override
    public int getItemCount() {
        return mColors.size() + getColorsStart() + 1;
    }

    public void addColor(int color) {
        mColors.add(color);
        notifyItemInserted(getItemCount() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mResetColor != null)
            return TYPE_RESET_COLOR;
        if (position == getItemCount() - 1)
            return TYPE_ADD_COLOR;
        return TYPE_COLOR;
    }

    public TouchHelperCallbacks createTouchHelperCallbacks() {
        return new TouchHelperCallbacks();
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

    public class AddViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mIcon;

        public AddViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mAddListener.run();
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


    public class TouchHelperCallbacks extends ItemTouchHelper.Callback {

        private boolean mDragDelete = false;

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            int swipeFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int colorsStart = getColorsStart();
            int fromPosition = viewHolder.getAdapterPosition() - colorsStart;
            int toPosition = Math.max(Math.min(target.getAdapterPosition() - colorsStart, mColors.size() - 1), 0);
            mDragDelete = (target.getAdapterPosition() == 0);
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++)
                    Collections.swap(mColors, i, i + 1);
            } else {
                for (int i = fromPosition; i > toPosition; i--)
                    Collections.swap(mColors, i, i - 1);
            }
            notifyItemMoved(fromPosition + colorsStart, toPosition + colorsStart);
            return true;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (mDragDelete) {
                mColors.remove(viewHolder.getAdapterPosition() - getColorsStart());
                notifyItemRemoved(viewHolder.getAdapterPosition());
            }
            mDragDelete = false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }

    }

}
