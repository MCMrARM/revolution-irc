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
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.SimpleTextWatcher;

public class ChatAutoCompleteEditText extends FormattableEditText implements SharedPreferences.OnSharedPreferenceChangeListener, AdapterView.OnItemClickListener {

    private static final int THRESHOLD = 2;

    private boolean mDoThresholdSuggestions;
    private boolean mDoAtSuggestions;
    private boolean mAtSuggestionsRemoveAt;
    private boolean mDoChannelSuggestions;
    private boolean mForceShowSuggestions = false;

    private ListPopupWindow mPopup;
    private ListAdapter mAdapter;
    private Filter mFilter;
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
        mPopup.setOnItemClickListener(this);

        addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
            if (enoughToFilter())
                performFiltering(false);
            else
                dismissDropDown();
        }));
    }

    public void setAdapter(ListAdapter adapter) {
        mAdapter = adapter;
        mFilter = ((Filterable) adapter).getFilter();
        mPopup.setAdapter(adapter);
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
        mFilter.filter(text, (int i) -> {
            if (i == 0)
                dismissDropDown();
            if (!getText().toString().equals(text) && !enoughToFilter())
                return;
            if (completeIfSingle && i == 1) {
                onItemClick(null, null, 0, 0L);
                return;
            }
            if (i > 0)
                showDropDown();
        });
    }

    private boolean enoughToFilter() {
        Editable s = getText();
        int end = getSelectionEnd();
        if (end < 0)
            return false;
        int start = findTokenStart();
        boolean hasAt = s.length() > start && s.charAt(start) == '@';
        return mForceShowSuggestions ||
                (mDoThresholdSuggestions && end - start >= THRESHOLD &&
                        (mDoAtSuggestions || !hasAt)) ||
                (mDoAtSuggestions && hasAt) ||
                (mDoChannelSuggestions && mChannelTypes != null && s.length() > start &&
                        mChannelTypes.contains(s.charAt(start)));
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
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
        Object item = mAdapter.getItem(index);
        CharSequence val;
        if (item instanceof NickWithPrefix) {
            val = terminateNickToken(((NickWithPrefix) item).getNick());
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
