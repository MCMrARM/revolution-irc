package io.mrarm.irc.chat;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.mrarm.chatlib.dto.MessageId;
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
    private static final int TYPE_MESSAGE_WITH_NEW_MESSAGE_MARKER = 2;

    private ChatMessagesFragment mFragment;
    private List<Item> mMessages;
    private List<Item> mPrependedMessages;
    private LongPressSelectTouchListener mMultiSelectListener;
    private ChatSelectTouchListener mSelectListener;
    private Set<Long> mSelectedItems = new TreeSet<>();
    private Set<BaseHolder> mSelectedVH = new HashSet<>();
    private Drawable mItemBackground;
    private Drawable mSelectedItemBackground;
    private Typeface mTypeface;
    private int mFontSize;
    private long mItemIdOffset = -1000000000L;

    // Used to display the day marker
    private int mFirstMessageDay = -1;
    private int mLastMessageDay = -1;

    private MessageId mNewMessagesStart;

    public ChatMessagesAdapter(ChatMessagesFragment fragment, List<MessageInfo> messages,
                               List<MessageId> messageIds) {
        mFragment = fragment;
        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(fragment.getContext(),
                new int[] { R.attr.selectableItemBackground, R.attr.colorControlHighlight });
        // mItemBackground = ta.getDrawable(R.attr.selectableItemBackground);
        int color = ta.getColor(R.attr.colorControlHighlight, 0);
        //color = ColorUtils.setAlphaComponent(color, Color.alpha(color) / 2);
        mSelectedItemBackground = new ColorDrawable(color);
        ta.recycle();

        setMessages(messages, messageIds);
        setHasStableIds(true);
    }

    public void setNewMessagesStart(MessageId start) {
        int oldi = findMessageWithId(mNewMessagesStart);
        mNewMessagesStart = start;
        int i = findMessageWithId(start);
        if (oldi != -1)
            notifyItemChanged(oldi);
        if (i != -1)
            notifyItemChanged(i);
    }

    public MessageId getNewMessagesStart() {
        return mNewMessagesStart;
    }

    public void setMessageFont(Typeface typeface, int fontSize) {
        mTypeface = typeface;
        mFontSize = fontSize;
    }

    public Item getMessage(int index) {
        if (index < mPrependedMessages.size())
            return mPrependedMessages.get(mPrependedMessages.size() - 1 - index);
        index -= mPrependedMessages.size();
        if (index < mMessages.size())
            return mMessages.get(index);
        return null;
    }

    public int findMessageWithId(MessageId id) {
        if (id == null)
            return -1;
        for (int i = mPrependedMessages.size() - 1; i >= 0; --i) {
            Item it = mPrependedMessages.get(i);
            if (it instanceof MessageItem && ((MessageItem) it).mMessageId.equals(id))
                return mPrependedMessages.size() - 1 - i;
        }
        for (int i = mMessages.size() - 1; i >= 0; --i) {
            Item it = mMessages.get(i);
            if (it instanceof MessageItem && ((MessageItem) it).mMessageId.equals(id))
                return mPrependedMessages.size() + i;
        }
        return -1;
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

    private int appendMessageInternal(MessageInfo m, MessageId mi) {
        int ret = 0;
        int day = getDayInt(m.getDate());
        if (mFirstMessageDay == -1)
            mFirstMessageDay = day;
        if (day != mLastMessageDay) {
            mMessages.add(new DayMarkerItem(day));
            mLastMessageDay = day;
            ret++;
        }
        mMessages.add(new MessageItem(m, mi));
        ret++;
        return ret;
    }

    private int prependMessageInternal(MessageInfo m, MessageId mi) {
        int ret = 0;
        int day = getDayInt(m.getDate());
        if (mLastMessageDay == -1)
            mLastMessageDay = day;
        if (day != mFirstMessageDay) {
            mPrependedMessages.add(new DayMarkerItem(day));
            mFirstMessageDay = day;
            ret++;
        }
        mPrependedMessages.add(new MessageItem(m, mi));
        ret++;
        return ret;
    }

    public void appendMessage(MessageInfo m, MessageId mi) {
        int c = appendMessageInternal(m, mi);
        if (c == 1)
            notifyItemInserted(mMessages.size() - 1);
        else
            notifyItemRangeInserted(mMessages.size() - c, c);
    }

    public void setMessages(List<MessageInfo> messages, List<MessageId> messageIds) {
        mMessages = new ArrayList<>();
        mPrependedMessages = new ArrayList<>();
        int n = messages.size();
        for (int i = 0; i < n; i++)
            appendMessageInternal(messages.get(i), messageIds.get(i));
        notifyDataSetChanged();
    }

    public void addMessagesToTop(List<MessageInfo> messages, List<MessageId> messageIds) {
        if (messages.size() == 0)
            return;
        if (getMessage(0) instanceof DayMarkerItem) {
            deleteMessageInternal(0);
            notifyItemRangeRemoved(0, 1);
            mItemIdOffset -= 1;
        }
        int cnt = 0;
        for (int i = messages.size() - 1; i >= 0; --i)
            cnt += prependMessageInternal(messages.get(i), messageIds.get(i));
        mPrependedMessages.add(new DayMarkerItem(mFirstMessageDay));
        ++cnt;
        mItemIdOffset += cnt;
        notifyItemRangeInserted(0, cnt);
    }

    public void addMessagesToBottom(List<MessageInfo> messages, List<MessageId> messageIds) {
        if (messages.size() == 0)
            return;
        int appendAt = getItemCount();
        int cnt = 0;
        int n = messages.size();
        for (int i = 0; i < n; i++)
            cnt += appendMessageInternal(messages.get(i), messageIds.get(i));
        notifyItemRangeInserted(appendAt, cnt);
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

    public Set<Long> getSelectedItems() {
        return mSelectedItems;
    }

    public List<MessageId> getSelectedMessageIds() {
        Set<Long> items = getSelectedItems();
        List<MessageId> ret = new ArrayList<>();
        for (Long msgIndex : items) {
            Item item = getMessage(getItemPosition(msgIndex));
            if (item instanceof MessageItem)
                ret.add(((MessageItem) item).mMessageId);
        }
        return ret;
    }

    public CharSequence getSelectedMessages() {
        Set<Long> items = getSelectedItems();
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean first = true;
        for (Long msgIndex : items) {
            if (first)
                first = false;
            else
                builder.append('\n');
            builder.append(getTextAt(getItemPosition(msgIndex)));
        }
        return builder;
    }

    public void clearSelection() {
        for (BaseHolder viewHolder : mSelectedVH)
            viewHolder.setSelected(false, false, false);
        mSelectedItems.clear();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_MESSAGE) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_message, viewGroup, false);
            return new MessageHolder(view);
        }
        if (viewType == TYPE_MESSAGE_WITH_NEW_MESSAGE_MARKER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.chat_new_messages_marker, viewGroup, false);
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
        if (viewType == TYPE_MESSAGE || viewType == TYPE_MESSAGE_WITH_NEW_MESSAGE_MARKER) {
            ((MessageHolder) holder).bind((MessageItem) msg);
        } else if (viewType == TYPE_DAY_MARKER) {
            ((DayMarkerHolder) holder).bind((DayMarkerItem) msg);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        ((BaseHolder) holder).unbind();
    }

    @Override
    public CharSequence getTextAt(int position) {
        Object msg = getMessage(position);
        if (msg instanceof MessageItem)
            return MessageBuilder.getInstance(mFragment.getContext())
                    .buildMessage(((MessageItem) msg).mMessage);
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
        if (m instanceof MessageItem) {
            if (((MessageItem) m).mMessageId.equals(mNewMessagesStart))
                return TYPE_MESSAGE_WITH_NEW_MESSAGE_MARKER;
            return TYPE_MESSAGE;
        }
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
    public void onElementSelected(RecyclerView recyclerView, long itemId) {
        if (mSelectedItems.size() == 0)
            mFragment.showMessagesActionMenu();
        mSelectedItems.add(itemId);
        onElementHighlighted(recyclerView, itemId, true);
    }

    @Override
    public void onElementHighlighted(RecyclerView recyclerView, long itemId, boolean highlight) {
        BaseHolder holder = (BaseHolder) recyclerView.findViewHolderForItemId(itemId);
        if (holder != null)
            holder.setSelected(highlight || mSelectedItems.contains(itemId), false);
    }

    private abstract class BaseHolder extends RecyclerView.ViewHolder {

        protected boolean mSelected = false;

        public BaseHolder(View itemView) {
            super(itemView);
        }

        public boolean isSelected() {
            return mSelected;
        }

        public void setSelected(boolean selected, boolean updateAdapter, boolean updateVHList) {
            if (updateVHList) {
                if (selected)
                    mSelectedVH.add(this);
                else
                    mSelectedVH.remove(this);
            }
            if (mSelected == selected)
                return;
            mSelected = selected;
            if (updateAdapter) {
                if (selected)
                    mSelectedItems.add(getItemId());
                else
                    mSelectedItems.remove(getItemId());
            }
            itemView.setBackground(selected
                    ? mSelectedItemBackground.getConstantState().newDrawable()
                    : (mItemBackground != null ? mItemBackground.getConstantState().newDrawable() : null));
            if (mSelectedItems.size() == 0)
                mFragment.hideMessagesActionMenu();
        }

        public void setSelected(boolean selected, boolean updateAdapter) {
            setSelected(selected, updateAdapter, true);
        }

        public void unbind() {
            mSelectedVH.remove(this);
        }

    }

    public class MessageHolder extends BaseHolder {

        private TextView mText;
        private ViewGroup.LayoutParams mDefaultLayoutParams;

        public MessageHolder(View v) {
            super(v);
            mDefaultLayoutParams = v.getLayoutParams();
            mText = v.findViewById(R.id.chat_message);
            mText.setOnClickListener((View view) -> {
                if (mSelectedItems.size() > 0)
                    setSelected(!isSelected(), true);
            });
            mText.setOnLongClickListener((View view) -> {
                if (mSelectListener != null && mSelectedItems.size() == 0) {
                    mSelectListener.startLongPressSelect();
                } else {
                    mMultiSelectListener.startSelectMode(getItemId());
                }
                return true;
            });
            if (mItemBackground != null)
                mText.setBackground(mItemBackground.getConstantState().newDrawable());
            else
                mText.setBackground(null);
            mText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        public void bind(MessageItem item) {
            itemView.setVisibility(item.mHidden ? View.GONE : View.VISIBLE);
            if (item.mHidden)
                itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            else if (itemView.getLayoutParams() != mDefaultLayoutParams)
                itemView.setLayoutParams(mDefaultLayoutParams);
            MessageInfo message = item.mMessage;
            if (mTypeface != null)
                mText.setTypeface(mTypeface);
            if (mFontSize != -1)
                mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSize);

            if (mMultiSelectListener != null)
                setSelected(mSelectedItems.contains(getItemId()) ||
                        mMultiSelectListener.isElementHighlighted(getItemId()), false);

            if (NotificationManager.getInstance().shouldMessageUseMentionFormatting(mFragment.getConnectionInfo(), mFragment.getChannelName(), message))
                mText.setText(AlignToPointSpan.apply(mText, MessageBuilder.getInstance(mText.getContext()).buildMessageWithMention(message)));
            else
                mText.setText(AlignToPointSpan.apply(mText, MessageBuilder.getInstance(mText.getContext()).buildMessage(message)));

            if (mSelectListener != null)
                mSelectListener.applySelectionTo(itemView, getAdapterPosition());
        }

    }

    public static class Item {
    }

    public static class MessageItem extends Item {

        MessageInfo mMessage;
        MessageId mMessageId;
        boolean mHidden;

        public MessageItem(MessageInfo message, MessageId msgId) {
            mMessage = message;
            mMessageId = msgId;
        }

    }

    public static class DayMarkerItem extends Item {

        int mDate;

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
