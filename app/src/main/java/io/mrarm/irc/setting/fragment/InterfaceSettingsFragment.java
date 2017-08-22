package io.mrarm.irc.setting.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;

import java.util.Date;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.MessageFormatSettingsActivity;
import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.ClickableSetting;
import io.mrarm.irc.setting.FontSizeSetting;
import io.mrarm.irc.setting.ListSetting;
import io.mrarm.irc.setting.ListWithCustomSetting;
import io.mrarm.irc.setting.SettingsListAdapter;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.MessageBuilder;

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
        a.add(new CheckBoxSetting(getString(R.string.pref_title_dark_theme),
                getString(R.string.pref_summary_dark_theme), false)
                .linkPreference(prefs, SettingsHelper.PREF_DARK_THEME)
                .addListener((EntryRecyclerViewAdapter.Entry entry) -> {
                    AppCompatDelegate.setDefaultNightMode(((CheckBoxSetting) entry).isChecked()
                            ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                    getActivity().recreate();
                }));
        a.add(new ListWithCustomSetting(a, getString(R.string.pref_title_font),
                getResources().getStringArray(R.array.pref_entries_font),
                getResources().getStringArray(R.array.pref_entry_values_font), "default",
                SettingsHelper.PREF_CHAT_FONT, ListWithCustomSetting.TYPE_FONT)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_FONT));
        a.add(new FontSizeSetting(getString(R.string.pref_title_font_size), -1)
                .linkPreference(prefs, SettingsHelper.PREF_CHAT_FONT_SIZE));
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

        MessageSenderInfo testSender = new MessageSenderInfo(
                getString(R.string.message_example_sender), "", "", null, null);
        Date date = MessageFormatSettingsActivity.getSampleMessageTime();
        mSampleMessage = new MessageInfo(testSender, date,
                getString(R.string.message_example_message), MessageInfo.MessageType.NORMAL);
        return a;
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

}