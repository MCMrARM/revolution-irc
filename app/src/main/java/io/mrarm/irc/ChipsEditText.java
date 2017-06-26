package io.mrarm.irc;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChipsEditText extends FrameLayout {

    private static char ZERO_WIDTH_SPACE = '\u200B';

    private FlexboxLayout mFlexbox;
    private MyEditText mItemEditText;
    private int mEditTextHorizontalPadding;

    private Editable mEditable;
    private List<Integer> mEditableLineStarts;

    private List<ChipListener> mListeners = new ArrayList<>();

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

    @TargetApi(21)
    public ChipsEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mEditableLineStarts = new ArrayList<>();
        mEditableLineStarts.add(0);

        mEditTextHorizontalPadding = getResources().getDimensionPixelSize(R.dimen.chip_edit_text_horizontal_padding);

        TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs,
                new int[] { android.R.attr.editTextBackground }, 0, 0);
        try {
            setBackgroundDrawable(ta.getDrawable(0));
        } finally {
            ta.recycle();
        }

        setAddStatesFromChildren(true);

        mFlexbox = new FlexboxLayout(getContext());
        mFlexbox.setFlexWrap(FlexboxLayout.FLEX_WRAP_WRAP);
        mFlexbox.setAddStatesFromChildren(true);
        addView(mFlexbox);

        setOnClickListener((View v) -> {
            startItemEdit(mEditableLineStarts.size() - 1);
        });

        mItemEditText = new MyEditText(getContext());
        mItemEditText.setPadding(mEditTextHorizontalPadding, 0, mEditTextHorizontalPadding, 0);
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.flexGrow = 0.f;
        mItemEditText.setLayoutParams(params);
        mItemEditText.setText("");

        mEditable = mItemEditText.getEditableText();

        setMinimumHeight(mItemEditText.getLineHeight() + getPaddingTop() + getPaddingBottom());
    }

    private boolean mFinishingItemEdit = false;

    private void removeLastEditEntry() {
        mEditable.replace(mEditableLineStarts.get(mEditableLineStarts.size() - 1), mEditable.length(), "");
    }

    private void finishItemEdit() {
        if (mFinishingItemEdit)
            return;
        mFinishingItemEdit = true;
        int editIndex = mItemEditText.mEditIndex;
        Log.d("ChipsEditText", "Finish Item Edit " + editIndex + "/" + mEditableLineStarts.size());
        if (editIndex != -1) {
            mFlexbox.removeViewAt(editIndex);
            String text = getItemText(editIndex);
            mItemEditText.mEditIndex = -1;
            if (editIndex == mEditableLineStarts.size() - 1) {
                if (text.length() > 0) {
                    addItem(text, mEditableLineStarts.size() - 1);
                    removeLastEditEntry();
                }
            } else if (text.length() > 0) {
                createChip(text, editIndex);
            } else {
                removeItem(editIndex);
            }
            mFlexbox.refreshDrawableState();
        }
        mFinishingItemEdit = false;
    }

    private void startItemEdit(int index) {
        Log.d("ChipsEditText", "Start Item Edit " + index);
        finishItemEdit();

        if (index != mFlexbox.getChildCount())
            mFlexbox.removeViewAt(index);
        mItemEditText.setEditIndex(index);
        mFlexbox.addView(mItemEditText, index);
        mItemEditText.requestFocus();
    }

    private boolean isDirectlyEditing = false;

    public String getItemText(int index) {
        int i2 = (index == mEditableLineStarts.size() - 1 ? mEditable.length() : mEditableLineStarts.get(index + 1) - 1);
        return mEditable.subSequence(mEditableLineStarts.get(index), i2).toString();
    }

    public int getItemCount() {
        return mEditableLineStarts.size() - 1;
    }

    public void addItem(String text, int index) {
        isDirectlyEditing = true;
        createChip(text, index);
        if (mItemEditText.mEditIndex >= index)
            mItemEditText.mEditIndex++;
        text = text + "\n";
        int s = mEditableLineStarts.get(index);
        int l = text.length();
        mEditable.replace(s, s, text);
        mEditableLineStarts.add(index + 1, s + l);
        for (int i = index + 2; i < mEditableLineStarts.size(); i++) {
            mEditableLineStarts.set(i, mEditableLineStarts.get(i) + l);
        }
        isDirectlyEditing = false;
        for (ChipListener listener : mListeners)
            listener.onChipAdded(text, index);
    }

    public void removeItem(int index) {
        if (mItemEditText.mEditIndex == index)
            finishItemEdit();
        isDirectlyEditing = true;
        mFlexbox.removeViewAt(index);
        if (mItemEditText.mEditIndex > index)
            mItemEditText.mEditIndex--;
        int s = mEditableLineStarts.get(index);
        int l = mEditableLineStarts.get(index + 1) - s;
        mEditable.delete(s, s + l);
        mEditableLineStarts.remove(index + 1);
        for (int i = index + 1; i < mEditableLineStarts.size(); i++) {
            mEditableLineStarts.set(i, mEditableLineStarts.get(i) - l);
        }
        isDirectlyEditing = false;
        for (ChipListener listener : mListeners)
            listener.onChipRemoved(index);
    }

    public void addChipListener(ChipListener listener) {
        mListeners.add(listener);
    }

    public void removeChipListener(ChipListener listener) {
        mListeners.remove(listener);
    }

    private void createChip(String text, int index) {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.chip, mFlexbox, false);
        TextView textView = (TextView) view.findViewById(R.id.text);
        textView.setText(text);
        view.setOnClickListener((View v) -> {
            startItemEdit(mFlexbox.indexOfChild(v));
        });
        mFlexbox.addView(view, index);
    }

    private int getChipIndexAtEditablePos(int i) {
        if (i < mEditableLineStarts.get(0))
            return 0;
        if (i >= mEditableLineStarts.get(mEditableLineStarts.size() - 1))
            return mEditableLineStarts.size() - 1;
        int a = 0, b = mEditableLineStarts.size() - 1, c;
        while (true) {
            c = (a + b) / 2;
            if (i >= mEditableLineStarts.get(c) && i < mEditableLineStarts.get(c + 1))
                return c;
            if (i > mEditableLineStarts.get(c))
                a = c;
            else
                b = c;
        }
    }

    private class MyEditText extends AppCompatEditText {

        private int mEditIndex = -1;
        private boolean mFocusing = false;


        public MyEditText(Context context) {
            super(context);

            setBackgroundDrawable(null);
            setLines(1);
            setMaxLines(1);
            setHorizontallyScrolling(true);
            setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus)
                        finishItemEdit();
                }
            });
            addTextChangedListener(new TextWatcher() {

                private List<Integer> mRemoveItems = new ArrayList<>();
                private List<Integer> mAddItems = new ArrayList<>();

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    if (isDirectlyEditing)
                        return;
                    int startIndex = getChipIndexAtEditablePos(start) + 1;
                    for (int i = startIndex; i < mEditableLineStarts.size(); i++) {
                        mEditableLineStarts.set(i, mEditableLineStarts.get(i) + after - count);
                    }

                    for (int i = start; i < start + count; i++) {
                        if (s.charAt(i) == '\n') {
                            // chip removal
                            mRemoveItems.add(getChipIndexAtEditablePos(i) - 1);
                        }
                    }
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isDirectlyEditing)
                        return;
                    for (int i = start; i < start + count; i++) {
                        if (s.charAt(i) == ' ' || s.charAt(i) == ',' || s.charAt(i) == '\n') {
                            mAddItems.add(i);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isDirectlyEditing)
                        return;
                    if (mRemoveItems.size() > 0) {
                        Collections.sort(mRemoveItems, Collections.reverseOrder());
                        int pi = -1;
                        for (int i : mRemoveItems) {
                            if (pi == i)
                                continue;
                            pi = i;
                            mFlexbox.removeViewAt(i);
                            mEditableLineStarts.remove(i + 1);
                            mEditIndex--;
                        }
                        mRemoveItems.clear();
                    }
                    if (mAddItems.size() > 0) {
                        Collections.sort(mAddItems, Collections.reverseOrder());
                        for (int i : mAddItems) {
                            isDirectlyEditing = true;
                            s.replace(i, i + 1, "\n");
                            isDirectlyEditing = false;
                            int n = getChipIndexAtEditablePos(i);
                            mEditableLineStarts.add(n + 1, i + 1);
                            createChip(getItemText(n), n);

                            mEditIndex++;
                        }
                        mAddItems.clear();
                    }
                }
            });
        }

        public void setEditIndex(int index) {
            mEditIndex = index;
            if (mEditIndex == mEditableLineStarts.size() - 1)
                setSelection(mEditableLineStarts.get(index));
            else
                setSelection(mEditableLineStarts.get(index + 1) - 1);
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            mFocusing = true;
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            mFocusing = false;
        }

        @Override
        protected void onSelectionChanged(int selStart, int selEnd) {
            if (getParent() != null && !mFocusing && !isDirectlyEditing) {
                int newItemEditIndex = getChipIndexAtEditablePos(selStart);
                if (newItemEditIndex != mEditIndex) {
                    startItemEdit(newItemEditIndex);
                }
            }
            super.onSelectionChanged(selStart, selEnd);
        }

        @Override
        public boolean bringPointIntoView(int offset) {
            int oldScrollY = getScrollY();
            //boolean ret = super.bringPointIntoView(offset);
            int p = (getMeasuredHeight() - getLineHeight());
            if (mEditIndex != -1)
                setScrollY(getLineHeight() * mEditIndex);
            return getScrollY() != oldScrollY;
        }

        @Override
        public void computeScroll() {
            //
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            bringPointIntoView(getSelectionStart());
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = 0, height = 0;
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
                width = MeasureSpec.getSize(widthMeasureSpec);
            } else {
                if (getLayout() != null)
                    width = (int) Math.ceil(getLayout().getLineWidth(mEditIndex));
                width = Math.max((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()), width);
                width += getCompoundPaddingLeft() + getCompoundPaddingRight();
                width = Math.max(width, getSuggestedMinimumWidth());

                if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST)
                    width = Math.min(MeasureSpec.getSize(widthMeasureSpec), width);
            }
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                height = MeasureSpec.getSize(heightMeasureSpec);
            } else {
                if (getLayout() != null)
                    height = getLineHeight();
                height += getCompoundPaddingTop() + getCompoundPaddingBottom();
                height = Math.max(height, getSuggestedMinimumHeight());

                if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST)
                    height = Math.min(MeasureSpec.getSize(heightMeasureSpec), height);
            }
            setMeasuredDimension(width, height);
        }
    }

    public interface ChipListener {

        void onChipAdded(String text, int index);

        void onChipRemoved(int index);

    }

}
