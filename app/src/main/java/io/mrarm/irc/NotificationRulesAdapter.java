package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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

import io.mrarm.irc.util.AdvancedDividerItemDecoration;

public class NotificationRulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DEFAULT_RULE = 0;
    private static final int TYPE_USER_RULE = 1;
    private static final int TYPE_HEADER = 2;
    private static final int TYPE_TIP = 3;

    private ItemTouchHelper mItemTouchHelper;
    private List<NotificationRule> mRules;
    private List<NotificationRule> mDefaultRules;
    private boolean mHasChanges = false;
    private int mDragItemBgColor;

    public NotificationRulesAdapter(Context context) {
        mRules = NotificationManager.getUserRules(context);

        mDefaultRules = new ArrayList<>();
        mDefaultRules.add(NotificationManager.sNickMentionRule);
        mDefaultRules.add(NotificationManager.sDirectMessageRule);
        mDefaultRules.add(NotificationManager.sDirectNoticeRule);
        mDefaultRules.add(NotificationManager.sChannelNoticeRule);
        mDefaultRules.add(NotificationManager.sZNCPlaybackRule);

        TypedArray ta = context.getTheme().obtainStyledAttributes(R.style.AppTheme,
                new int[] { R.attr.colorBackgroundFloating });
        mDragItemBgColor = ta.getColor(0, Color.BLACK);
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
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
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
                return true;
            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (Build.VERSION.SDK_INT >= 21)
                    viewHolder.itemView.setElevation(isCurrentlyActive ? 2.f : 0.f);
                if (isCurrentlyActive)
                    viewHolder.itemView.setBackgroundColor(mDragItemBgColor);
                else
                    viewHolder.itemView.setBackgroundDrawable(null);
                if (viewHolder instanceof RuleHolder)
                    ((RuleHolder) viewHolder).mNoDivider = isCurrentlyActive;
                super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
                // stub
            }

        };
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
        public boolean mNoDivider = false;

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
            mName = (TextView) itemView.findViewById(R.id.name);
            mEnabled = (CheckBox) itemView.findViewById(R.id.enabled);
            itemView.findViewById(R.id.enabled_area).setOnClickListener((View view) -> {
                mEnabled.setChecked(!mEnabled.isChecked());
            });
            itemView.setOnClickListener((View view) -> {
                if (mNotEditable)
                    return;
                Intent intent = new Intent(view.getContext(),
                        EditNotificationSettingsActivity.class);
                int index = findDefaultRuleIndex(mRule, NotificationManager.sDefaultTopRules);
                if (index == -1)
                    index = findDefaultRuleIndex(mRule, NotificationManager.sDefaultBottomRules) +
                            NotificationManager.sDefaultTopRules.size();
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
            mText = (TextView) view.findViewById(R.id.text);
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
            return viewType != TYPE_HEADER && viewType != TYPE_TIP && !(viewType != TYPE_USER_RULE
                    && ((RuleHolder) holder).mNoDivider);
        }
    }

}
