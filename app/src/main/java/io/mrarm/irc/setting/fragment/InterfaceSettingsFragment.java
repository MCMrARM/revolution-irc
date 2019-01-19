package io.mrarm.irc.setting.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.CompoundButtonCompat;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.Date;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.MessageFormatSettingsActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;
import io.mrarm.irc.ThemedActivity;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.ClickableSetting;
import io.mrarm.irc.setting.FontSizeSetting;
import io.mrarm.irc.setting.ListSetting;
import io.mrarm.irc.setting.ListWithCustomSetting;
import io.mrarm.irc.setting.RadioButtonSetting;
import io.mrarm.irc.setting.SettingsHeader;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.util.theme.ThemeInfo;
import io.mrarm.irc.util.theme.ThemeManager;

public class InterfaceSettingsFragment extends SettingsListFragment
        implements NamedSettingsFragment {

    private ClickableSetting mMessageFormatItem;
    private MessageInfo mSampleMessage;
    private ClickableSetting mAutocompleteItem;

    @Override
    public String getName() {
        return getString(R.string.pref_header_interface);
    }

    @Override
    public SettingsListAdapter createAdapter() {
        SettingsListAdapter a = new SettingsListAdapter(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        a.setRequestCodeCounter(((SettingsActivity) getActivity()).getRequestCodeCounter());
        a.add(new SettingsHeader(getString(R.string.pref_header_theme)));
        createThemeList(a);
        a.add(new ClickableSetting(getString(R.string.theme_create_new), null)
                .setOnClickListener((View v) -> {
                    ThemeInfo newTheme = createNewTheme();
                    ThemeManager.getInstance(getContext()).setTheme(newTheme);
                    openThemeEditor(newTheme);
                    getActivity().recreate();
                }));
        a.add(new SettingsHeader(getString(R.string.pref_header_interface)));
        a.add(new ListWithCustomSetting(a, getString(R.string.pref_title_font),
                getResources().getStringArray(R.array.pref_entries_font),
                getResources().getStringArray(R.array.pref_entry_values_font), "default",
                SettingsHelper.PREF_CHAT_FONT, ListWithCustomSetting.TYPE_FONT)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_FONT));
        a.add(new FontSizeSetting(getString(R.string.pref_title_font_size), -1)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_FONT_SIZE));
        a.add(new CheckBoxSetting(getString(R.string.pref_title_hide_join_part),
                getString(R.string.pref_summary_hide_join_part), false)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_HIDE_JOIN_PART));
        a.add(new CheckBoxSetting(getString(R.string.pref_title_autocorrect),
                getString(R.string.pref_summary_autocorrect), true)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_TEXT_AUTOCORRECT));
        a.add(new ListSetting(getString(R.string.pref_title_appbar_compact_mode),
                getResources().getStringArray(R.array.pref_entries_appbar_compact_mode),
                getResources().getStringArray(R.array.pref_entry_values_appbar_compact_mode),
                SettingsHelper.COMPACT_MODE_AUTO)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_APPBAR_COMPACT_MODE));
        mMessageFormatItem = new ClickableSetting(getString(R.string.pref_title_message_format), null)
                .setIntent(new Intent(getActivity(), MessageFormatSettingsActivity.class));
        a.add(mMessageFormatItem);
        mAutocompleteItem = new ClickableSetting(getString(R.string.pref_title_nick_autocomplete), null)
                .setOnClickListener((View v) -> {
                    ((SettingsActivity) getActivity()).setFragment(
                            new AutocompletePreferenceFragment());
                });
        a.add(mAutocompleteItem);
        a.add(new CheckBoxSetting(getString(R.string.pref_title_chat_box_always_multiline),
                getString(R.string.pref_summary_chat_box_always_multiline), true)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_SEND_BOX_ALWAYS_MULTILINE));
        a.add(new ListSetting(getString(R.string.pref_title_chat_box_history_swipe_mode),
                getResources().getStringArray(R.array.pref_entries_chat_history_swipe_mode),
                getResources().getStringArray(R.array.pref_entry_values_chat_history_swipe_mode),
                SettingsHelper.SWIPE_LEFT_TO_RIGHT)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_SEND_BOX_HISTORY_SWIPE_MODE));
        a.add(new CheckBoxSetting(getString(R.string.pref_title_chat_multi_scroll_mode),
                getString(R.string.pref_summary_chat_multi_scroll_mode), false)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_MULTI_SELECT_MODE));
        a.add(new CheckBoxSetting(getString(R.string.pref_title_chat_show_dcc_send),
                getString(R.string.pref_summary_chat_show_dcc_send), false)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_SHOW_DCC_SEND)
                .addListener((EntryRecyclerViewAdapter.Entry entry) -> {
                    boolean checked = ((CheckBoxSetting) entry).isChecked();
                    if (checked)
                        showDCCWarning((CheckBoxSetting) entry);
                }));

        MessageSenderInfo testSender = new MessageSenderInfo(
                getString(R.string.message_example_sender), "", "", null, null);
        Date date = MessageFormatSettingsActivity.getSampleMessageTime();
        mSampleMessage = new MessageInfo(testSender, date,
                getString(R.string.message_example_message), MessageInfo.MessageType.NORMAL);
        return a;
    }


    @Override
    public void onStart() {
        super.onStart();
        if (((ThemedActivity) getActivity()).hasThemeChanged()) {
            getActivity().recreate();
        }
    }

    private int[] getBaseThemeColors(int resId) {
        int[] colors = new int[3];
        StyledAttributesHelper attrs = StyledAttributesHelper.obtainStyledAttributes(getContext(),
                resId, new int[] { R.attr.colorPrimary, R.attr.colorPrimaryDark,
                        R.attr.colorAccent });
        colors[0] = attrs.getColor(R.attr.colorPrimary, 0);
        colors[1] = attrs.getColor(R.attr.colorPrimaryDark, 0);
        colors[2] = attrs.getColor(R.attr.colorAccent, 0);
        attrs.recycle();
        return colors;
    }

    private void createThemeList(SettingsListAdapter a) {
        ThemeManager themeManager = ThemeManager.getInstance(getContext());
        RadioButtonSetting.Group themeGroup = new RadioButtonSetting.Group();
        SettingsListAdapter.SettingChangedListener recreateCb =
                (EntryRecyclerViewAdapter.Entry e) -> getActivity().recreate();
        for (ThemeManager.BaseTheme theme : themeManager.getBaseThemes()) {
            int themeResId = theme.getThemeResId();
            a.add(new ThemeOptionSetting(getString(theme.getNameResId()),
                    themeGroup, getBaseThemeColors(themeResId))
                    .linkBaseTheme(this, theme).addListener(recreateCb));
        }
        for (ThemeInfo theme : themeManager.getCustomThemes()) {
            int[] colors = getBaseThemeColors(theme.baseThemeInfo.getThemeResId());
            Integer c = theme.colors.get(ThemeInfo.COLOR_PRIMARY);
            if (c != null)
                colors[0] = c;
            c = theme.colors.get(ThemeInfo.COLOR_PRIMARY_DARK);
            if (c != null)
                colors[1] = c;
            c = theme.colors.get(ThemeInfo.COLOR_ACCENT);
            if (c != null)
                colors[2] = c;
            a.add(new ThemeOptionSetting(theme.name, themeGroup, colors)
                    .linkCustomTheme(this, theme).addListener(recreateCb));
        }
    }

    private void openThemeEditor(ThemeInfo theme) {
        ThemeSettingsFragment fragment = new ThemeSettingsFragment();
        Bundle args = new Bundle();
        args.putString(ThemeSettingsFragment.ARG_THEME_UUID, theme.uuid.toString());
        fragment.setArguments(args);
        ((SettingsActivity) getActivity()).setFragment(fragment);
    }

    private ThemeInfo createNewTheme() {
        ThemeManager themeManager = ThemeManager.getInstance(getContext());
        ThemeInfo currentCustomTheme = themeManager.getCurrentCustomTheme();
        if (currentCustomTheme != null) {
            return createNewTheme(currentCustomTheme);
        } else {
            ThemeManager.ThemeResInfo currentTheme = themeManager.getCurrentTheme();
            if (!(currentTheme instanceof ThemeManager.BaseTheme))
                currentTheme = themeManager.getFallbackTheme();
            return createNewTheme((ThemeManager.BaseTheme) currentTheme);
        }
    }

    private ThemeInfo createNewTheme(ThemeManager.BaseTheme theme) {
        ThemeInfo newTheme = new ThemeInfo();
        newTheme.base = theme.getId();
        newTheme.baseThemeInfo = theme;
        newTheme.name = getString(R.string.theme_custom_default_name);
        initNewTheme(newTheme);
        return newTheme;
    }

    private ThemeInfo createNewTheme(ThemeInfo theme) {
        ThemeInfo newTheme = new ThemeInfo();
        newTheme.copyFrom(theme);
        newTheme.name = getString(R.string.value_copy, theme.name);
        initNewTheme(newTheme);
        return newTheme;
    }

    private void initNewTheme(ThemeInfo newTheme) {
        ThemeManager themeManager = ThemeManager.getInstance(getContext());
        try {
            themeManager.saveTheme(newTheme);
        } catch (IOException e) {
            Log.w("InterfaceSettings", "Failed to save new theme");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMessageFormatItem.setDescription(MessageBuilder.getInstance(getActivity())
                .buildMessage(mSampleMessage));

        SettingsHelper settingsHelper = SettingsHelper.getInstance(getActivity());
        StringBuilder builder = new StringBuilder();
        if (settingsHelper.isNickAutocompleteButtonVisible())
            appendString(builder, getString(R.string.pref_title_nick_autocomplete_show_button));
        if (settingsHelper.isNickAutocompleteDoubleTapEnabled())
            appendString(builder, getString(R.string.pref_title_nick_autocomplete_double_tap));
        if (settingsHelper.shouldShowNickAutocompleteSuggestions())
            appendString(builder, getString(R.string.pref_title_nick_autocomplete_suggestions));
        if (settingsHelper.shouldShowNickAutocompleteAtSuggestions())
            appendString(builder, getString(R.string.pref_title_nick_autocomplete_at_suggestions));
        if (settingsHelper.shouldShowChannelAutocompleteSuggestions())
            appendString(builder, getString(R.string.pref_title_channel_autocomplete_suggestions));
        mAutocompleteItem.setDescription(builder.toString());
    }

    private void appendString(StringBuilder builder, String str) {
        if (builder.length() > 0) {
            builder.append(getString(R.string.text_comma));
            builder.append(str.substring(0, 1).toLowerCase() + str.substring(1));
        } else {
            builder.append(str);
        }
    }


    private void showDCCWarning(CheckBoxSetting setting) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dcc_enable_send_warning_title)
                .setMessage(R.string.dcc_enable_send_warning_body)
                .setPositiveButton(R.string.dcc_approve_download_enable_anyway, null)
                .setNegativeButton(R.string.action_cancel, (DialogInterface di, int w) -> {
                    setting.setChecked(false);
                })
                .setOnCancelListener((DialogInterface di) -> {
                    setting.setChecked(false);
                })
                .show();
    }


    public static final class ThemeOptionSetting extends RadioButtonSetting {

        private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
                R.layout.settings_theme_option);

        private InterfaceSettingsFragment fragment;
        private int[] overrideColors;
        private ThemeManager.BaseTheme linkedBaseTheme;
        private ThemeInfo linkedCustomTheme;

        public ThemeOptionSetting(String name, RadioButtonSetting.Group group,
                                  int[] overrideColors) {
            super(name, group);
            this.overrideColors = overrideColors;
        }

        @Override
        public void setChecked(boolean checked) {
            super.setChecked(checked);
            if (checked) {
                if (linkedBaseTheme != null)
                    ThemeManager.getInstance(null).setTheme(linkedBaseTheme);
                if (linkedCustomTheme != null)
                    ThemeManager.getInstance(null).setTheme(linkedCustomTheme);
            }
        }

        public ThemeOptionSetting linkBaseTheme(InterfaceSettingsFragment fragment,
                                                ThemeManager.BaseTheme theme) {
            this.fragment = fragment;
            setChecked(ThemeManager.getInstance(null).getCurrentTheme() == theme);
            linkedBaseTheme = theme;
            return this;
        }

        public ThemeOptionSetting linkCustomTheme(InterfaceSettingsFragment fragment,
                                                  ThemeInfo theme) {
            this.fragment = fragment;
            setChecked(ThemeManager.getInstance(null).getCurrentCustomTheme() == theme);
            linkedCustomTheme = theme;
            return this;
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

        public static class Holder extends RadioButtonSetting.Holder
                implements View.OnLongClickListener {

            private ColorStateList mDefaultButtonTintList;

            public Holder(View itemView, SettingsListAdapter adapter) {
                super(itemView, adapter);
                mDefaultButtonTintList = CompoundButtonCompat.getButtonTintList(mCheckBox);
                itemView.setOnLongClickListener(this);
            }

            @Override
            public void bind(CheckBoxSetting entry) {
                super.bind(entry);
                int[] overrideColors = ((ThemeOptionSetting) entry).overrideColors;
                int bgColor = StyledAttributesHelper.getColor(mCheckBox.getContext(),
                        android.R.attr.colorBackground, 0);
                boolean darkBg = ColorUtils.calculateLuminance(bgColor) < 0.4;
                int overrideColor = overrideColors[0];
                for (int c : overrideColors) {
                    if ((!darkBg && ColorUtils.calculateLuminance(c) < 0.75)
                            || (darkBg && ColorUtils.calculateLuminance(c) > 0.25)) {
                        overrideColor = c;
                        break;
                    }
                }
                if (overrideColor != 0)
                    CompoundButtonCompat.setButtonTintList(mCheckBox,
                            ColorStateList.valueOf(overrideColor));
                else
                    CompoundButtonCompat.setButtonTintList(mCheckBox, mDefaultButtonTintList);
            }

            @Override
            public void onClick(View v) {
                ThemeOptionSetting themeEntry = (ThemeOptionSetting) getEntry();
                if (getEntry().isChecked() && themeEntry.linkedCustomTheme != null) {
                    themeEntry.fragment.openThemeEditor(themeEntry.linkedCustomTheme);
                    return;
                }
                super.onClick(v);
            }


            @Override
            public boolean onLongClick(View v) {
                ThemeOptionSetting themeEntry = (ThemeOptionSetting) getEntry();
                MenuBottomSheetDialog menu = new MenuBottomSheetDialog(v.getContext());
                menu.addItem(R.string.action_copy, R.drawable.ic_content_copy,
                        (MenuBottomSheetDialog.Item i) -> {
                            ThemeInfo newTheme;
                            if (themeEntry.linkedCustomTheme != null)
                                newTheme = themeEntry.fragment.createNewTheme(
                                        themeEntry.linkedCustomTheme);
                            else
                                newTheme = themeEntry.fragment.createNewTheme(
                                        themeEntry.linkedBaseTheme);
                            ThemeManager.getInstance(null).setTheme(newTheme);
                            themeEntry.fragment.openThemeEditor(newTheme);
                            themeEntry.fragment.getActivity().recreate();
                            return true;
                        });
                if (themeEntry.linkedCustomTheme != null) {
                    menu.addItem(R.string.action_edit, R.drawable.ic_edit,
                            (MenuBottomSheetDialog.Item i) -> {
                                themeEntry.fragment.openThemeEditor(themeEntry.linkedCustomTheme);
                                return true;
                            });
                    menu.addItem(R.string.action_delete, R.drawable.ic_delete,
                            (MenuBottomSheetDialog.Item i) -> {
                                ThemeManager.getInstance(null)
                                        .deleteTheme(themeEntry.linkedCustomTheme);
                                getEntry().getOwner().remove(getEntry().getIndex());
                                themeEntry.fragment.getActivity().recreate();
                                return true;
                            });
                }
                menu.show();
                return true;
            }
        }

    }

}