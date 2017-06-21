package io.mrarm.irc;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationRulesAdapter extends RecyclerView.Adapter<NotificationRulesAdapter.RuleHolder> {

    private static final int TYPE_DEFAULT_RULE = 0;
    private static final int TYPE_USER_RULE = 1;

    private ItemTouchHelper mItemTouchHelper;
    private List<NotificationRule> mRules;
    private List<NotificationRule> mDefaultTopRules;
    private List<NotificationRule> mDefaultBottomRules;

    public NotificationRulesAdapter(List<NotificationRule> rules,
                                    List<NotificationRule> defaultTop,
                                    List<NotificationRule> defaultBottom) {
        mRules = rules;
        mDefaultTopRules = defaultTop;
        mDefaultBottomRules = defaultBottom;
    }

    public NotificationRulesAdapter(List<NotificationRule> rules) {
        mRules = rules;
        mDefaultTopRules = new ArrayList<>();
        mDefaultTopRules.add(NotificationManager.sZNCPlaybackRule);
        mDefaultBottomRules = new ArrayList<>();
        mDefaultBottomRules.add(NotificationManager.sNickMentionRule);
        mDefaultBottomRules.add(NotificationManager.sDirectMessageRule);
        mDefaultBottomRules.add(NotificationManager.sNoticeRule);
        mDefaultBottomRules.add(NotificationManager.sChannelNoticeRule);
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
                int fromPosition = viewHolder.getAdapterPosition() - mDefaultTopRules.size();
                int toPosition = Math.max(Math.min(target.getAdapterPosition() - mDefaultTopRules.size(), mRules.size() - 1), 0);
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++)
                        Collections.swap(mRules, i, i + 1);
                } else {
                    for (int i = fromPosition; i > toPosition; i--)
                        Collections.swap(mRules, i, i - 1);
                }
                notifyItemMoved(fromPosition + mDefaultTopRules.size(), toPosition + mDefaultTopRules.size());
                return true;
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
    public RuleHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        if (type == TYPE_DEFAULT_RULE) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.settings_notifications_rule_default, viewGroup, false);
            return new RuleHolder(this, view);
        }
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.settings_notifications_rule, viewGroup, false);
        return new UserRuleHolder(this, view);
    }

    @Override
    public void onBindViewHolder(RuleHolder viewHolder, int i) {
        if (i < mDefaultTopRules.size())
            viewHolder.bind(mDefaultTopRules.get(i));
        else if (i >= mDefaultTopRules.size() + mRules.size())
            viewHolder.bind(mDefaultBottomRules.get(i - mDefaultTopRules.size() - mRules.size()));
        else
            viewHolder.bind(mRules.get(i - mDefaultTopRules.size()));
    }

    @Override
    public int getItemCount() {
        return mDefaultTopRules.size() + mRules.size() + mDefaultBottomRules.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mDefaultTopRules.size() || position >= mDefaultTopRules.size() + mRules.size())
            return TYPE_DEFAULT_RULE;
        return TYPE_USER_RULE;
    }

    public static class RuleHolder extends RecyclerView.ViewHolder {

        protected TextView mName;
        protected CheckBox mEnabled;

        public RuleHolder(NotificationRulesAdapter adapter, View itemView) {
            super(itemView);
            itemView.setOnClickListener((View view) -> {
                //
            });
            mName = (TextView) itemView.findViewById(R.id.name);
            mEnabled = (CheckBox) itemView.findViewById(R.id.enabled);
        }

        public void bind(NotificationRule rule) {
            if (rule.getNameId() != -1)
                mName.setText(rule.getNameId());
            else
                mName.setText(rule.getName());
            mEnabled.setChecked(rule.settings.enabled);
        }

    }

    public static class UserRuleHolder extends RuleHolder {

        public UserRuleHolder(NotificationRulesAdapter adapter, View itemView) {
            super(adapter, itemView);
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

}
