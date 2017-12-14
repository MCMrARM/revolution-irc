package io.mrarm.irc.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.PopupWindow;

import io.mrarm.irc.R;
import io.mrarm.irc.chat.ChatSuggestionsAdapter;
import io.mrarm.irc.chat.CommandListSuggestionsAdapter;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.util.SelectableRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.view.theme.ThemedEditText;

public class AutoRunCommandListEditText extends ThemedEditText
        implements ChatSuggestionsAdapter.OnItemClickListener {

    private RecyclerView mSuggestionsList;
    private CommandListSuggestionsAdapter mCommandAdapter;
    private PopupWindow mPopupWindow;
    private int mPopupItemHeight;
    private int mMaxPopupHeight;

    public AutoRunCommandListEditText(Context context) {
        super(context);
        init();
    }

    public AutoRunCommandListEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoRunCommandListEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mCommandAdapter = new CommandListSuggestionsAdapter(getContext());
        mCommandAdapter.setClickListener(this);

        mSuggestionsList = new RecyclerView(getContext());
        mSuggestionsList.setAdapter(mCommandAdapter);
        mSuggestionsList.setLayoutManager(new LinearLayoutManager(getContext()));

        mPopupWindow = new PopupWindow(getContext(), null, android.R.attr.listPopupWindowStyle);
        mPopupWindow.setContentView(mSuggestionsList);
        mPopupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        mPopupItemHeight = StyledAttributesHelper.getDimensionPixelSize(getContext(),
                android.R.attr.listPreferredItemHeightSmall, 0);
        mMaxPopupHeight = getResources().getDimensionPixelSize(R.dimen.list_popup_max_height);

        addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
            if (enoughToFilter())
                performFiltering(false);
            else
                dismissDropDown();
        }));
    }

    private void performFiltering(boolean completeIfSingle) {
        final CharSequence text = getCurrentLine();
        if (text == null)
            return;
        Filter filter = mCommandAdapter.getFilter();
        filter.filter(text, (int i) -> {
            if (i == 0) {
                dismissDropDown();
                return;
            }
            if (!getCurrentLine().equals(text) && !enoughToFilter())
                return;
            if (completeIfSingle && i == 1) {
                onItemClick(mCommandAdapter.getItem(0));
                return;
            }
            if (i > 0)
                showDropDown();
        });
    }

    private void showDropDown() {
        int h = Math.min(mCommandAdapter.getItemCount() * mPopupItemHeight, mMaxPopupHeight);
        if (!mPopupWindow.isShowing()) {
            mPopupWindow.setWidth(getWidth());
            mPopupWindow.setHeight(h);
            mPopupWindow.showAsDropDown(this);
        } else {
            mPopupWindow.update(this, getWidth(), h);
        }
    }

    public void dismissDropDown() {
        if (mPopupWindow != null)
            mPopupWindow.dismiss();
        if (mSuggestionsList.getAdapter() instanceof SelectableRecyclerViewAdapter)
            ((SelectableRecyclerViewAdapter) mSuggestionsList.getAdapter()).setSelection(-1);
    }

    @Override
    public void onItemClick(Object item) {
        CharSequence val = "/" + ((CommandAliasManager.CommandAlias) item).name + " ";
        int start = findTokenStart();
        int end = findTokenEnd();
        clearComposingText();
        getText().replace(start, end, val);
    }


    private boolean enoughToFilter() {
        CharSequence line = getCurrentLine();
        return (line != null && line.length() > 0 && line.charAt(0) == '/');
    }

    public int findTokenStart() {
        int cursor = getSelectionStart();
        for (int start = cursor; start > 0; start--) {
            char c = getText().charAt(start - 1);
            if (c == ' ' || c == '\n')
                return start;
        }
        return 0;
    }

    public int findTokenEnd() {
        int len = getText().length();
        int cursor = getSelectionStart();
        for (int end = cursor; end < len; end++) {
            char c = getText().charAt(end);
            if (c == ' ' || c == '\n')
                return end;
        }
        return len;
    }

    private CharSequence getCurrentLine() {
        int tokenStart = findTokenStart();
        if (tokenStart != 0 && getText().charAt(tokenStart - 1) != '\n')
            return null;
        return getText().subSequence(tokenStart, getSelectionStart());
    }

}
