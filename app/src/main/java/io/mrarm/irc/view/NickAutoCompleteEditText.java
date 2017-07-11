package io.mrarm.irc.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;

import io.mrarm.irc.util.SettingsHelper;

public class NickAutoCompleteEditText extends FormattableMultiAutoCompleteEditText
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    private NickTokenizer mTokenizer;
    private boolean mDoThresholdSuggestions;
    private boolean mDoAtSuggestions;
    private boolean mAtSuggestionsRemoveAt;

    public NickAutoCompleteEditText(Context context) {
        super(context);
        init();
    }

    public NickAutoCompleteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NickAutoCompleteEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.addPreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_SUGGESTIONS, this);
        s.addPreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS, this);
        s.addPreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS_REMOVE_AT, this);
        onSharedPreferenceChanged(null, null);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SettingsHelper s = SettingsHelper.getInstance(getContext());
        s.removePreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_SUGGESTIONS, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS, this);
        s.removePreferenceChangeListener(SettingsHelper.PREF_NICK_AUTOCOMPLETE_AT_SUGGESTIONS_REMOVE_AT, this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SettingsHelper s = SettingsHelper.getInstance(getContext());
        mDoThresholdSuggestions = s.shouldShowNickAutocompleteSuggestions();
        mDoAtSuggestions = s.shouldShowNickAutocompleteAtSuggestions();
        mAtSuggestionsRemoveAt = s.shouldRemoveAtWithNickAutocompleteAtSuggestions();
    }

    private void init() {
        mTokenizer = new NickTokenizer();
        setTokenizer(mTokenizer);
    }

    @Override
    public boolean enoughToFilter() {
        Editable s = getText();
        int end = getSelectionEnd();
        if (end < 0)
            return false;
        int start = mTokenizer.findTokenStart(s, end);
        boolean hasAt = s.length() > start && s.charAt(start) == '@';
        return (mDoThresholdSuggestions && end - start >= getThreshold() &&
                (mDoAtSuggestions || !hasAt)) || (mDoAtSuggestions && hasAt);
    }

    public class NickTokenizer implements MultiAutoCompleteTextView.Tokenizer {

        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            for (int start = cursor; start > 0; start--) {
                char c = text.charAt(start - 1);
                if (c == ' ' || c == ',')
                    return start;
            }
            return 0;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int len = text.length();
            for (int end = cursor; end < len; end++) {
                char c = text.charAt(end);
                if (c == ' ' || c == ',' || c == ':')
                    return end;
            }
            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            return terminateToken(text, findTokenStart(getText(), getSelectionEnd()));
        }

        public CharSequence terminateToken(CharSequence text, int tokenStart) {
            if (!mAtSuggestionsRemoveAt &&
                    (getText().length() >= tokenStart && getText().charAt(tokenStart) == '@'))
                return "@" + text + " ";
            if (tokenStart == 0)
                return text + ": ";
            return text + " ";
        }

    }

}
