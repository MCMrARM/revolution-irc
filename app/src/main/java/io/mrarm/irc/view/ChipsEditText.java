package io.mrarm.irc.view;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.util.Collection;

import io.mrarm.irc.util.SimpleChipSpan;

public class ChipsEditText extends AppCompatEditText {

    public static final char SEPARATOR = ' ';

    private boolean mIsDirectlyEditing = false;

    public ChipsEditText(@NonNull Context context) {
        super(context);
        init(null);
    }

    public ChipsEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ChipsEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        setWillNotDraw(false);

        addTextChangedListener(new TextWatcher() {
            private int mStart;
            private int mEnd;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (mOldSelStart >= start + count)
                    mOldSelStart += after - count;
                if (mOldSelEnd >= start + count)
                    mOldSelEnd += after - count;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mStart = start;
                mEnd = start + count;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mIsDirectlyEditing)
                    return;
                mIsDirectlyEditing = true;
                for (int i = mStart; i < mEnd; i++) {
                    char c = s.charAt(i);
                    if (c == ' ' || c == '\n' || c == ',') {
                        char pc = s.charAt(Math.max(i - 1, 0));
                        char nc = i + 1 < s.length() ? s.charAt(i + 1) : 0;
                        if (pc == SEPARATOR || nc == SEPARATOR) {
                            // remove this character
                            s.replace(i, i + 1, "");
                            mEnd--;
                            if (nc == SEPARATOR && getSelectionStart() == i && getSelectionEnd() == i)
                                setSelection(i + 1);
                            continue;
                        }
                        if (c != ' ')
                            s.replace(i, i + 1, String.valueOf(SEPARATOR));
                        int ss = i;
                        while (ss > 0 && s.charAt(ss - 1) != SEPARATOR)
                            ss--;
                        s.setSpan(new SimpleChipSpan(getContext(), s.subSequence(ss, i).toString(), false), ss, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                mIsDirectlyEditing = false;
            }
        });
    }


    public boolean isEmpty() {
        return getText().length() == 0;
    }

    public void clearItems() {
        mIsDirectlyEditing = true;
        getText().clear();
        mIsDirectlyEditing = false;
    }

    public void setItems(Collection<String> items) {
        clearItems();
        if (items != null) {
            for (String item : items)
                addItem(item);
            if (getText().length() > 0)
                getText().append(SEPARATOR);
        }
    }

    public String[] getItems() {
        String[] s = getText().toString().split(String.valueOf(SEPARATOR));
        if (s.length > 0 && s[s.length - 1].isEmpty()) {
            String[] ns = new String[s.length - 1];
            System.arraycopy(s, 0, ns, 0, s.length - 1);
            return ns;
        }
        return s;
    }

    public void addItem(String text) {
        if (getText().length() > 0 && getText().charAt(getText().length() - 1) != SEPARATOR)
            getText().append(SEPARATOR);
        getText().append(text);
    }

    private int mOldSelStart = -1;
    private int mOldSelEnd = -1;

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        while (selStart > 0 && getText().charAt(selStart - 1) != SEPARATOR)
            selStart--;
        int l = getText().length();
        while (selEnd < l && getText().charAt(selEnd) != SEPARATOR)
            selEnd++;

        int s = mOldSelStart;
        if (s != -1) {
            Object[] spans = getText().getSpans(mOldSelStart, mOldSelEnd, SimpleChipSpan.class);
            for (Object span : spans)
                getText().removeSpan(span);
            for (int i = s; i <= Math.min(mOldSelEnd, l); i++) {
                if (i == l || getText().charAt(i) == SEPARATOR) {
                    getText().setSpan(new SimpleChipSpan(getContext(), getText().subSequence(s, i).toString(), false), s, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    s = i + 1;
                }
            }
        }

        Object[] spans = getText().getSpans(selStart, selEnd, SimpleChipSpan.class);
        for (Object span : spans)
            getText().removeSpan(span);
        mOldSelStart = selStart;
        mOldSelEnd = selEnd;
    }

}
