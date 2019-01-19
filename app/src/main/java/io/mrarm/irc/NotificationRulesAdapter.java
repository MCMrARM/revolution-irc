package io.mrarm.irc;

import android.animation.ValueAnimator;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mrarm.irc.config.NotificationRuleManager;
import io.mrarm.irc.config.NotificationRule;
import io.mrarm.irc.util.AdvancedDividerItemDecoration;
import io.mrarm.irc.util.StyledAttributesHelper;

public class NotificationRulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DEFAULT_RULE = 0;
    private static final int TYPE_USER_RULE = 1;
    private static final int TYPE_HEADER = 2;
    private static final int TYPE_TIP = 3;

    private ItemTouchHelper mItemTouchHelper;
    private List<NotificationRule> mRules;
    private List<NotificationRule> mDefaultRules;
    private boolean mHasChanges = false;
    private int mNormalBgColor;
    private int mDragItemBgColor;

    public NotificationRulesAdapter(Context context) {
        mRules = NotificationRuleManager.getUserRules(context);

        mDefaultRules = new ArrayList<>();
        mDefaultRules.add(NotificationRuleManager.sNickMentionRule);
        mDefaultRules.add(NotificationRuleManager.sDirectMessageRule);
        mDefaultRules.add(NotificationRuleManager.sDirectNoticeRule);
        mDefaultRules.add(NotificationRuleManager.sChannelNoticeRule);
        mDefaultRules.add(NotificationRuleManager.sZNCPlaybackRule);

        StyledAttributesHelper ta = StyledAttributesHelper.obtainStyledAttributes(context,
                R.style.AppTheme, new int[] { android.R.attr.colorBackground, R.attr.colorBackgroundFloating });
        mNormalBgColor = ta.getColor(android.R.attr.colorBackground, Color.BLACK);
        mDragItemBgColor = ta.getColor(R.attr.colorBackgroundFloating, Color.BLACK);
        ta.recycle();
    }

    public boolean hasUnsavedChanges() {
        return mHasChanges;
    }

    public ItemDecoration createItemDecoration(Context context) {
        return new ItemDecoration(context);
    }

    private int getDefaultRulesStartIndex() {
        return 0;
    }

    private int getUserRulesStartIndex() {
        return mDefaultRules.size() + 1;
    }

    public void enableDragDrop(RecyclerView recyclerView) {
        ItemTouchHelper.Callback callback = new MyItemTouchHelperCallback(recyclerView.getContext());
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        if (type == TYPE_DEFAULT_RULE) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.settings_notifications_rule_default, viewGroup, false);
            return new RuleHolder(this, view);
        } else if (type == TYPE_USER_RULE) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.settings_notifications_rule, viewGroup, false);
            return new UserRuleHolder(this, view);
        } else if (type == TYPE_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.settings_list_header, viewGroup, false);
            return new HeaderHolder(view);
        } else if (type == TYPE_TIP) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.settings_list_tip, viewGroup, false);
            return new TipHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        int defRulesI = getDefaultRulesStartIndex();
        int userRulesI = getUserRulesStartIndex();
        if (i >= userRulesI && i - userRulesI < mRules.size())
            ((RuleHolder) viewHolder).bind(mRules.get(i - userRulesI));
        else if (i >= defRulesI && i - defRulesI < mDefaultRules.size())
            ((RuleHolder) viewHolder).bind(mDefaultRules.get(i - defRulesI));
        else if (i == userRulesI - 1)
            ((HeaderHolder) viewHolder).bind(R.string.notification_custom_rules);
        else if (mRules.size() == 0 && i == userRulesI)
            ((TipHolder) viewHolder).bind(R.string.notification_tip_no_custom_rules);
    }

    @Override
    public int getItemCount() {
        int rulesCount = mRules.size();
        return mDefaultRules.size() + 1 + (rulesCount == 0 ? 1 : rulesCount);
    }

    @Override
    public int getItemViewType(int i) {
        int userRulesI = getUserRulesStartIndex();
        int defRulesI = getDefaultRulesStartIndex();
        if (i >= userRulesI && i - userRulesI < mRules.size())
            return TYPE_USER_RULE;
        if (i >= defRulesI && i - defRulesI < mDefaultRules.size())
            return TYPE_DEFAULT_RULE;
        if (i == userRulesI - 1)
            return TYPE_HEADER;
        if (mRules.size() == 0 && i == userRulesI)
            return TYPE_TIP;
        return -1;
    }

    public static class RuleHolder extends RecyclerView.ViewHolder
            implements CompoundButton.OnCheckedChangeListener {

        protected NotificationRulesAdapter mAdapter;
        protected TextView mName;
        protected CheckBox mEnabled;
        protected boolean mNotEditable = false;
        protected NotificationRule mRule;
        public boolean mIsDragged = false;
        public ValueAnimator mDragAnimator;

        private static int findDefaultRuleIndex(NotificationRule rule, List<NotificationRule> findIn) {
            for (int i = 0; i < findIn.size(); i++) {
                if (findIn.get(i) == rule)
                    return i;
            }
            return -1;
        }

        public RuleHolder(NotificationRulesAdapter adapter, View itemView) {
            super(itemView);
            mAdapter = adapter;
            mName = itemView.findViewById(R.id.name);
            mEnabled = itemView.findViewById(R.id.enabled);
            itemView.findViewById(R.id.enabled_area).setOnClickListener((View view) -> {
                mEnabled.setChecked(!mEnabled.isChecked());
            });
            itemView.setOnClickListener((View view) -> {
                if (mNotEditable)
                    return;
                Intent intent = new Intent(view.getContext(),
                        EditNotificationSettingsActivity.class);
                int index = findDefaultRuleIndex(mRule, NotificationRuleManager.getDefaultTopRules());
                if (index == -1)
                    index = findDefaultRuleIndex(mRule, NotificationRuleManager.getDefaultBottomRules())
                            + NotificationRuleManager.getDefaultTopRules().size();
                intent.putExtra(EditNotificationSettingsActivity.ARG_DEFAULT_RULE_INDEX, index);
                view.getContext().startActivity(intent);
            });
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mRule.settings.enabled = isChecked;
            mAdapter.mHasChanges = true;
        }

        public void bind(NotificationRule rule) {
            mRule = rule;
            if (rule.getNameId() != -1)
                mName.setText(rule.getNameId());
            else
                mName.setText(rule.getName());
            mEnabled.setOnCheckedChangeListener(null);
            mEnabled.setChecked(rule.settings.enabled);
            mEnabled.setOnCheckedChangeListener(this);
            mNotEditable = rule.notEditable;
        }

    }

    public static class UserRuleHolder extends RuleHolder {

        public UserRuleHolder(NotificationRulesAdapter adapter, View itemView) {
            super(adapter, itemView);
            itemView.setOnClickListener((View view) -> {
                Intent intent = new Intent(view.getContext(), EditNotificationSettingsActivity.class);
                intent.putExtra(EditNotificationSettingsActivity.ARG_USER_RULE_INDEX, getAdapterPosition() - adapter.getUserRulesStartIndex());
                view.getContext().startActivity(intent);
            });
            itemView.findViewById(R.id.reorder).setOnTouchListener((View v, MotionEvent e) -> {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN)
                    adapter.mItemTouchHelper.startDrag(UserRuleHolder.this);
                return false;
            });
        }

        @Override
        public void bind(NotificationRule rule) {
            super.bind(rule);
        }

    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {

        protected TextView mText;

        public HeaderHolder(View view) {
            super(view);
            mText = (TextView) view.findViewById(R.id.title);
            mText.setPadding(mText.getPaddingLeft(), mText.getPaddingTop(), mText.getPaddingRight(), mText.getResources().getDimensionPixelSize(R.dimen.notification_rule_list_header_padding));
        }

        public void bind(int textId) {
            mText.setText(textId);
        }

    }

    public static class TipHolder extends RecyclerView.ViewHolder {

        protected TextView mText;

        public TipHolder(View view) {
            super(view);
            mText = view.findViewById(R.id.text);
        }

        public void bind(int textId) {
            mText.setText(textId);
        }

    }


    public static class ItemDecoration extends AdvancedDividerItemDecoration {

        public ItemDecoration(Context context) {
            super(context);
        }

        @Override
        public boolean hasDivider(RecyclerView parent, View view) {
            RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
            int viewType = holder.getItemViewType();
            return viewType != TYPE_HEADER && viewType != TYPE_TIP && !(viewType == TYPE_USER_RULE
                    && ((RuleHolder) holder).mIsDragged);
        }

    }

    public class MyItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private Paint mSwipePaint;
        private Rect mTempRect = new Rect();
        private Drawable mDeleteIcon;
        private int mIconPadding;

        MyItemTouchHelperCallback(Context context) {
            mSwipePaint = new Paint();
            mSwipePaint.setColor(context.getResources().getColor(R.color.colorSwipeDeleteBackground));
            mDeleteIcon = AppCompatResources.getDrawable(context, R.drawable.ic_delete).mutate();
            int iconColor = context.getResources().getColor(R.color.colorSwipeIconColor);
            DrawableCompat.setTint(mDeleteIcon, iconColor);
            mIconPadding = context.getResources().getDimensionPixelSize(R.dimen.item_swipe_icon_padding);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (!(viewHolder instanceof UserRuleHolder))
                return makeMovementFlags(0, 0);
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                    ItemTouchHelper.START | ItemTouchHelper.END);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                RecyclerView.ViewHolder target) {
            int userRulesI = getUserRulesStartIndex();
            int fromPosition = viewHolder.getAdapterPosition() - userRulesI;
            int toPosition = Math.max(Math.min(target.getAdapterPosition() - userRulesI, mRules.size() - 1), 0);
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++)
                    Collections.swap(mRules, i, i + 1);
            } else {
                for (int i = fromPosition; i > toPosition; i--)
                    Collections.swap(mRules, i, i - 1);
            }
            notifyItemMoved(fromPosition + userRulesI, toPosition + userRulesI);
            mHasChanges = true;
            return true;
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                View view = viewHolder.itemView;
                c.save();
                mSwipePaint.setAlpha((int) (255 * view.getAlpha()));
                mDeleteIcon.setAlpha((int) (255 * view.getAlpha()));
                int sw = mDeleteIcon.getIntrinsicWidth();
                int sh = mDeleteIcon.getIntrinsicHeight();
                int cx;
                int cy = (view.getTop() + view.getBottom()) / 2;
                if (dX > 0.f) {
                    mTempRect.set(view.getLeft(), view.getTop(), (int) dX, view.getBottom());
                    cx = view.getLeft() + mIconPadding + sw / 2;
                } else {
                    mTempRect.set(view.getRight() + (int) dX, view.getTop(), view.getRight(), view.getBottom());
                    cx = view.getRight() - mIconPadding - sw / 2;
                }
                mDeleteIcon.setBounds(cx - sw / 2, cy - sh / 2, cx + sw / 2, cy + sh / 2);
                c.drawRect(mTempRect, mSwipePaint);
                c.clipRect(mTempRect);
                mDeleteIcon.draw(c);
                c.restore();
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (viewHolder instanceof RuleHolder) {
                RuleHolder holder = ((RuleHolder) viewHolder);
                boolean dragged = (dY != 0.f);
                if (holder.mIsDragged == dragged) {
                    super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    return;
                }
                holder.mIsDragged = dragged;
                if (Build.VERSION.SDK_INT >= 21 && dragged)
                    viewHolder.itemView.setElevation(2.f);

                if (holder.mDragAnimator == null) {
                    holder.mDragAnimator = ValueAnimator.ofFloat(0.f, 1.f);
                    holder.mDragAnimator.addUpdateListener((ValueAnimator animation) -> {
                        float f = (Float) animation.getAnimatedValue();
                        if (Build.VERSION.SDK_INT >= 21 && !holder.mIsDragged)
                            viewHolder.itemView.setElevation(2.f);
                        if (f == 0.f) {
                            viewHolder.itemView.setBackgroundDrawable(null);
                            if (Build.VERSION.SDK_INT >= 21 && !holder.mIsDragged)
                                viewHolder.itemView.setElevation(0.f);
                        } else if (f == 1.f) {
                            viewHolder.itemView.setBackgroundColor(mDragItemBgColor);
                        } else {
                            int color = ColorUtils.blendARGB(mNormalBgColor, mDragItemBgColor, f);
                            viewHolder.itemView.setBackgroundColor(color);
                        }
                    });
                    holder.mDragAnimator.setDuration(200L);
                }
                if (dragged)
                    holder.mDragAnimator.setFloatValues(0.f, 1.f);
                else
                    holder.mDragAnimator.setFloatValues(1.f, 0.f);
                holder.mDragAnimator.start();
            }
            super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
            int position = viewHolder.getAdapterPosition();
            int index = position - getUserRulesStartIndex();
            NotificationRule rule = mRules.remove(index);
            notifyItemRemoved(position);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager mgr = (NotificationManager) viewHolder.itemView.getContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                mgr.deleteNotificationChannel(rule.settings.notificationChannelId);
            }
            if (mRules.size() == 0)
                notifyItemInserted(getUserRulesStartIndex() + 1); // the tip
            Snackbar.make(viewHolder.itemView, R.string.notification_custom_rule_deleted, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.action_undo, (View v) -> {
                        int newIndex = Math.min(index, mRules.size());
                        mRules.add(newIndex, rule);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            ChannelNotificationManager.createChannel(
                                    viewHolder.itemView.getContext(), rule);
                        if (mRules.size() == 1)
                            notifyItemRemoved(getUserRulesStartIndex() + 1); // the tip
                        notifyItemInserted(newIndex + getUserRulesStartIndex());
                        mHasChanges = true;
                    })
                    .show();
            mHasChanges = true;
        }

    }

}
