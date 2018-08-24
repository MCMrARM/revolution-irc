package io.mrarm.irc.chat;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import io.mrarm.irc.R;
import io.mrarm.irc.util.TextSelectionHandlePopup;
import io.mrarm.irc.util.TextSelectionHelper;
import io.mrarm.irc.view.TextSelectionHandleView;

public class ChatSelectTouchListener implements RecyclerView.OnItemTouchListener {

    private RecyclerView mRecyclerView;

    private BaseActionModeCallback mActionModeCallback;
    private ActionModeCallback2 mActionModeCallback2;
    private ActionModeStateCallback mActionModeStateCallback;

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

    private TextSelectionHandlePopup mLeftHandle;
    private TextSelectionHandlePopup mRightHandle;

    private int[] mTmpLocation = new int[2];
    private int[] mTmpLocation2 = new int[2];

    public ChatSelectTouchListener(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mActionModeCallback = new BaseActionModeCallback();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mActionModeCallback2 = new ActionModeCallback2(mActionModeCallback);
        recyclerView.getViewTreeObserver().addOnScrollChangedListener(this::showHandles);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(this::showHandles);
    }

    public void setActionModeStateCallback(ActionModeStateCallback callback) {
        mActionModeStateCallback = callback;
    }

    private void createHandles() {
        mLeftHandle = new TextSelectionHandlePopup(mRecyclerView.getContext(), false);
        mRightHandle = new TextSelectionHandlePopup(mRecyclerView.getContext(), true);
        mLeftHandle.setOnMoveListener(new HandleMoveListener(false));
        mRightHandle.setOnMoveListener(new HandleMoveListener(true));
    }

    private void showHandle(TextSelectionHandlePopup handle, int index, int offset) {
        TextView textView = findTextViewIn(index);
        if (textView != null) {
            int line = textView.getLayout().getLineForOffset(offset);
            int y = textView.getLayout().getLineBottom(line);
            float x = textView.getLayout().getPrimaryHorizontal(offset);
            handle.show(textView, (int) x, y);
        } else {
            handle.hide();
        }
    }

    private void showHandles() {
        if (mLeftHandle == null)
            createHandles();
        showHandle(mLeftHandle, mSelectionStartIndex, mSelectionStartOffset);
        showHandle(mRightHandle, mSelectionEndIndex, mSelectionEndOffset);
    }

    private void showActionMode() {
        if (mActionModeCallback.mCurrentActionMode != null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mRecyclerView.startActionMode(mActionModeCallback2, ActionMode.TYPE_FLOATING);
        } else {
            mRecyclerView.startActionMode(mActionModeCallback);
        }
    }

