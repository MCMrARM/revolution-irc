package io.mrarm.irc.setting.fragment.theme;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.List;

import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.mrarm.irc.R;
import io.mrarm.irc.dialog.MaterialColorPickerDialog;
import io.mrarm.irc.setting.MaterialColorSetting;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.setting.SimpleSetting;
import io.mrarm.irc.util.SavedColorListAdapter;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.SpacingItemDecorator;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.view.ColorAlphaPicker;
import io.mrarm.irc.view.ColorHuePicker;
import io.mrarm.irc.view.ColorPicker;

public class ExpandableColorSetting extends MaterialColorSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry_color_compact);
    private static final int sExpandedHolder = SettingsListAdapter.registerViewHolder(
            ExpandedHolder.class, R.layout.settings_list_entry_color_expanded);

    private boolean mExpanded = false;
    private ExpandGroup mGroup;
    private List<Integer> mSavedColors;
    private int mDefaultColor;

    public ExpandableColorSetting(String name) {
        super(name);
    }

    public ExpandableColorSetting setExpandGroup(ExpandGroup group) {
        mGroup = group;
        return this;
    }

    public ExpandableColorSetting setSavedColors(List<Integer> colors) {
        mSavedColors = colors;
        return this;
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    public void setExpanded(boolean expanded) {
        if (mExpanded == expanded)
            return;
        if (mGroup.mCurrentlyExpanded != null && expanded)
            mGroup.mCurrentlyExpanded.setExpanded(false);
        mExpanded = expanded;
        if (expanded)
            mGroup.mCurrentlyExpanded = this;
        onUpdated();
    }

    protected void resetColor(boolean noNotifyRV) {
        mSelectedColor = getDefaultColor();
        mHasSelectedColor = false;
        onUpdated(noNotifyRV);
    }

    public void setDefaultColor(int defaultColor) {
        this.mDefaultColor = defaultColor;
    }

    public int getDefaultColor() {
        return mDefaultColor;
    }

    @Override
    public int getViewHolder() {
        if (mExpanded)
            return sExpandedHolder;
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<ExpandableColorSetting> {

        private ImageView mColor;

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mColor = itemView.findViewById(R.id.color);
        }

        @Override
        public void bind(ExpandableColorSetting entry) {
            super.bind(entry);
            mColor.setColorFilter(entry.getSelectedColor(), PorterDuff.Mode.MULTIPLY);
            if (entry.hasSelectedColor())
                setValueText(String.format("#%06x", entry.getSelectedColor() & 0xFFFFFF));
            else
                setValueText(R.string.value_default);
        }

        @Override
        public void onClick(View v) {
            getEntry().setExpanded(true);
        }

    }

    public static class ExpandedHolder extends SimpleSetting.Holder<ExpandableColorSetting>
            implements ColorPicker.ColorChangeListener {

        private ImageView mColor;
        private ColorPicker mColorPicker;
        private ColorHuePicker mHuePicker;
        private ColorAlphaPicker mAlphaPicker;
        private RecyclerView mSavedColors;
        private ItemTouchHelper mSavedColorsTouchHelper;
        private View mPaletteBtn;
        private ImageView mPaletteIcon;
        private EditText mValueHex, mValueRed, mValueGreen, mValueBlue, mValueAlpha;
        private boolean mChangingValue = false;

        public ExpandedHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mColor = itemView.findViewById(R.id.color);
            mColorPicker = itemView.findViewById(R.id.picker);
            mHuePicker = itemView.findViewById(R.id.hue);
            mAlphaPicker = itemView.findViewById(R.id.alpha);
            mColorPicker.attachToHuePicker(mHuePicker);
            mAlphaPicker.attachToPicker(mColorPicker);
            itemView.setOnClickListener(null);
            itemView.findViewById(R.id.header).setOnClickListener(this);
            mSavedColors = itemView.findViewById(R.id.recent_colors);
            mSavedColors.setLayoutManager(new LinearLayoutManager(
                    itemView.getContext(), LinearLayout.HORIZONTAL, false));
            mSavedColors.addItemDecoration(SpacingItemDecorator.fromResDimension(
                    itemView.getContext(), R.dimen.color_list_spacing));
            mPaletteBtn = itemView.findViewById(R.id.palette_btn);
            mPaletteIcon = itemView.findViewById(R.id.palette_icon);
            mValueHex = itemView.findViewById(R.id.value_hex);
            mValueRed = itemView.findViewById(R.id.value_r);
            mValueGreen = itemView.findViewById(R.id.value_g);
            mValueBlue = itemView.findViewById(R.id.value_b);
            mValueAlpha = itemView.findViewById(R.id.value_a);
            int colorAccent = StyledAttributesHelper.getColor(itemView.getContext(),
                    R.attr.colorAccent, 0);
            setPaletteBtnColor(colorAccent);

            mColorPicker.addColorChangeListener(this);
            mAlphaPicker.addValueChangeListener((a) -> {
                if (mChangingValue)
                    return;
                int v = (mColorPicker.getColor() & 0xFFFFFF) | ((int) (a * 255.f) << 24);
                setColor(v, mAlphaPicker, true);
            });
            mValueHex.addTextChangedListener(new SimpleTextWatcher((s) -> {
                if (mChangingValue)
                    return;
                try {
                    if (s.length() == 0)
                        return;
                    setColor(Color.parseColor(s.toString()), mValueHex, true);
                } catch (IllegalArgumentException ignored) {
                }
            }));
            mValueRed.addTextChangedListener(new SimpleTextWatcher(
                    (s) -> onComponentChanged(mValueRed, s, 16)));
            mValueGreen.addTextChangedListener(new SimpleTextWatcher(
                    (s) -> onComponentChanged(mValueGreen, s, 8)));
            mValueBlue.addTextChangedListener(new SimpleTextWatcher(
                    (s) -> onComponentChanged(mValueBlue, s, 0)));
            mValueAlpha.addTextChangedListener(new SimpleTextWatcher(
                    (s) -> onComponentChanged(mValueAlpha, s, 24)));

            mPaletteBtn.setOnClickListener((View v) -> showPalette());
        }

        protected void setPaletteBtnColor(int color) {
            ViewCompat.setBackgroundTintList(mPaletteBtn, ColorStateList.valueOf(color));
            int paletteIconColor = 0xFFFFFFFF;
            if (ColorUtils.calculateLuminance(color) >= 0.6)
                paletteIconColor = 0xFF000000;
            ImageViewCompat.setImageTintList(mPaletteIcon, ColorStateList.valueOf(paletteIconColor));
        }

        @Override
        public void bind(ExpandableColorSetting entry) {
            super.bind(entry);
            SavedColorListAdapter adapter = new SavedColorListAdapter(entry.mSavedColors);
            adapter.setListener((c) -> setColor(c, null, true));
            adapter.setResetColor(entry.mDefaultColor, () -> {
                setColor(entry.getDefaultColor(), null, false);
                getEntry().resetColor(true);
            });
            adapter.setAddColorListener(() -> {
                adapter.addColor(getEntry().getSelectedColor());
            });
            mSavedColors.setAdapter(adapter);
            if (mSavedColorsTouchHelper != null)
                mSavedColorsTouchHelper.attachToRecyclerView(null);
            mSavedColorsTouchHelper = new ItemTouchHelper(adapter.createTouchHelperCallbacks());
            mSavedColorsTouchHelper.attachToRecyclerView(mSavedColors);
            setColor(entry.getSelectedColor(), null, false);
        }

        private void showPalette() {
            MaterialColorPickerDialog dialog = new MaterialColorPickerDialog(
                    itemView.getContext());
            dialog.setTitle(getEntry().getName());
            dialog.setColorPickListener(mColorPicker::setColor);
            dialog.show();
        }

        private void setColor(int newColor, Object source, boolean update) {
            mColor.setColorFilter(newColor, PorterDuff.Mode.MULTIPLY);
            String hexValue = String.format("#%08x", newColor);
            setValueText(hexValue);
            mChangingValue = true;
            if (source != mColorPicker)
                mColorPicker.setColor(newColor);
            if (source != mAlphaPicker) {
                mAlphaPicker.setColor(newColor | 0xFF000000);
                mAlphaPicker.setValue(Color.alpha(newColor) / 255.f);
            }
            if (source != mValueHex)
                mValueHex.setText(hexValue);
            if (source != mValueRed)
                mValueRed.setText(String.valueOf(Color.red(newColor)));
            if (source != mValueGreen)
                mValueGreen.setText(String.valueOf(Color.green(newColor)));
            if (source != mValueBlue)
                mValueBlue.setText(String.valueOf(Color.blue(newColor)));
            if (source != mValueAlpha)
                mValueAlpha.setText(String.valueOf(Color.alpha(newColor)));
            mChangingValue = false;
            if (update) {
                getEntry().setSelectedColor(newColor, true);
            }
        }

        @Override
        public void onColorChanged(int newColor) {
            if (!mChangingValue) {
                newColor = newColor & 0xFFFFFF;
                newColor |= (int) (mAlphaPicker.getValue() * 255.f) << 24;
                setColor(newColor, mColorPicker, true);
            }
        }

        private void onComponentChanged(Object source, Editable newVal, int componentShift) {
            if (mChangingValue)
                return;
            try {
                int val = Integer.parseInt(newVal.toString());
                int color = getEntry().getSelectedColor();
                color &= ~(0xff << componentShift);
                color |= (val & 0xff) << componentShift;
                setColor(color, source, true);
            } catch (NumberFormatException ignored) {
            }
        }

        @Override
        public void onClick(View v) {
            getEntry().setExpanded(false);
        }
    }


    public static class ExpandGroup {

        private ExpandableColorSetting mCurrentlyExpanded;

    }

}