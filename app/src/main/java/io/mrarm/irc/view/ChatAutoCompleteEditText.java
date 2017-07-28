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
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.chatlib.dto.ModeList;
import io.mrarm.chatlib.dto.NickWithPrefix;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.chatlib.irc.ServerConnectionData;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.chat.ChatSuggestionsAdapter;
import io.mrarm.irc.chat.CommandListSuggestionsAdapter;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.CommandAliasSyntaxParser;
import io.mrarm.irc.util.SimpleTextWatcher;

public class ChatAutoCompleteEditText extends FormattableEditText implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int THRESHOLD = 2;

    private boolean mDoThresholdSuggestions;
    private boolean mDoAtSuggestions;
    private boolean mAtSuggestionsRemoveAt;
    private boolean mDoChannelSuggestions;

    private ListPopupWindow mPopup;
    private boolean mCurrentCommandAdapter = false;
    private ServerConnectionInfo mConnection;
    private ChatSuggestionsAdapter mAdapter;
    private CommandListSuggestionsAdapter mCommandAdapter;
    private ModeList mChannelTypes;
    private List<CommandAliasManager.CommandAlias> mCompletingCommands;

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
            updateCompletingCommands();
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
        mAdapter.setEnabledSuggestions(true, mDoChannelSuggestions);
    }

    public void setCommandListAdapter(CommandListSuggestionsAdapter adapter) {
        mCommandAdapter = adapter;
    }

    public void setConnectionContext(ServerConnectionInfo info) {
        mConnection = info;
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
        updateCompletingCommandFlags();
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
        final String text = getCurrentToken();
        Filter filter = isCommandNameToken() ? mCommandAdapter.getFilter() : mAdapter.getFilter();
        filter.filter(text, (int i) -> {
            if (i == 0) {
                dismissDropDown();
                return;
            }
            if (!getCurrentToken().equals(text) && !enoughToFilter())
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
        updateCompletingCommandFlags();
        return (mDoThresholdSuggestions && end - start >= THRESHOLD &&
                (mDoAtSuggestions || !hasAt)) ||
                (mDoAtSuggestions && hasAt) ||
                (mDoChannelSuggestions && mChannelTypes != null && s.length() > start &&
                        mChannelTypes.contains(s.charAt(start))) ||
                isCommandNameToken() || updateCompletingCommandFlags();
    }

    private boolean isCommandNameToken() {
        return findTokenStart() == 0 && getText().length() > 0 && getText().charAt(0) == '/';
    }

    private void updateCompletingCommands() {
        String text = getText().toString();
        if (text.length() == 0 || text.charAt(0) != '/') {
            mCompletingCommands = null;
            return;
        }
        int iof = text.indexOf(' ');
        String currentCommand = iof != -1 ? text.substring(1, iof) : text;
        if (mCompletingCommands != null && mCompletingCommands.get(0).name.equalsIgnoreCase(currentCommand))
            return; // up to date
        mCompletingCommands = new ArrayList<>();
        for (CommandAliasManager.CommandAlias alias : CommandAliasManager.getDefaultAliases()) {
            if (alias.name.equalsIgnoreCase(currentCommand))
                mCompletingCommands.add(alias);
        }
        for (CommandAliasManager.CommandAlias alias : CommandAliasManager.getInstance(getContext()).getUserAliases()) {
            if (alias.name.equalsIgnoreCase(currentCommand))
                mCompletingCommands.add(alias);
        }
        if (mCompletingCommands.size() == 0)
            mCompletingCommands = null;
    }

    private boolean updateCompletingCommandFlags() {
        if (mCompletingCommands == null)
            return false;
        int end = getSelectionStart();
        String[] args = getText().toString().substring(0, end).split(" ", -1);
        if (args.length <= 1)
            return false;
        int flags = 0;
        for (CommandAliasManager.CommandAlias alias : mCompletingCommands) {
            ServerConnectionData data = ((ServerConnectionApi) mConnection.getApiInstance()).getServerConnectionData();
            flags |= alias.getSyntaxParser().getAutocompleteFlags(data, args, 1);
        }
        if (flags != 0)
            mAdapter.setEnabledSuggestions((flags & CommandAliasSyntaxParser.AUTOCOMPLETE_MEMBERS) != 0, (flags & CommandAliasSyntaxParser.AUTOCOMPLETE_CHANNELS) != 0);
        return flags != 0;
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
            mAdapter.setEnabledSuggestions(true, mDoChannelSuggestions);
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

    private String getCurrentToken() {
        int start = findTokenStart();
        int end = getSelectionEnd();
        return getText().toString().substring(start, end);
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