    private void hideActionModeForSelection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                mActionModeCallback.mCurrentActionMode != null)
            mActionModeCallback.mCurrentActionMode.finish();
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN)
            hideActionModeForSelection();
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            mRecyclerView.getParent().requestDisallowInterceptTouchEvent(false);
            mSelectionLongPressMode = false;
            showHandles();
            showActionMode();
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

    public CharSequence getSelectedText() {
        if (mSelectionStartIndex == -1)
            return "";
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean first = true;
        for (int i = mSelectionStartIndex; i <= mSelectionEndIndex; i++) {
            if (first)
                first = false;
            else
                builder.append('\n');
            CharSequence text = ((AdapterInterface) mRecyclerView.getAdapter()).getTextAt(i);
            if (i == mSelectionStartIndex && i == mSelectionEndIndex)
                builder.append(text.subSequence(mSelectionStartOffset, mSelectionEndOffset));
            else if (i == mSelectionStartIndex)
                builder.append(text.subSequence(mSelectionStartOffset, text.length()));
            else if (i == mSelectionEndIndex)
                builder.append(text.subSequence(0, mSelectionEndOffset));
            else
                builder.append(text);
        }
        return builder;
    }

    public void clearSelection() {
        if (mActionModeCallback.mCurrentActionMode != null)
            mActionModeCallback.mCurrentActionMode.finish();
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
            if (textView != null)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), startOffset, endOffset);
        } else {
            TextView textView = findTextViewIn(startIndex);
            if (textView != null)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), startOffset, textView.length());
            textView = findTextViewIn(endIndex);
            if (textView != null)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), 0, endOffset);
        }
    }

    public void applySelectionTo(View view, int position) {
        TextView textView = findTextViewIn(view);
        if (textView == null)
            return;
        if (position >= mSelectionStartIndex && position <= mSelectionEndIndex) {
            if (mSelectionStartIndex == mSelectionEndIndex)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), mSelectionStartOffset, mSelectionEndOffset);
            else if (position == mSelectionStartIndex)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), mSelectionStartOffset, textView.length());
            else if (position == mSelectionEndOffset)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), 0, mSelectionEndOffset);
            else
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), 0, textView.length());
        } else {
            TextSelectionHelper.removeSelection((Spannable) textView.getText());
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


    private class HandleMoveListener implements TextSelectionHandleView.MoveListener {

        private boolean mRightHandle;
        private boolean mCurrentlyRightHandle;

        public HandleMoveListener(boolean rightHandle) {
            mRightHandle = rightHandle;
        }

        @Override
        public void onMoveStarted() {
            mCurrentlyRightHandle = mRightHandle;
        }

        @Override
        public void onMoveFinished() {
            showActionMode();
        }

        @Override
        public void onMoved(float x, float y) {
            hideActionModeForSelection();

            mRecyclerView.getLocationOnScreen(mTmpLocation);
            View view = mRecyclerView.findChildViewUnder(x - mTmpLocation[0],
                    y - mTmpLocation[1]);
            int index = mRecyclerView.getChildAdapterPosition(view);
            TextView textView = findTextViewIn(view);
            if (textView == null)
                return;
            view.getLocationOnScreen(mTmpLocation);
            int offset = textView.getOffsetForPosition(x - mTmpLocation[0],
                    y - mTmpLocation[1]);

            if (mCurrentlyRightHandle) {
                if (index < mSelectionStartIndex ||
                        (index == mSelectionStartIndex && offset < mSelectionStartOffset)) {
                    setSelection(index, offset, mSelectionStartIndex, mSelectionStartOffset);
                    mCurrentlyRightHandle = false;
                } else {
                    setSelection(mSelectionStartIndex, mSelectionStartOffset, index, offset);
                }
            } else {
                if (index > mSelectionEndIndex ||
                        (index == mSelectionEndIndex && offset > mSelectionEndOffset)) {
                    setSelection(mSelectionEndIndex, mSelectionEndOffset, index, offset);
                    mCurrentlyRightHandle = true;
                } else {
                    setSelection(index, offset, mSelectionEndIndex, mSelectionEndOffset);
                }
            }
            showHandles();
        }

    }

    public class BaseActionModeCallback implements ActionMode.Callback {

        private ActionMode mCurrentActionMode;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mCurrentActionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_context_messages, menu);
            mActionModeStateCallback.onActionModeStateChanged(mode, true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_copy:
                    ClipboardManager clipboard = (ClipboardManager) mRecyclerView.getContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(
                            ClipData.newPlainText("IRC Messages", getSelectedText()));
                    clearSelection();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionModeStateCallback.onActionModeStateChanged(mode, false);
            mCurrentActionMode = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    mode.getType() != ActionMode.TYPE_FLOATING)
                clearSelection();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public class ActionModeCallback2 extends ActionMode.Callback2 {

        private BaseActionModeCallback mActionMode;

        ActionModeCallback2(BaseActionModeCallback actionMode) {
            mActionMode = actionMode;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mActionMode.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mActionMode.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mActionMode.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode.onDestroyActionMode(mode);
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            view.getLocationOnScreen(mTmpLocation);

            TextView textViewStart = findTextViewIn(mSelectionStartIndex);
            TextView textViewEnd = findTextViewIn(mSelectionEndIndex);
            int lineStart = textViewStart != null ?
                    textViewStart.getLayout().getLineForOffset(mSelectionStartOffset) : -1;
            int lineEnd = textViewStart != null ?
                    textViewStart.getLayout().getLineForOffset(mSelectionEndOffset) : -1;

            outRect.top = 0;
            if (textViewStart != null) {
                textViewStart.getLocationOnScreen(mTmpLocation2);
                outRect.top = mTmpLocation2[1] - mTmpLocation[1];
                outRect.top += textViewStart.getLayout().getLineTop(lineStart);
            }
            outRect.bottom = view.getHeight();
            if (textViewEnd != null) {
                textViewEnd.getLocationOnScreen(mTmpLocation2);
                outRect.bottom = mTmpLocation2[1] - mTmpLocation[1];
                outRect.bottom += textViewStart.getLayout().getLineBottom(lineEnd);
            }
            outRect.left = 0;
            outRect.right = view.getWidth();
            if (textViewStart != null && textViewStart == textViewEnd && lineStart == lineEnd) {
                textViewStart.getLocationOnScreen(mTmpLocation2);
                outRect.left = mTmpLocation2[0] - mTmpLocation[0];
                outRect.left += textViewStart.getLayout().getPrimaryHorizontal(mSelectionStartOffset);
                outRect.right = mTmpLocation2[0] - mTmpLocation[0];
                outRect.right += textViewStart.getLayout().getPrimaryHorizontal(mSelectionEndOffset);
            }
        }
    }


    public interface ActionModeStateCallback {
        void onActionModeStateChanged(ActionMode mode, boolean visible);
    }

    public interface AdapterInterface {
        CharSequence getTextAt(int position);
    }

}
