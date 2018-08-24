package io.mrarm.irc.chat;

import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.TextSelectionHelper;

public class ChatSelectTouchListener implements RecyclerView.OnItemTouchListener {

    private RecyclerView mRecyclerView;

    private int mSelectionStartIndex = -1;
    private int mSelectionStartOffset = -1;
    private int mSelectionEndIndex = -1;
    private int mSelectionEndOffset = -1;

    private boolean mSelectionLongPressMode = false;
    private int mSelectionLongPressIndex = -1;
    private int mSelectionLongPressStart = -1;
    private int mSelectionLongPressEnd = -1;

    private int mLastTouchTextIndex;
    private int mLastTouchTextOffset;

    private int[] mTmpLocation = new int[2];

    public ChatSelectTouchListener(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            mRecyclerView.getParent().requestDisallowInterceptTouchEvent(false);
            mSelectionLongPressMode = false;
        }

        View view = rv.findChildViewUnder(e.getX(), e.getY());
        int index = rv.getChildAdapterPosition(view);
        TextView textView = findTextViewIn(view);
        if (textView == null)
            return mSelectionLongPressMode;
        view.getLocationOnScreen(mTmpLocation);
        float viewX = e.getRawX() - mTmpLocation[0];
        float viewY = e.getRawY() - mTmpLocation[1];

        mLastTouchTextIndex = index;
        mLastTouchTextOffset = textView.getOffsetForPosition(viewX, viewY);
        if (mSelectionLongPressMode) {
            long sel = TextSelectionHelper.getWordAt(textView.getText(), mLastTouchTextOffset,
                    mLastTouchTextOffset + 1);
            int selStart = TextSelectionHelper.unpackTextRangeStart(sel);
            int selEnd = TextSelectionHelper.unpackTextRangeEnd(sel);
            if (mLastTouchTextIndex > mSelectionLongPressIndex ||
                    (mLastTouchTextIndex == mSelectionLongPressIndex &&
                            selEnd >= mSelectionLongPressStart)) {
                setSelection(mSelectionLongPressIndex, mSelectionLongPressStart,
                        mLastTouchTextIndex, selEnd);
            } else {
                setSelection(mLastTouchTextIndex, selStart,
                        mSelectionLongPressIndex, mSelectionLongPressEnd);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        onInterceptTouchEvent(rv, e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    public void startLongPressSelect() {
        TextView textView = findTextViewIn(mLastTouchTextIndex);
        if (textView == null)
            return;

        mSelectionLongPressMode = true;
        mSelectionLongPressIndex = mLastTouchTextIndex;

        long sel = TextSelectionHelper.getWordAt(textView.getText(), mLastTouchTextOffset,
                mLastTouchTextOffset + 1);
        mSelectionLongPressStart = TextSelectionHelper.unpackTextRangeStart(sel);
        mSelectionLongPressEnd = TextSelectionHelper.unpackTextRangeEnd(sel);
        setSelection(mSelectionLongPressIndex, mSelectionLongPressStart,
                mSelectionLongPressIndex, mSelectionLongPressEnd);
        mRecyclerView.getParent().requestDisallowInterceptTouchEvent(true);
    }

    public void clearSelection() {
        if (mSelectionStartIndex != -1) {
            for (int i = mSelectionStartIndex; i <= mSelectionEndIndex; i++) {
                TextView textView = findTextViewIn(i);
                if (textView != null)
                    TextSelectionHelper.removeSelection((Spannable) textView.getText());
            }
        }
        mSelectionStartIndex = -1;
        mSelectionStartOffset = -1;
        mSelectionEndIndex = -1;
        mSelectionEndOffset = -1;
    }

    public void setSelection(int startIndex, int startOffset, int endIndex, int endOffset) {
        if (mSelectionStartIndex != -1 && startIndex < mSelectionEndIndex &&
                endIndex > mSelectionStartIndex) {
            if (startIndex > mSelectionStartIndex) {
                for (int i = mSelectionStartIndex; i < startIndex; i++) {
                    TextView textView = findTextViewIn(i);
                    if (textView != null)
                        TextSelectionHelper.removeSelection((Spannable) textView.getText());
                }
            } else if (startIndex < mSelectionStartIndex) {
                for (int i = startIndex + 1; i <= mSelectionStartIndex; i++) {
                    TextView textView = findTextViewIn(i);
                    if (textView != null)
                        TextSelectionHelper.setSelection(textView.getContext(),
                                (Spannable) textView.getText(), 0, textView.length());
                }
            }
            if (endIndex < mSelectionEndIndex) {
                for (int i = endIndex + 1; i <= mSelectionEndIndex; i++) {
                    TextView textView = findTextViewIn(i);
                    if (textView != null)
                        TextSelectionHelper.removeSelection((Spannable) textView.getText());
                }
            } else if (endIndex > mSelectionEndIndex) {
                for (int i = mSelectionEndIndex; i < endIndex; i++) {
                    TextView textView = findTextViewIn(i);
                    if (textView != null)
                        TextSelectionHelper.setSelection(textView.getContext(),
                                (Spannable) textView.getText(), 0, textView.length());
                }
            }
        } else {
            clearSelection();

            for (int i = startIndex + 1; i < endIndex; i++) {
                TextView textView = findTextViewIn(i);
                if (textView != null)
                    TextSelectionHelper.setSelection(textView.getContext(),
                            (Spannable) textView.getText(), 0, textView.length());
            }
        }
        mSelectionStartIndex = startIndex;
        mSelectionStartOffset = startOffset;
        mSelectionEndIndex = endIndex;
        mSelectionEndOffset = endOffset;
        if (startIndex == endIndex) {
            TextView textView = findTextViewIn(startIndex);
            TextSelectionHelper.setSelection(textView.getContext(),
                    (Spannable) textView.getText(), startOffset, endOffset);
        } else {
            TextView textView = findTextViewIn(startIndex);
            TextSelectionHelper.setSelection(textView.getContext(),
                    (Spannable) textView.getText(), startOffset, textView.length());
            textView = findTextViewIn(endIndex);
            TextSelectionHelper.setSelection(textView.getContext(),
                    (Spannable) textView.getText(), 0, endOffset);
        }
    }


    private TextView findTextViewIn(View view) {
        return view.findViewById(R.id.chat_message);
    }

    private TextView findTextViewIn(int position) {
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(position);
        if (vh == null)
            return null;
        return findTextViewIn(vh.itemView);
    }

}
