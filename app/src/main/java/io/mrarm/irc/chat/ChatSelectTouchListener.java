package io.mrarm.irc.chat;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import androidx.recyclerview.widget.RecyclerView;
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
import io.mrarm.irc.util.LongPressSelectTouchListener;
import io.mrarm.irc.util.RecyclerViewScrollerRunnable;
import io.mrarm.irc.util.TextSelectionHandlePopup;
import io.mrarm.irc.util.TextSelectionHelper;
import io.mrarm.irc.view.TextSelectionHandleView;

public class ChatSelectTouchListener implements RecyclerView.OnItemTouchListener,
        View.OnAttachStateChangeListener {

    private static final int MAX_CLICK_DURATION = 200;
    private static final int MAX_CLICK_DISTANCE = 30;

    private RecyclerView mRecyclerView;

    private BaseActionModeCallback mActionModeCallback;
    private ActionModeCallback2 mActionModeCallback2;
    private ActionModeStateCallback mActionModeStateCallback;

    private RecyclerViewScrollerRunnable mScroller;

    private long mSelectionStartId = -1;
    private int mSelectionStartOffset = -1;
    private long mSelectionEndId = -1;
    private int mSelectionEndOffset = -1;

    private boolean mSelectionLongPressMode = false;
    private long mSelectionLongPressId = -1;
    private int mSelectionLongPressStart = -1;
    private int mSelectionLongPressEnd = -1;

    private long mLastTouchTextId;
    private boolean mLastTouchInText;
    private int mLastTouchTextOffset;

    private float mTouchDownX;
    private float mTouchDownY;

    private TextSelectionHandlePopup mLeftHandle;
    private TextSelectionHandlePopup mRightHandle;

    private LongPressSelectTouchListener mMultiSelectListener;

    private int[] mTmpLocation = new int[2];
    private int[] mTmpLocation2 = new int[2];

    public ChatSelectTouchListener(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mScroller = new RecyclerViewScrollerRunnable(recyclerView, (int scrollDir) -> {
            if (scrollDir < 0) {
                handleSelection(0, 0, 0, 0);
            } else if (scrollDir > 0) {
                mRecyclerView.getLocationOnScreen(mTmpLocation);
                handleSelection(mRecyclerView.getWidth(), mRecyclerView.getHeight(),
                        mTmpLocation[0] + mRecyclerView.getWidth(),
                        mTmpLocation[1] + mRecyclerView.getHeight());
            }
        });
        mActionModeCallback = new BaseActionModeCallback();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mActionModeCallback2 = new ActionModeCallback2(mActionModeCallback);
        recyclerView.getViewTreeObserver().addOnScrollChangedListener(this::showHandles);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(this::showHandles);
        recyclerView.addOnAttachStateChangeListener(this);
    }

    public void setMultiSelectListener(LongPressSelectTouchListener selectListener) {
        mMultiSelectListener = selectListener;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        clearSelection();
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

    private void showHandle(TextSelectionHandlePopup handle, long id, int offset) {
        TextView textView = findTextViewByItemId(id);
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
        if (mSelectionLongPressMode)
            return;
        showHandle(mLeftHandle, mSelectionStartId, mSelectionStartOffset);
        showHandle(mRightHandle, mSelectionEndId, mSelectionEndOffset);
        if (!mLeftHandle.isVisible() && !mRightHandle.isVisible() && mSelectionStartId != -1)
            clearSelection();
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

    private boolean handleSelection(float x, float y, float rawX, float rawY) {
        View view = mRecyclerView.findChildViewUnder(x, y);
        if (view == null)
            return mSelectionLongPressMode;
        long id = mRecyclerView.getChildItemId(view);
        int index = mRecyclerView.getChildAdapterPosition(view);
        TextView textView = findTextViewIn(view);
        if (textView == null)
            return mSelectionLongPressMode;
        textView.getLocationOnScreen(mTmpLocation);
        float viewX = rawX - mTmpLocation[0];
        float viewY = rawY - mTmpLocation[1];

        float tViewY = Math.min(Math.max(viewY, 0), textView.getHeight() -
                textView.getCompoundPaddingBottom()) - textView.getCompoundPaddingTop();
        float tViewX = Math.min(Math.max(viewX, 0), textView.getWidth() -
                textView.getCompoundPaddingRight()) - textView.getCompoundPaddingLeft();

        mLastTouchTextId = id;
        int line = textView.getLayout().getLineForVertical((int) tViewY);
        mLastTouchTextOffset = textView.getLayout().getOffsetForHorizontal(line, tViewX);
        mLastTouchInText = viewX >= textView.getCompoundPaddingLeft() &&
                viewX <= textView.getWidth() - textView.getCompoundPaddingEnd() &&
                viewY >= textView.getCompoundPaddingTop() &&
                viewY <= textView.getHeight() - textView.getCompoundPaddingBottom() &&
                tViewX <= textView.getLayout().getLineWidth(line);
        if (mSelectionLongPressMode) {
            long sel = TextSelectionHelper.getWordAt(textView.getText(), mLastTouchTextOffset,
                    mLastTouchTextOffset + 1);
            int selStart = TextSelectionHelper.unpackTextRangeStart(sel);
            int selEnd = TextSelectionHelper.unpackTextRangeEnd(sel);
            int selLongPressIndex = getItemPosition(mSelectionLongPressId);
            if (index > selLongPressIndex ||
                    (index == selLongPressIndex && selEnd >= mSelectionLongPressStart)) {
                setSelection(mSelectionLongPressId, mSelectionLongPressStart,
                        mLastTouchTextId, selEnd);
            } else {
                setSelection(mLastTouchTextId, selStart,
                        mSelectionLongPressId, mSelectionLongPressEnd);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (((ChatMessagesAdapter) mRecyclerView.getAdapter()).getSelectedItems().size() > 0)
            return false;
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mTouchDownX = e.getX();
            mTouchDownY = e.getY();
            hideActionModeForSelection();
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP ||
                e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (!mSelectionLongPressMode &&
                    e.getEventTime() - e.getDownTime() < MAX_CLICK_DURATION &&
                    Math.sqrt(Math.pow(e.getX() - mTouchDownX, 2) +
                            Math.pow(e.getY() - mTouchDownY, 2)) < MAX_CLICK_DISTANCE *
                            Resources.getSystem().getDisplayMetrics().density) {
                clearSelection();
            }
            mRecyclerView.getParent().requestDisallowInterceptTouchEvent(false);
            mSelectionLongPressMode = false;
            if (mSelectionStartId != -1) {
                showHandles();
                showActionMode();
            }
        }
        if (mSelectionLongPressMode) {
            if (e.getActionMasked() == MotionEvent.ACTION_UP)
                mScroller.setScrollDir(0);
            else if (e.getY() < 0)
                mScroller.setScrollDir(-1);
            else if (e.getY() > mRecyclerView.getHeight())
                mScroller.setScrollDir(1);
            else
                mScroller.setScrollDir(0);
        }

        return handleSelection(e.getX(), e.getY(), e.getRawX(), e.getRawY());
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        onInterceptTouchEvent(rv, e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    public void startLongPressSelect() {
        clearSelection();

        ((ChatMessagesAdapter) mRecyclerView.getAdapter()).clearSelection();
        if (!mLastTouchInText && mMultiSelectListener != null) {
            mMultiSelectListener.startSelectMode(mRecyclerView.getAdapter().getItemId(
                    getItemPosition(mLastTouchTextId)));
            return;
        }

        TextView textView = findTextViewByItemId(mLastTouchTextId);
        if (textView == null)
            return;

        mSelectionLongPressMode = true;
        mSelectionLongPressId = mLastTouchTextId;

        long sel = TextSelectionHelper.getWordAt(textView.getText(), mLastTouchTextOffset,
                mLastTouchTextOffset + 1);
        mSelectionLongPressStart = TextSelectionHelper.unpackTextRangeStart(sel);
        mSelectionLongPressEnd = TextSelectionHelper.unpackTextRangeEnd(sel);
        setSelection(mSelectionLongPressId, mSelectionLongPressStart,
                mSelectionLongPressId, mSelectionLongPressEnd);
        mRecyclerView.getParent().requestDisallowInterceptTouchEvent(true);
    }

    public CharSequence getSelectedText() {
        if (mSelectionStartId == -1)
            return "";
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean first = true;
        int selStartIndex = getItemPosition(mSelectionStartId);
        int selEndIndex = getItemPosition(mSelectionEndId);
        for (int i = selStartIndex; i <= selEndIndex; i++) {
            if (first)
                first = false;
            else
                builder.append('\n');
            CharSequence text = ((AdapterInterface) mRecyclerView.getAdapter()).getTextAt(i);
            if (i == selStartIndex && i == selEndIndex)
                builder.append(text.subSequence(mSelectionStartOffset, mSelectionEndOffset));
            else if (i == selStartIndex)
                builder.append(text.subSequence(mSelectionStartOffset, text.length()));
            else if (i == selEndIndex)
                builder.append(text.subSequence(0, mSelectionEndOffset));
            else
                builder.append(text);
        }
        return builder;
    }

    public void clearSelection() {
        if (mActionModeCallback.mCurrentActionMode != null)
            mActionModeCallback.mCurrentActionMode.finish();
        if (mSelectionStartId != -1) {
            int selStartIndex = getItemPosition(mSelectionStartId);
            int selEndIndex = getItemPosition(mSelectionEndId);
            for (int i = selStartIndex; i <= selEndIndex; i++) {
                TextView textView = findTextViewByPosition(i);
                if (textView != null)
                    TextSelectionHelper.removeSelection((Spannable) textView.getText());
            }
        }
        mSelectionStartId = -1;
        mSelectionStartOffset = -1;
        mSelectionEndId = -1;
        mSelectionEndOffset = -1;
        showHandles();
    }

    public void setSelection(long startId, int startOffset, long endId, int endOffset) {
        int startIndex = getItemPosition(startId);
        int endIndex = getItemPosition(endId);
        int oldStartIndex = getItemPosition(mSelectionStartId);
        int oldEndIndex = getItemPosition(mSelectionEndId);

        if (mSelectionStartId != -1 && startIndex <= oldEndIndex && endIndex >= oldStartIndex) {
            if (startIndex > oldStartIndex) {
                for (int i = oldStartIndex; i < startIndex; i++) {
                    TextView textView = findTextViewByPosition(i);
                    if (textView != null)
                        TextSelectionHelper.removeSelection((Spannable) textView.getText());
                }
            } else if (startIndex < oldStartIndex) {
                for (int i = startIndex + 1; i <= oldStartIndex; i++) {
                    TextView textView = findTextViewByPosition(i);
                    if (textView != null)
                        TextSelectionHelper.setSelection(textView.getContext(),
                                (Spannable) textView.getText(), 0, textView.length());
                }
            }
            if (endIndex < oldEndIndex) {
                for (int i = endIndex + 1; i <= oldEndIndex; i++) {
                    TextView textView = findTextViewByPosition(i);
                    if (textView != null)
                        TextSelectionHelper.removeSelection((Spannable) textView.getText());
                }
            } else if (endIndex > oldEndIndex) {
                for (int i = oldEndIndex; i < endIndex; i++) {
                    TextView textView = findTextViewByPosition(i);
                    if (textView != null)
                        TextSelectionHelper.setSelection(textView.getContext(),
                                (Spannable) textView.getText(), 0, textView.length());
                }
            }
        } else {
            clearSelection();

            for (int i = startIndex + 1; i < endIndex; i++) {
                TextView textView = findTextViewByPosition(i);
                if (textView != null)
                    TextSelectionHelper.setSelection(textView.getContext(),
                            (Spannable) textView.getText(), 0, textView.length());
            }
        }
        mSelectionStartId = startId;
        mSelectionStartOffset = startOffset;
        mSelectionEndId = endId;
        mSelectionEndOffset = endOffset;
        if (startIndex == endIndex) {
            TextView textView = findTextViewByItemId(startId);
            if (textView != null)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), startOffset, endOffset);
        } else {
            TextView textView = findTextViewByItemId(startId);
            if (textView != null)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), startOffset, textView.length());
            textView = findTextViewByItemId(endId);
            if (textView != null)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), 0, endOffset);
        }
    }

    public void applySelectionTo(View view, int position) {
        TextView textView = findTextViewIn(view);
        if (textView == null)
            return;
        int selStartIndex = ((AdapterInterface) mRecyclerView.getAdapter())
                .getItemPosition(mSelectionStartId);
        int selEndIndex = ((AdapterInterface) mRecyclerView.getAdapter())
                .getItemPosition(mSelectionEndId);
        if (position >= selStartIndex && position <= selEndIndex) {
            if (selStartIndex == selEndIndex)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), mSelectionStartOffset, mSelectionEndOffset);
            else if (position == selStartIndex)
                TextSelectionHelper.setSelection(textView.getContext(),
                        (Spannable) textView.getText(), mSelectionStartOffset, textView.length());
            else if (position == selEndIndex)
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

    private TextView findTextViewByPosition(int pos) {
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(pos);
        if (vh == null)
            return null;
        return findTextViewIn(vh.itemView);
    }

    private TextView findTextViewByItemId(long id) {
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForItemId(id);
        if (vh == null)
            return null;
        return findTextViewIn(vh.itemView);
    }

    private int getItemPosition(long id) {
        return ((AdapterInterface) mRecyclerView.getAdapter()).getItemPosition(id);
    }


    private class HandleMoveListener implements TextSelectionHandleView.MoveListener {

        private boolean mRightHandle;
        private boolean mCurrentlyRightHandle;
        private RecyclerViewScrollerRunnable mScroller;

        public HandleMoveListener(boolean rightHandle) {
            mRightHandle = rightHandle;

            mScroller = new RecyclerViewScrollerRunnable(mRecyclerView, (int scrollDir) -> {
                mRecyclerView.getLocationOnScreen(mTmpLocation);
                if (scrollDir < 0)
                    onMoved(mTmpLocation[0], mTmpLocation[1] - 1);
                else if (scrollDir > 0)
                    onMoved(mTmpLocation[0] + mRecyclerView.getWidth(),
                            mTmpLocation[1] + mRecyclerView.getHeight() + 1);
            });
        }

        @Override
        public void onMoveStarted() {
            mCurrentlyRightHandle = mRightHandle;
        }

        @Override
        public void onMoveFinished() {
            showActionMode();
            mScroller.setScrollDir(0);
        }

        @Override
        public void onMoved(float x, float y) {
            hideActionModeForSelection();

            mRecyclerView.getLocationOnScreen(mTmpLocation);

            if (y - mTmpLocation[1] < 0)
                mScroller.setScrollDir(-1);
            else if (y - mTmpLocation[1] > mRecyclerView.getHeight())
                mScroller.setScrollDir(1);
            else
                mScroller.setScrollDir(0);

            View view = mRecyclerView.findChildViewUnder(x - mTmpLocation[0],
                    y - mTmpLocation[1]);
            if (view == null)
                return;
            long id = mRecyclerView.getChildItemId(view);
            int index = mRecyclerView.getChildAdapterPosition(view);
            TextView textView = findTextViewIn(view);
            if (textView == null)
                return;
            view.getLocationOnScreen(mTmpLocation);
            int offset = textView.getOffsetForPosition(x - mTmpLocation[0],
                    y - mTmpLocation[1]);

            if (mCurrentlyRightHandle) {
                int selStartIndex = getItemPosition(mSelectionStartId);
                if (index < selStartIndex ||
                        (index == selStartIndex && offset < mSelectionStartOffset)) {
                    setSelection(id, offset, mSelectionStartId, mSelectionStartOffset);
                    mCurrentlyRightHandle = false;
                } else {
                    setSelection(mSelectionStartId, mSelectionStartOffset, id, offset);
                }
            } else {
                int selEndIndex = getItemPosition(mSelectionEndId);
                if (index > selEndIndex ||
                        (index == selEndIndex && offset > mSelectionEndOffset)) {
                    setSelection(mSelectionEndId, mSelectionEndOffset, id, offset);
                    mCurrentlyRightHandle = true;
                } else {
                    setSelection(id, offset, mSelectionEndId, mSelectionEndOffset);
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
                case R.id.action_share:
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_TEXT, getSelectedText());
                    intent.setType("text/plain");
                    mRecyclerView.getContext().startActivity(Intent.createChooser(intent,
                            mRecyclerView.getContext().getString(R.string.message_share_title)));
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

            TextView textViewStart = findTextViewByItemId(mSelectionStartId);
            TextView textViewEnd = findTextViewByItemId(mSelectionEndId);
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
                outRect.bottom += textViewEnd.getLayout().getLineBottom(lineEnd);
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
        int getItemPosition(long id);
    }

}
