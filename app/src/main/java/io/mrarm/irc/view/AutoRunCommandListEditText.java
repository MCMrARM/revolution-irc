package io.mrarm.irc.view;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.chat.ChatSuggestionsAdapter;
import io.mrarm.irc.chat.CommandListSuggestionsAdapter;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.util.SelectableRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleChipSpan;
import io.mrarm.irc.util.SimpleTextWatcher;
import io.mrarm.irc.util.StyledAttributesHelper;

public class AutoRunCommandListEditText extends AppCompatEditText
        implements ChatSuggestionsAdapter.OnItemClickListener {

    private static final String[] PASSWORD_LINE_PREFIXES = new String[] {
            "/msg NickServ IDENTIFY ",
            "/raw NICKSERV IDENTIFY ",
            "NICKSERV IDENTIFY "
    };

    private RecyclerView mSuggestionsList;
    private CommandListSuggestionsAdapter mCommandAdapter;
    private View mPopupAnchor;
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
        List<CommandAliasManager.CommandAlias> additionalItems = new ArrayList<>();
        additionalItems.add(CommandAliasManager.CommandAlias.raw("wait", "<seconds>", ""));
        additionalItems.add(CommandAliasManager.CommandAlias.raw("wait-for", "<channel>", ""));
        mCommandAdapter.setAdditionalItems(additionalItems);

        mSuggestionsList = new RecyclerView(getContext());
        mSuggestionsList.setAdapter(mCommandAdapter);
        mSuggestionsList.setLayoutManager(new LinearLayoutManager(getContext()));

        mPopupAnchor = new View(getContext());

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
        final CharSequence text = getCurrentLineToken();
        if (text == null)
            return;
        Filter filter = mCommandAdapter.getFilter();
        filter.filter(text, (int i) -> {
            if (i == 0) {
                dismissDropDown();
                return;
            }
            if (!text.equals(getCurrentLineToken()) && !enoughToFilter())
                return;
            if (completeIfSingle && i == 1) {
                onItemClick(mCommandAdapter.getItem(0));
                return;
            }
            if (i > 0)
                showDropDown();
        });
    }

    private void updatePopupAnchor() {
        if (mPopupAnchor.getParent() != getParent() && getParent() != null)
            ((ViewGroup) getParent()).addView(mPopupAnchor);

        ViewGroup.MarginLayoutParams myLp = ((ViewGroup.MarginLayoutParams) getLayoutParams());
        ViewGroup.MarginLayoutParams anchorLp = ((ViewGroup.MarginLayoutParams)
                mPopupAnchor.getLayoutParams());
        anchorLp.topMargin = myLp.topMargin + getPaddingTop();
        anchorLp.leftMargin = myLp.leftMargin + getPaddingLeft();
        anchorLp.rightMargin = myLp.rightMargin + getPaddingLeft();
        anchorLp.width = ViewGroup.MarginLayoutParams.MATCH_PARENT;
        anchorLp.height = getLineHeight();
        int d = getResources().getDimensionPixelSize(R.dimen.add_server_autocomplete_dialog_dist);
        anchorLp.topMargin -= d;
        anchorLp.height += d * 2;
        mPopupAnchor.setLayoutParams(anchorLp);
    }

    private void showDropDown() {
        updatePopupAnchor();

        int h = Math.min(mCommandAdapter.getItemCount() * mPopupItemHeight, mMaxPopupHeight);
        if (!mPopupWindow.isShowing()) {
            mPopupWindow.setWidth(getWidth());
            mPopupWindow.setHeight(h);
            mPopupWindow.showAsDropDown(mPopupAnchor, 0,
                    getLayout().getLineTop(getLayout().getLineForOffset(getSelectionStart())));
        } else {
            mPopupWindow.update(mPopupAnchor, getWidth(), h);
        }
    }

    public void dismissDropDown() {
        if (mPopupWindow != null)
            mPopupWindow.dismiss();
        if (mSuggestionsList.getAdapter() instanceof SelectableRecyclerViewAdapter)
            ((SelectableRecyclerViewAdapter) mSuggestionsList.getAdapter()).setSelection(-1);
    }


    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);

        if (getLayout() == null)
            return;
        String line = getText().subSequence(getLayout().getLineStart(getLayout()
                .getLineForOffset(selStart)), selEnd).toString();
        boolean isPassword = getPasswordStart(line) != -1;
        boolean wasPassword = (getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0;
        if (isPassword && !wasPassword) {
            Typeface tf = getTypeface();
            setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                    InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            setTypeface(tf);
            setSelection(selStart, selEnd);
        } else if (!isPassword && wasPassword) {
            createPasswordSpans();
            setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        createPasswordSpans();
    }

    private void createPasswordSpans() {
        Editable text = getText();
        for (int start = 0; start < text.length(); start++) {
            int end;
            for (end = start; end < text.length(); end++) {
                if (text.charAt(end) == '\n')
                    break;
            }
            CharSequence line = text.subSequence(start, end);

            int passwordStart = getPasswordStart(line.toString());
            if (passwordStart != -1 && (line.length() != passwordStart + 1 ||
                    line.charAt(passwordStart) != '-')) {
                String replacedText = getTextWithPasswords(line.subSequence(passwordStart,
                        line.length()));
                getText().replace(start + passwordStart, end, "-");
                getText().setSpan(new PasswordSpan(getContext(), replacedText),
                        start + passwordStart, start + passwordStart + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = start + passwordStart + 2;
                continue;
            }
            start = end;
        }
    }

    private static String getTextWithPasswords(CharSequence seq) {
        SpannableStringBuilder lstr = new SpannableStringBuilder(seq);
        for (PasswordSpan span : lstr.getSpans(0, lstr.length(), PasswordSpan.class)) {
            lstr.replace(lstr.getSpanStart(span), lstr.getSpanEnd(span), span.mPassword);
            lstr.removeSpan(span);
        }
        return lstr.toString();
    }

    public String getTextWithPasswords() {
        return getTextWithPasswords(getText());
    }

    private static int getPasswordStart(String line) {
        for (String p : PASSWORD_LINE_PREFIXES) {
            if (line.regionMatches(true, 0, p, 0, p.length()))
                return p.length();
        }
        return -1;
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
        CharSequence line = getCurrentLineToken();
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

    private CharSequence getCurrentLineToken() {
        int tokenStart = findTokenStart();
        if (tokenStart != 0 && getText().charAt(tokenStart - 1) != '\n')
            return null;
        return getText().subSequence(tokenStart, getSelectionStart());
    }

    public static class PasswordSpan extends SimpleChipSpan {

        private final String mPassword;

        private static Drawable getIcon(Context context) {
            Drawable d = context.getResources().getDrawable(R.drawable.ic_lock_small).mutate();
            DrawableCompat.setTint(d, StyledAttributesHelper.getColor(
                    context, R.attr.iconColor, 0));
            return d;
        }

        public PasswordSpan(Context context, String password) {
            super(context, null, getIcon(context), true);
            mPassword = password;
        }

    }

}
