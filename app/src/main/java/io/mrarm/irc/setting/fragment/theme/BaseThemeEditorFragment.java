package io.mrarm.irc.setting.fragment.theme;

import android.content.Context;
import android.view.View;

import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.ThemeEditorActivity;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.ListSetting;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.setting.fragment.SettingsListFragment;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.theme.ThemeAttrMapping;
import io.mrarm.irc.util.theme.ThemeInfo;
import io.mrarm.irc.util.theme.live.LiveThemeManager;
import io.mrarm.irc.util.theme.live.LiveThemeViewFactory;
import io.mrarm.irc.view.ColorPicker;

public abstract class BaseThemeEditorFragment extends SettingsListFragment {

    protected ThemeInfo getThemeInfo() {
        return ((ThemeEditorActivity) getActivity()).getThemeInfo();
    }

    protected LiveThemeManager getLiveThemeManager() {
        return ((ThemeEditorActivity) getActivity()).getLiveThemeManager();
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
            if (attrs != null && attrs.size() > 0) {
                StyledAttributesHelper attrVals =
                        StyledAttributesHelper.obtainStyledAttributes(context,
                                theme.baseThemeInfo.getThemeResId(), new int[]{attrs.get(0)});
                int ret = attrVals.getColor(attrs.get(0), 0);
                attrVals.recycle();
                return ret;
            }
            Integer attr = ThemeAttrMapping.getIrcColorAttr(prop);
            if (attr != null) {
                StyledAttributesHelper attrVals =
                        StyledAttributesHelper.obtainStyledAttributes(context,
                                theme.baseThemeInfo.getIRCColorsResId(), new int[]{attr});
                int ret = attrVals.getColor(attr, 0);
                attrVals.recycle();
                return ret;
            }
            return 0;
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
