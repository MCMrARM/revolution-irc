package io.mrarm.irc.setting.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;
import io.mrarm.irc.dialog.MaterialColorPickerDialog;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.ListSetting;
import io.mrarm.irc.setting.MaterialColorSetting;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.setting.SimpleSetting;
import io.mrarm.irc.util.ColorListAdapter;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.SpacingItemDecorator;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.theme.ThemeAttrMapping;
import io.mrarm.irc.util.theme.ThemeInfo;
import io.mrarm.irc.util.theme.ThemeManager;
import io.mrarm.irc.util.theme.live.LiveThemeManager;
import io.mrarm.irc.util.theme.live.LiveThemeViewFactory;
import io.mrarm.irc.view.ColorAlphaPicker;
import io.mrarm.irc.view.ColorHuePicker;
import io.mrarm.irc.view.ColorPicker;

public class ThemeSettingsFragment extends SettingsListFragment implements NamedSettingsFragment {

    public static final String ARG_THEME_UUID = "theme";

    private ThemeInfo themeInfo;
    private ListSetting baseSetting;
    private RecentColorList recentColors;
    private LiveThemeManager mLiveThemeManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        ThemeManager themeManager = ThemeManager.getInstance(getContext());
        themeInfo = themeManager.getCustomTheme(
                UUID.fromString(getArguments().getString(ARG_THEME_UUID)));
        if (themeInfo == null)
            throw new RuntimeException("Invalid theme UUID");

        recentColors = new RecentColorList();
        recentColors.recentColors = new ArrayList<>();
        recentColors.recentColors.add(getResources().getColor(R.color.ircYellow));
        recentColors.recentColors.add(getResources().getColor(R.color.ircLightRed));
        recentColors.recentColors.add(getResources().getColor(R.color.ircBlue));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLiveThemeManager = new LiveThemeManager(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ((LiveThemeViewFactory) getActivity().getLayoutInflater().getFactory2())
                .setLiveThemeManager(mLiveThemeManager);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((LiveThemeViewFactory) getActivity().getLayoutInflater().getFactory2())
                .setLiveThemeManager(null);
        ThemeManager.getInstance(getContext()).invalidateCurrentCustomTheme();
    }

    @Override
    public String getName() {
        return themeInfo.name;
    }

