package io.mrarm.irc.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import io.mrarm.irc.R;

public class ColorListPickerDialog {

    private Context mContext;
    private String mTitle;
    private int[] mColors;
    private int mSelectedColor = -1;
    private OnColorChangeListener mListener;
    private int mPositiveButtonText = R.string.action_ok;
    private DialogInterface.OnClickListener mPositiveButtonListener;
    private int mNegativeButtonText = 0;
    private DialogInterface.OnClickListener mNegativeButtonListener;
    private int mNeutralButtonText = 0;
    private DialogInterface.OnClickListener mNeutralButtonListener;
    private AlertDialog mDialog;

    public ColorListPickerDialog(Context context) {
        mContext = context;
        mColors = context.getResources().getIntArray(R.array.colorPickerColors);
    }

    public ColorListPickerDialog setTitle(int titleId) {
        mTitle = mContext.getString(titleId);
        return this;
    }

    public ColorListPickerDialog setTitle(String title) {
        mTitle = title;
        return this;
    }

    public void setColors(int[] colors, int selectedColor) {
        mColors = colors;
        mSelectedColor = selectedColor;
    }

    public void setSelectedColor(int color) {
        for (int i = 0; i < mColors.length; i++) {
            if (mColors[i] == color) {
                mSelectedColor = i;
                return;
            }
        }
    }

    public ColorListPickerDialog setOnColorChangeListener(OnColorChangeListener listener) {
        mListener = listener;
        return this;
    }

    public void setPositiveButton(int stringId, DialogInterface.OnClickListener listener) {
        mPositiveButtonText = stringId;
        mPositiveButtonListener = listener;
    }

    public void setNegativeButton(int stringId, DialogInterface.OnClickListener listener) {
        mNegativeButtonText = stringId;
        mNegativeButtonListener = listener;
    }

    public void setNeutralButton(int stringId, DialogInterface.OnClickListener listener) {
        mNeutralButtonText = stringId;
        mNeutralButtonListener = listener;
    }


    private View buildDialogView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_color_picker, null);

        RecyclerView recyclerView = view.findViewById(R.id.items);
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 4));
        recyclerView.setAdapter(new ColorListAdapter(this));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        return view;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(mTitle)
                .setView(buildDialogView())
                .setPositiveButton(mPositiveButtonText, mPositiveButtonListener);
        if (mNegativeButtonText != 0)
            builder.setNegativeButton(mNegativeButtonText, mNegativeButtonListener);
        if (mNegativeButtonText != 0)
            builder.setNegativeButton(mNegativeButtonText, mNegativeButtonListener);
        if (mNeutralButtonText != 0)
            builder.setNeutralButton(mNeutralButtonText, mNeutralButtonListener);
        mDialog = builder.show();
    }

    public void cancel() {
        mDialog.cancel();
        mDialog = null;
    }


    private static class ColorListAdapter extends RecyclerView.Adapter<ColorListAdapter.ViewHolder> {

        private ColorListPickerDialog mDialog;

        public ColorListAdapter(ColorListPickerDialog dialog) {
            mDialog = dialog;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.dialog_color_picker_item, viewGroup, false);
            return new ViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            viewHolder.bind(mDialog.mColors[i], mDialog.mSelectedColor == i);
        }

        @Override
        public int getItemCount() {
            return mDialog.mColors.length;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {

            private ImageView mColor;
            private ImageView mCheck;

            public ViewHolder(View view, ColorListAdapter adapter) {
                super(view);
                view.setOnClickListener((View v) -> {
                    int oldIndex = adapter.mDialog.mSelectedColor;
                    adapter.mDialog.mSelectedColor = getAdapterPosition();
                    if (oldIndex != -1)
                    adapter.notifyItemChanged(oldIndex);
                    adapter.notifyItemChanged(adapter.mDialog.mSelectedColor);
                    if (adapter.mDialog.mListener != null)
                        adapter.mDialog.mListener.onColorChanged(adapter.mDialog,
                                adapter.mDialog.mSelectedColor,
                                adapter.mDialog.mColors[adapter.mDialog.mSelectedColor]);
                });
                mColor = view.findViewById(R.id.color);
                mCheck = view.findViewById(R.id.check);
            }

            public void bind(int color, boolean selected) {
                mColor.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                if (selected) {
                    mCheck.setVisibility(View.VISIBLE);
                    mCheck.clearColorFilter();
                    if (Color.red(color) > 180 && Color.green(color) > 180 && Color.blue(color) > 180)
                        mCheck.setColorFilter(0xFF000000, PorterDuff.Mode.MULTIPLY);
                } else {
                    mCheck.setVisibility(View.GONE);
                }
            }

        }

    }

    public interface OnColorChangeListener {

        void onColorChanged(ColorListPickerDialog dialog, int newColorIndex, int color);

    }

}
