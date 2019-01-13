package io.mrarm.irc.chat;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    private ChatMessagesFragment mFragment;
    private List<MessageInfo> mMessages;
    private LongPressSelectTouchListener mMultiSelectListener;
    private ChatSelectTouchListener mSelectListener;
    private Set<Integer> mSelectedItems = new TreeSet<>();
    private Drawable mItemBackground;
    private Drawable mSelectedItemBackground;
    private Typeface mTypeface;
    private int mFontSize;
    private long mItemIdOffset = -1000000000L;

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

    public void setMessages(List<MessageInfo> messages) {
        this.mMessages = messages;
        notifyDataSetChanged();
    }

    public void addMessagesToTop(List<MessageInfo> messages) {
        mMessages.addAll(0, messages);
        mItemIdOffset += messages.size();
        notifyItemRangeInserted(0, messages.size());
    }

    public boolean hasMessages() {
        return mMessages != null && mMessages.size() > 0;
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

    public void clearSelection(RecyclerView recyclerView) {
        if (recyclerView != null) {
            for (int item : mSelectedItems) {
                RecyclerView.ViewHolder viewHolder = recyclerView
                        .findViewHolderForAdapterPosition(item);
                if (viewHolder == null)
                    continue;
                ((MessageHolder) viewHolder).setSelected(false, false);
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
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        if (viewType == TYPE_MESSAGE) {
            ((MessageHolder) holder).bind(mMessages.get(position), position);
        }
    }

    @Override
    public CharSequence getTextAt(int position) {
        return MessageBuilder.getInstance(mFragment.getContext())
                .buildMessage(mMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_MESSAGE;
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
        MessageHolder holder = ((MessageHolder) recyclerView.findViewHolderForAdapterPosition(adapterPos));
        if (holder != null)
            holder.setSelected(highlight || mSelectedItems.contains(adapterPos), false);
    }

    public class MessageHolder extends RecyclerView.ViewHolder {

        private TextView mText;
        private boolean mSelected = false;

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
            mText.setBackground(selected
                    ? mSelectedItemBackground.getConstantState().newDrawable()
                    : (mItemBackground != null ? mItemBackground.getConstantState().newDrawable() : null));
            if (mSelectedItems.size() == 0)
                mFragment.hideMessagesActionMenu();
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

}
