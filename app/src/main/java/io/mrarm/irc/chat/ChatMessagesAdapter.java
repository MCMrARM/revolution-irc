package io.mrarm.irc.chat;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.mrarm.chatlib.dto.MessageInfo;
import io.mrarm.irc.NotificationManager;
import io.mrarm.irc.R;
import io.mrarm.irc.util.AlignToPointSpan;
import io.mrarm.irc.util.LongPressSelectTouchListener;
import io.mrarm.irc.util.MessageBuilder;
import io.mrarm.irc.util.StyledAttributesHelper;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements LongPressSelectTouchListener.Listener, ChatSelectTouchListener.AdapterInterface {

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_DAY_MARKER = 1;

    private ChatMessagesFragment mFragment;
    private List<Object> mMessages;
    private List<Object> mPrependedMessages;
    private LongPressSelectTouchListener mMultiSelectListener;
    private ChatSelectTouchListener mSelectListener;
    private Set<Integer> mSelectedItems = new TreeSet<>();
    private Drawable mItemBackground;
    private Drawable mSelectedItemBackground;
    private Typeface mTypeface;
    private int mFontSize;
    private long mItemIdOffset = -1000000000L;

    // Used to display the day marker
    private int mFirstMessageDay = -1;
    private int mLastMessageDay = -1;

    public ChatMessagesAdapter(ChatMessagesFragment fragment, List<MessageInfo> messages) {
        mFragment = fragment;
        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(fragment.getContext(),
                new int[] { R.attr.selectableItemBackground, R.attr.colorControlHighlight });
        // mItemBackground = ta.getDrawable(R.attr.selectableItemBackground);
        int color = ta.getColor(R.attr.colorControlHighlight, 0);
        //color = ColorUtils.setAlphaComponent(color, Color.alpha(color) / 2);
        mSelectedItemBackground = new ColorDrawable(color);
        ta.recycle();

        setMessages(messages);
        setHasStableIds(true);
    }

    public void setMessageFont(Typeface typeface, int fontSize) {
        mTypeface = typeface;
        mFontSize = fontSize;
    }

    private Object getMessage(int index) {
        if (index < mPrependedMessages.size())
            return mPrependedMessages.get(mPrependedMessages.size() - 1 - index);
        index -= mPrependedMessages.size();
        if (index < mMessages.size())
            return mMessages.get(index);
        return null;
    }

    private void deleteMessageInternal(int index) {
        if (index < mPrependedMessages.size()) {
            mPrependedMessages.remove(mPrependedMessages.size() - 1 - index);
        } else {
            index -= mPrependedMessages.size();
            if (index < mMessages.size())
                mMessages.remove(index);
        }
    }

    private int appendMessageInternal(MessageInfo m) {
        int ret = 0;
        int day = getDayInt(m.getDate());
        if (mFirstMessageDay == -1)
            mFirstMessageDay = day;
        if (day != mLastMessageDay) {
            mMessages.add(new DayMarkerItem(day));
            mLastMessageDay = day;
            ret++;
        }
        mMessages.add(m);
        return ret;
    }

    private int prependMessageInternal(MessageInfo m) {
        int ret = 0;
        int day = getDayInt(m.getDate());
        if (mLastMessageDay == -1)
            mLastMessageDay = day;
        if (day != mFirstMessageDay) {
            mPrependedMessages.add(new DayMarkerItem(day));
            mFirstMessageDay = day;
            ret++;
        }
        mPrependedMessages.add(m);
        ret++;
        return ret;
    }

    public void appendMessage(MessageInfo m) {
        int c = appendMessageInternal(m);
        if (c == 1)
            notifyItemInserted(mMessages.size() - 1);
        else
            notifyItemRangeInserted(mMessages.size() - c, c);
    }

    public void setMessages(List<MessageInfo> messages) {
        mMessages = new ArrayList<>();
        mPrependedMessages = new ArrayList<>();
        for (MessageInfo m : messages)
            appendMessageInternal(m);
        notifyDataSetChanged();
    }

    public void addMessagesToTop(List<MessageInfo> messages) {
        if (messages.size() == 0)
            return;
        if (getMessage(0) instanceof DayMarkerItem) {
            deleteMessageInternal(0);
            notifyItemRangeRemoved(0, 1);
            mItemIdOffset -= 1;
        }
        int cnt = 0;
        for (int i = messages.size() - 1; i >= 0; --i)
            cnt += prependMessageInternal(messages.get(i));
        mPrependedMessages.add(new DayMarkerItem(mFirstMessageDay));
        ++cnt;
        mItemIdOffset += cnt;
        notifyItemRangeInserted(0, cnt);
    }

    public boolean hasMessages() {
        return mMessages != null && (mMessages.size() > 0 || mPrependedMessages.size() > 0);
    }

    public void setSelectListener(ChatSelectTouchListener selectListener) {
        mSelectListener = selectListener;
    }

    public void setMultiSelectListener(LongPressSelectTouchListener selectListener) {
        mMultiSelectListener = selectListener;
        if (selectListener != null)
            selectListener.setListener(this);
    }

    public Set<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public CharSequence getSelectedMessages() {
        Set<Integer> items = getSelectedItems();
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean first = true;
        for (Integer msgIndex : items) {
            if (first)
                first = false;
            else
                builder.append('\n');
            builder.append(getTextAt(msgIndex));
        }
        return builder;
    }

    public void clearSelection(RecyclerView recyclerView) {
        if (recyclerView != null) {
            for (int item : mSelectedItems) {
                RecyclerView.ViewHolder viewHolder = recyclerView
                        .findViewHolderForAdapterPosition(item);
                if (viewHolder == null)
                    continue;
                ((BaseHolder) viewHolder).setSelected(false, false);
            }
        }
        mSelectedItems.clear();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_MESSAGE) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_message, viewGroup, false);
            return new MessageHolder(view);
        }
        if (viewType == TYPE_DAY_MARKER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_day_marker, viewGroup, false);
            return new DayMarkerHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        Object msg = getMessage(position);
        if (viewType == TYPE_MESSAGE) {
            ((MessageHolder) holder).bind((MessageInfo) msg, position);
        } else if (viewType == TYPE_DAY_MARKER) {
            ((DayMarkerHolder) holder).bind((DayMarkerItem) msg);
        }
    }

    @Override
    public CharSequence getTextAt(int position) {
        Object msg = getMessage(position);
        if (msg instanceof MessageInfo)
            return MessageBuilder.getInstance(mFragment.getContext())
                    .buildMessage((MessageInfo) msg);
        else if (msg instanceof DayMarkerItem)
            return ((DayMarkerItem) msg).getMessageText(mFragment.getContext());
        return null;
    }

    @Override
    public int getItemCount() {
        return mPrependedMessages.size() + mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object m = getMessage(position);
        if (m instanceof MessageInfo)
            return TYPE_MESSAGE;
        if (m instanceof DayMarkerItem)
            return TYPE_DAY_MARKER;
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return position - mItemIdOffset;
    }

    @Override
    public int getItemPosition(long id) {
        return (int) (id + mItemIdOffset);
    }

    @Override
    public void onElementSelected(RecyclerView recyclerView, int adapterPos) {
        if (mSelectedItems.size() == 0)
            mFragment.showMessagesActionMenu();
        mSelectedItems.add(adapterPos);
        onElementHighlighted(recyclerView, adapterPos, true);
    }

    @Override
    public void onElementHighlighted(RecyclerView recyclerView, int adapterPos, boolean highlight) {
        BaseHolder holder = (BaseHolder) recyclerView.findViewHolderForAdapterPosition(adapterPos);
        if (holder != null)
            holder.setSelected(highlight || mSelectedItems.contains(adapterPos), false);
    }

    private abstract class BaseHolder extends RecyclerView.ViewHolder {

        protected boolean mSelected = false;

        public BaseHolder(View itemView) {
            super(itemView);
        }

        public boolean isSelected() {
            return mSelected;
        }

        public void setSelected(boolean selected, boolean updateAdapter) {
            if (mSelected == selected)
                return;
            mSelected = selected;
            if (updateAdapter) {
                if (selected)
                    mSelectedItems.add(getAdapterPosition());
                else
                    mSelectedItems.remove(getAdapterPosition());
            }
            itemView.setBackground(selected
                    ? mSelectedItemBackground.getConstantState().newDrawable()
                    : (mItemBackground != null ? mItemBackground.getConstantState().newDrawable() : null));
            if (mSelectedItems.size() == 0)
                mFragment.hideMessagesActionMenu();
        }
    }

    public class MessageHolder extends BaseHolder {

        private TextView mText;

        public MessageHolder(View v) {
            super(v);
            mText = v.findViewById(R.id.chat_message);
            v.setOnClickListener((View view) -> {
                if (mSelectedItems.size() > 0)
                    setSelected(!isSelected(), true);
            });
            v.setOnLongClickListener((View view) -> {
                if (mSelectListener != null && mSelectedItems.size() == 0) {
                    mSelectListener.startLongPressSelect();
                } else {
                    mMultiSelectListener.startSelectMode(getAdapterPosition());
                }
                return true;
            });
            if (mItemBackground != null)
                mText.setBackground(mItemBackground.getConstantState().newDrawable());
            else
                mText.setBackground(null);
            mText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        public void bind(MessageInfo message, int position) {
            if (mTypeface != null)
                mText.setTypeface(mTypeface);
            if (mFontSize != -1)
                mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);

            if (mMultiSelectListener != null)
                setSelected(mSelectedItems.contains(position) ||
                        mMultiSelectListener.isElementHightlighted(position), false);

            if (NotificationManager.getInstance().shouldMessageUseMentionFormatting(mFragment.getConnectionInfo(), mFragment.getChannelName(), message))
                mText.setText(AlignToPointSpan.apply(mText, MessageBuilder.getInstance(mText.getContext()).buildMessageWithMention(message)));
            else
                mText.setText(AlignToPointSpan.apply(mText, MessageBuilder.getInstance(mText.getContext()).buildMessage(message)));

            if (mSelectListener != null)
                mSelectListener.applySelectionTo(itemView, position);
        }

    }

    public static class DayMarkerItem {

        private int mDate;

        public DayMarkerItem(int date) {
            mDate = date;
        }

        public String getMessageText(Context ctx) {
            return DateUtils.formatDateTime(ctx, getDateIntMs(mDate), DateUtils.FORMAT_SHOW_DATE);
        }

    }

    public class DayMarkerHolder extends BaseHolder {

        private TextView mText;

        public DayMarkerHolder(View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.text);
        }

        public void bind(DayMarkerItem item) {
            if (mTypeface != null)
                mText.setTypeface(mTypeface);
            if (mFontSize != -1)
                mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);

            mText.setText(item.getMessageText(mText.getContext()));
        }

    }


    private static final Calendar sDayIntCalendar = Calendar.getInstance();
    private static final int sDaysInYear = sDayIntCalendar.getMaximum(Calendar.DAY_OF_YEAR);

    private static int getDayInt(Date date) {
        sDayIntCalendar.setTime(date);
        return sDayIntCalendar.get(Calendar.YEAR) * (sDaysInYear + 1) +
                sDayIntCalendar.get(Calendar.DAY_OF_YEAR);
    }

    private static long getDateIntMs(int date) {
        sDayIntCalendar.setTimeInMillis(0);
        sDayIntCalendar.set(Calendar.YEAR, date / (sDaysInYear + 1));
        sDayIntCalendar.set(Calendar.DAY_OF_YEAR, date % (sDaysInYear + 1));
        return sDayIntCalendar.getTimeInMillis();
    }

}