    @Override
    public SettingsListAdapter createAdapter() {
        ThemeManager themeManager = ThemeManager.getInstance(getContext());
        SettingsListAdapter a = new SettingsListAdapter(this);
        a.setRequestCodeCounter(((SettingsActivity) getActivity()).getRequestCodeCounter());
        Collection<ThemeManager.BaseTheme> baseThemes = themeManager.getBaseThemes();
        String[] baseThemeNames = new String[baseThemes.size()];
        String[] baseThemeIds = new String[baseThemes.size()];
        int i = 0;
        for (ThemeManager.BaseTheme baseTheme : baseThemes) {
            baseThemeNames[i] = getString(baseTheme.getNameResId());
            baseThemeIds[i] = baseTheme.getId();
            ++i;
        }
        String baseThemeId =  themeInfo.base == null ? themeManager.getFallbackTheme().getId() : themeInfo.base;
        baseSetting = new ThemeListSetting(getString(R.string.theme_base), baseThemeNames, baseThemeIds, baseThemeId);
        baseSetting.addListener((EntryRecyclerViewAdapter.Entry entry) -> {
            themeInfo.base = ((ListSetting) entry).getSelectedOptionValue();
            themeInfo.baseThemeInfo = themeManager.getBaseThemeOrFallback(themeInfo.base);
            onNonLivePropertyChanged();
        });
        a.add(baseSetting);

        SettingsListAdapter.SettingChangedListener applyListener =
                (EntryRecyclerViewAdapter.Entry entry) -> onNonLivePropertyChanged();
        ExpandableColorSetting.ExpandGroup colorExpandGroup = new ExpandableColorSetting.ExpandGroup();
        a.add(new ThemeColorSetting(getString(R.string.theme_color_primary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_PRIMARY)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_primary_dark))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_PRIMARY_DARK)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_accent))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ACCENT)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        a.add(new ThemeBoolSetting(getString(R.string.theme_light_toolbar))
                .linkProperty(getContext(), themeInfo, ThemeInfo.PROP_LIGHT_TOOLBAR)
                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_background))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_BACKGROUND)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_background_floating))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_BACKGROUND_FLOATING)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_text_primary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_TEXT_PRIMARY)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_text_secondary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_TEXT_SECONDARY)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_icon))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ICON)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_icon_opaque))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ICON_OPAQUE)
                .linkLiveApplyManager(mLiveThemeManager)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors));
        return a;
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            ThemeManager.getInstance(getContext()).saveTheme(themeInfo);
        } catch (IOException e) {
            Log.w("ThemeSettings", "Failed to save theme");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_settings_theme, menu);
        menu.findItem(R.id.action_rename).setOnMenuItemClickListener(item -> {
            View view = LayoutInflater.from(getContext())
                    .inflate(R.layout.dialog_edit_text, null);
            EditText text = view.findViewById(R.id.edit_text);
            text.setText(themeInfo.name);
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.action_rename)
                    .setView(view)
                    .setPositiveButton(R.string.action_ok, (dialog1, which) -> {
                        themeInfo.name = text.getText().toString();
                        ((SettingsActivity) getActivity()).updateTitle();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return true;
        });
    }

    private void onNonLivePropertyChanged() {
        ThemeManager.getInstance(getContext()).invalidateCurrentCustomTheme();
        getActivity().recreate();
    }

    private static class RecentColorList {

        private List<Integer> recentColors;

    }

    public static class ExpandableColorSetting extends MaterialColorSetting {

        private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
                R.layout.settings_list_entry_color_compact);
        private static final int sExpandedHolder = SettingsListAdapter.registerViewHolder(
                ExpandedHolder.class, R.layout.settings_list_entry_color_expanded);

        private boolean mExpanded = false;
        private ExpandGroup mGroup;
        private RecentColorList mRecentColors;
        private int mDefaultColor;

        public ExpandableColorSetting(String name) {
            super(name);
        }

        public ExpandableColorSetting setExpandGroup(ExpandGroup group) {
            mGroup = group;
            return this;
        }

        public ExpandableColorSetting setRecentColors(RecentColorList colors) {
            mRecentColors = colors;
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
            private RecyclerView mRecentColors;
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
                mRecentColors = itemView.findViewById(R.id.recent_colors);
                mRecentColors.setLayoutManager(new LinearLayoutManager(
                        itemView.getContext(), LinearLayout.HORIZONTAL, false));
                mRecentColors.addItemDecoration(SpacingItemDecorator.fromResDimension(
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
                ColorListAdapter adapter = new ColorListAdapter(entry.mRecentColors.recentColors);
                adapter.setListener((c) -> setColor(c, null, true));
                adapter.setResetColor(entry.mDefaultColor, () -> {
                    setColor(entry.getDefaultColor(), null, false);
                    getEntry().resetColor(true);
                });
                mRecentColors.setAdapter(adapter);
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

    public static final class ThemeColorSetting extends ExpandableColorSetting {

        private static final int sExpandedHolder = SettingsListAdapter.registerViewHolder(
                ExpandedHolder.class, R.layout.settings_list_entry_color_expanded);

        private ThemeInfo mTheme;
        private String mThemeProp;
        private ColorPicker.ColorChangeListener mCustomApplyFunc;

        public ThemeColorSetting(String name) {
            super(name);
        }

        @Override
        protected void setSelectedColor(int color, boolean noNotifyRV) {
            super.setSelectedColor(color, noNotifyRV);
            if (mTheme != null)
                mTheme.colors.put(mThemeProp, color);
            if (mCustomApplyFunc != null)
                mCustomApplyFunc.onColorChanged(color);
        }

        @Override
        protected void resetColor(boolean noNotifyRV) {
            super.resetColor(noNotifyRV);
            if (mTheme != null)
                mTheme.colors.remove(mThemeProp);
            if (mCustomApplyFunc != null)
                mCustomApplyFunc.onColorChanged(getDefaultColor());
        }

        private static int getDefaultValue(Context context, ThemeInfo theme, String prop) {
            List<Integer> attrs = ThemeAttrMapping.getColorAttrs(prop);
            if (attrs == null || attrs.size() == 0)
                return 0;
            StyledAttributesHelper attrVals = StyledAttributesHelper.obtainStyledAttributes(context,
                    theme.baseThemeInfo.getThemeResId(), new int[] { attrs.get(0) });
            return attrVals.getColor(attrs.get(0), 0);
        }

        public ThemeColorSetting linkProperty(Context context, ThemeInfo theme, String prop) {
            Integer color = theme.colors.get(prop);
            setDefaultColor(getDefaultValue(context, theme, prop));
            if (color != null) {
                setSelectedColor(color);
            } else {
                super.setSelectedColor(getDefaultColor());
                mHasSelectedColor = false;
            }
            mTheme = theme;
            mThemeProp = prop;
            return this;
        }

        public ThemeColorSetting setCustomApplyFunc(ColorPicker.ColorChangeListener listener) {
            mCustomApplyFunc = listener;
            mCustomApplyFunc.onColorChanged(getSelectedColor());
            return this;
        }

        public ThemeColorSetting linkLiveApplyManager(LiveThemeManager liveThemeManager) {
            setCustomApplyFunc((int c) -> {
                for (int r : ThemeAttrMapping.getColorAttrs(mThemeProp)) {
                    liveThemeManager.setColorProperty(r, c);
                }
            });
            return this;
        }

        @Override
        public int getViewHolder() {
            if (isExpanded())
                return sExpandedHolder;
            return super.getViewHolder();
        }

        public static class ExpandedHolder extends ExpandableColorSetting.ExpandedHolder {
            public ExpandedHolder(View itemView, SettingsListAdapter adapter) {
                super(itemView, adapter);
                LiveThemeManager themeMgr = ((LiveThemeViewFactory) adapter.getActivity()
                        .getLayoutInflater().getFactory2()).getLiveThemeManager();
                themeMgr.addColorProperty(R.attr.colorAccent, this::setPaletteBtnColor);
            }
        }

    }

    public static final class ThemeListSetting extends ListSetting {

        private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
                R.layout.settings_list_entry_compact);

        public ThemeListSetting(String name, String[] options, String[] values,
                                String selectedOption) {
            super(name, options, values, selectedOption);
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }
    }

    public static class ThemeCheckBoxSetting extends CheckBoxSetting {

        private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
                R.layout.settings_list_checkbox_entry_compact);

        public ThemeCheckBoxSetting(String name, boolean checked) {
            super(name, checked);
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }
    }

    public static final class ThemeBoolSetting extends ThemeCheckBoxSetting {

        private boolean mHasCustomValue = false;
        private ThemeInfo mTheme;
        private String mThemeProp;

        public ThemeBoolSetting(String name) {
            super(name, false);
        }

        @Override
        public void setChecked(boolean checked) {
            super.setChecked(checked);
            mHasCustomValue = true;
            if (mTheme != null) {
                mTheme.properties.put(mThemeProp, checked);
            }
        }

        private static boolean getDefaultValue(Context context) {
            return false;
        }

        public ThemeBoolSetting linkProperty(Context context, ThemeInfo theme, String prop) {
            Boolean value = theme.getBool(prop);
            if (value != null) {
                setChecked(value);
            } else {
                mHasCustomValue = false;
                super.setChecked(getDefaultValue(context));
            }
            mTheme = theme;
            mThemeProp = prop;
            return this;
        }

    }

}
