package io.mrarm.irc.setting.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
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
import io.mrarm.irc.util.ColorListAdapter;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.SpacingItemDecorator;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.theme.ThemeAttrMapping;
import io.mrarm.irc.util.theme.ThemeInfo;
import io.mrarm.irc.util.theme.ThemeManager;
import io.mrarm.irc.view.ColorHuePicker;
import io.mrarm.irc.view.ColorPicker;

public class ThemeSettingsFragment extends SettingsListFragment implements NamedSettingsFragment {

    public static final String ARG_THEME_UUID = "theme";

    private ThemeInfo themeInfo;
    private ListSetting baseSetting;
    private RecentColorList recentColors;

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
        setHasOptionsMenu(true);
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
        baseSetting = new ListSetting(getString(R.string.theme_base), baseThemeNames, baseThemeIds, baseThemeId);
        baseSetting.addListener((EntryRecyclerViewAdapter.Entry entry) -> {
            themeInfo.base = ((ListSetting) entry).getSelectedOptionValue();
            onPropertyChanged();
        });
//        a.add(baseSetting);

        SettingsListAdapter.SettingChangedListener applyListener =
                (EntryRecyclerViewAdapter.Entry entry) -> onPropertyChanged();
        ExpandableColorSetting.ExpandGroup colorExpandGroup = new ExpandableColorSetting.ExpandGroup();
        a.add(new ThemeColorSetting(getString(R.string.theme_color_primary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_PRIMARY)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_primary_dark))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_PRIMARY_DARK)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_accent))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ACCENT)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
//        a.add(new ThemeBoolSetting(getString(R.string.theme_light_toolbar))
//                .linkProperty(getContext(), themeInfo, ThemeInfo.PROP_LIGHT_TOOLBAR)
//                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_background))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_BACKGROUND)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_background_floating))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_BACKGROUND_FLOATING)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_text_primary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_TEXT_PRIMARY)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_text_secondary))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_TEXT_SECONDARY)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_icon))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ICON)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
        a.add(new ThemeColorSetting(getString(R.string.theme_color_icon_opaque))
                .linkProperty(getContext(), themeInfo, ThemeInfo.COLOR_ICON_OPAQUE)
                .setExpandGroup(colorExpandGroup)
                .setRecentColors(recentColors)
                .addListener(applyListener));
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

    private void onPropertyChanged() {
//        ThemeManager.getInstance(getContext()).invalidateCurrentCustomTheme();
//        getActivity().recreate();
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

        public static class Holder extends MaterialColorSetting.Holder {

            public Holder(View itemView, SettingsListAdapter adapter) {
                super(itemView, adapter);
            }

            @Override
            public void onClick(View v) {
                ((ExpandableColorSetting) getEntry()).setExpanded(true);
            }

        }

        public static class ExpandedHolder extends MaterialColorSetting.Holder implements ColorPicker.ColorChangeListener {

            private ColorPicker mColorPicker;
            private ColorHuePicker mHuePicker;
            private RecyclerView mRecentColors;
            private View mPaletteBtn;

            public ExpandedHolder(View itemView, SettingsListAdapter adapter) {
                super(itemView, adapter);
                mColorPicker = itemView.findViewById(R.id.picker);
                mHuePicker = itemView.findViewById(R.id.hue);
                mColorPicker.attachToHuePicker(mHuePicker);
                itemView.setOnClickListener(null);
                itemView.findViewById(R.id.header).setOnClickListener(this);
                mRecentColors = itemView.findViewById(R.id.recent_colors);
                mRecentColors.setLayoutManager(new LinearLayoutManager(
                        itemView.getContext(), LinearLayout.HORIZONTAL, false));
                mRecentColors.addItemDecoration(SpacingItemDecorator.fromResDimension(
                        itemView.getContext(), R.dimen.color_list_spacing));
                mPaletteBtn = itemView.findViewById(R.id.palette_btn);
                int colorAccent = StyledAttributesHelper.getColor(itemView.getContext(),
                        R.attr.colorAccent, 0);
                ViewCompat.setBackgroundTintList(mPaletteBtn, ColorStateList.valueOf(colorAccent));

                mPaletteBtn.setOnClickListener((View v) -> showPalette());
            }

            @Override
            public void bind(MaterialColorSetting entry) {
                ExpandableColorSetting expandableEntry = (ExpandableColorSetting) entry;
                mRecentColors.setAdapter(new ColorListAdapter(expandableEntry.mDefaultColor,
                        expandableEntry.mRecentColors.recentColors));
                mColorPicker.removeColorChangeListener(this);
                super.bind(entry);
                mColorPicker.setColor(entry.getSelectedColor());
                mColorPicker.addColorChangeListener(this);
            }

            private void showPalette() {
                MaterialColorPickerDialog dialog = new MaterialColorPickerDialog(
                        itemView.getContext());
                dialog.setTitle(getEntry().getName());
                dialog.setColorPickListener(mColorPicker::setColor);
                dialog.show();
            }

            @Override
            public void onColorChanged(int newColor) {
                mColor.setColorFilter(newColor, PorterDuff.Mode.MULTIPLY);
                setValueText(String.format("#%06x", newColor & 0xFFFFFF));
            }

            @Override
            public void onClick(View v) {
                ((ExpandableColorSetting) getEntry()).setExpanded(false);
            }
        }


        public static class ExpandGroup {

            private ExpandableColorSetting mCurrentlyExpanded;

        }

    }

    public static final class ThemeColorSetting extends ExpandableColorSetting {

        private boolean mHasCustomColor = false;
        private ThemeInfo mTheme;
        private String mThemeProp;

        public ThemeColorSetting(String name) {
            super(name);
        }

        @Override
        public void setSelectedColor(int color) {
            super.setSelectedColor(color);
            mHasCustomColor = true;
            if (mTheme != null) {
                mTheme.colors.put(mThemeProp, color);
            }
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
                mHasCustomColor = false;
                super.setSelectedColor(getDefaultColor());
            }
            mTheme = theme;
            mThemeProp = prop;
            return this;
        }

    }

    public static final class ThemeBoolSetting extends CheckBoxSetting {

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
