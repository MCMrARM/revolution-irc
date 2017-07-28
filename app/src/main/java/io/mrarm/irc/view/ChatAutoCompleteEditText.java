package io.mrarm.irc.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.ListPopupWindow;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.QwertyKeyListener;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupWindow;

import io.mrarm.chatlib.dto.ModeList;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.irc.chat.ChatSuggestionsAdapter;
import io.mrarm.irc.chat.CommandListSuggestionsAdapter;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.SimpleTextWatcher;

public class ChatAutoCompleteEditText extends FormattableEditText implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int THRESHOLD = 2;

    private boolean mDoThresholdSuggestions;
    private boolean mDoAtSuggestions;
    private boolean mAtSuggestionsRemoveAt;
    private boolean mDoChannelSuggestions;

    private ListPopupWindow mPopup;
    private boolean mCurrentCommandAdapter = false;
    private ChatSuggestionsAdapter mAdapter;
    private CommandListSuggestionsAdapter mCommandAdapter;
    private ModeList mChannelTypes;

    public ChatAutoCompleteEditText(Context context) {
        super(context);
        init();
    }

    public ChatAutoCompleteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChatAutoCompleteEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPopup = new ListPopupWindow(getContext());
        mPopup.setAnchorView(this);
        mPopup.setOnItemClickListener((AdapterView<?> adapterView, View view, int i, long l) -> {
            onItemClick(adapterView.getAdapter(), i);
        });

        addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
            if (enoughToFilter())
                performFiltering(false);
            else
                dismissDropDown();
        }));

        onSharedPreferenceChanged(null, null);
    }

    public void setAdapter(ChatSuggestionsAdapter adapter) {
        mAdapter = adapter;
        mPopup.setAdapter(adapter);
        mCurrentCommandAdapter = false;
        mAdapter.setChannelsEnabled(mDoChannelSuggestions);
    }

    public void setCommandListAdapter(CommandListSuggestionsAdapter adapter) {
        mCommandAdapter = adapter;
    }

    private void setCurrentCommandAdapter(boolean command) {
        if (mCurrentCommandAdapter == command)
            return;
        if (command)
            mPopup.setAdapter(mCommandAdapter);
        else
            mPopup.setAdapter(mAdapter);
        mCurrentCommandAdapter = command;
    }

    public void setChannelTypes(ModeList channelTypes) {
        mChannelTypes = channelTypes;
    }

    public void requestTabComplete() {
        performFiltering(true);
    }

    public void dismissDropDown() {
        mPopup.dismiss();
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        mPopup.setOnDismissListener(listener);
    }

    private void showDropDown() {
        mPopup.show();
    }

    private void performFiltering(boolean completeIfSingle) {
        final String text = getText().toString();
        Filter filter = isCommandToken() ? mCommandAdapter.getFilter() : mAdapter.getFilter();
        filter.filter(text, (int i) -> {
            if (i == 0)
                dismissDropDown();
            if (!getText().toString().equals(text) && !enoughToFilter())
                return;
            if (completeIfSingle && i == 1) {
                onItemClick(filter == mCommandAdapter.getFilter() ? mCommandAdapter : mAdapter, 0);
                return;
            }
            if (i > 0) {
                setCurrentCommandAdapter(filter == mCommandAdapter.getFilter());
                showDropDown();
            }
        });
    }

    private boolean enoughToFilter() {
        Editable s = getText();
        int end = getSelectionEnd();
        if (end < 0)
            return false;
        int start = findTokenStart();
        boolean hasAt = s.length() > start && s.charAt(start) == '@';
        return (mDoThresholdSuggestions && end - start >= THRESHOLD &&
                (mDoAtSuggestions || !hasAt)) ||
                (mDoAtSuggestions && hasAt) ||
                (mDoChannelSuggestions && mChannelTypes != null && s.length() > start &&
                        mChannelTypes.contains(s.charAt(start))) ||
                isCommandToken();
    }

    private boolean isCommandToken() {
        return findTokenStart() == 0 && getText().length() > 0 && getText().charAt(0) == '/';
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.addPreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_SUGGESTIONS, this);
        s.addPreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS, this);
        s.addPreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS_REMOVE_AT, this);
        s.addPreferenceChangeListener(SettingsHelper.PREF_CHANNEL_AUTOCOMPLETE_SUGGESTIONS, this);
        onSharedPreferenceChanged(null, null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.removePreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_SUGGESTIONS, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS_REMOVE_AT, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_CHANNEL_AUTOCOMPLETE_SUGGESTIONS, this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SettingsHelper s = SettingsHelper.getInstance(getContext());
        mDoThresholdSuggestions = s.shouldShowNickAutocompleteSuggestions();
        mDoAtSuggestions = s.shouldShowNickAutocompleteAtSuggestions();
        mAtSuggestionsRemoveAt = s.shouldRemoveAtWithNickAutocompleteAtSuggestions();
        mDoChannelSuggestions = s.shouldShowChannelAutocompleteSuggestions();
        if (mAdapter != null)
            mAdapter.setChannelsEnabled(mDoChannelSuggestions);
    }

    public void onItemClick(Adapter adapter, int index) {
        Object item = adapter.getItem(index);
        CharSequence val;
        if (item instanceof NickWithPrefix) {
            val = terminateNickToken(((NickWithPrefix) item).getNick());
        } else if (item instanceof CommandAliasManager.CommandAlias) {
            val = "/" + ((CommandAliasManager.CommandAlias) item).name + " ";
        } else {
            val = item.toString() + " ";
        }
        int start = findTokenStart();
        int end = findTokenEnd();
        QwertyKeyListener.markAsReplaced(getText(), start, end, TextUtils.substring(getText(), start, end));
        getText().replace(findTokenStart(), findTokenEnd(), val);
    }

    public int findTokenStart() {
        int cursor = getSelectionStart();
        for (int start = cursor; start > 0; start--) {
            char c = getText().charAt(start - 1);
            if (c == ' ' || c == ',')
                return start;
        }
        return 0;
    }

    public int findTokenEnd() {
        int len = getText().length();
        int cursor = getSelectionStart();
        for (int end = cursor; end < len; end++) {
            char c = getText().charAt(end);
            if (c == ' ' || c == ',' || c == ':')
                return end;
        }
        return len;
    }

    public CharSequence terminateNickToken(CharSequence text) {
        int start = findTokenStart();
        if (!mAtSuggestionsRemoveAt &&
                (getText().length() >= start && getText().charAt(start) == '@'))
            return "@" + text + " ";
        if (start == 0)
            return text + ": ";
        return text + " ";
    }

}
