package io.mrarm.irc;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class NotificationRulesAdapter extends RecyclerView.Adapter<NotificationRulesAdapter.RuleHolder> {

    private static final int TYPE_DEFAULT_RULE = 0;
    private static final int TYPE_USER_RULE = 1;

    private ItemTouchHelper mItemTouchHelper;
    private List<NotificationRule> mRules;
    private boolean mHasChanges = false;

    public NotificationRulesAdapter(Context context) {
        mRules = NotificationManager.getUserRules(context);
    }

    public boolean hasUnsavedChanges() {
        return mHasChanges;
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
                int fromPosition = viewHolder.getAdapterPosition() - NotificationManager.sDefaultTopRules.size();
                int toPosition = Math.max(Math.min(target.getAdapterPosition() - NotificationManager.sDefaultTopRules.size(), mRules.size() - 1), 0);
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++)
                        Collections.swap(mRules, i, i + 1);
                } else {
                    for (int i = fromPosition; i > toPosition; i--)
                        Collections.swap(mRules, i, i - 1);
                }
                notifyItemMoved(fromPosition + NotificationManager.sDefaultTopRules.size(), toPosition + NotificationManager.sDefaultTopRules.size());
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
        if (i < NotificationManager.sDefaultTopRules.size())
            viewHolder.bind(NotificationManager.sDefaultTopRules.get(i));
        else if (i >= NotificationManager.sDefaultTopRules.size() + mRules.size())
            viewHolder.bind(NotificationManager.sDefaultBottomRules.get(i - NotificationManager.sDefaultTopRules.size() - mRules.size()));
        else
            viewHolder.bind(mRules.get(i - NotificationManager.sDefaultTopRules.size()));
    }

    @Override
    public int getItemCount() {
        return NotificationManager.sDefaultTopRules.size() + mRules.size() + NotificationManager.sDefaultBottomRules.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < NotificationManager.sDefaultTopRules.size() || position >= NotificationManager.sDefaultTopRules.size() + mRules.size())
            return TYPE_DEFAULT_RULE;
        return TYPE_USER_RULE;
    }

    public static class RuleHolder extends RecyclerView.ViewHolder
            implements CompoundButton.OnCheckedChangeListener {

        protected NotificationRulesAdapter mAdapter;
        protected TextView mName;
        protected CheckBox mEnabled;
        protected boolean mNotEditable = false;
        protected NotificationRule mRule;

        public RuleHolder(NotificationRulesAdapter adapter, View itemView) {
            super(itemView);
            mAdapter = adapter;
            mName = (TextView) itemView.findViewById(R.id.name);
            mEnabled = (CheckBox) itemView.findViewById(R.id.enabled);
            itemView.setOnClickListener((View view) -> {
                if (mNotEditable)
                    return;
                Intent intent = new Intent(view.getContext(), EditNotificationSettingsActivity.class);
                int index = getAdapterPosition();
                if (index >= NotificationManager.sDefaultTopRules.size() + adapter.mRules.size())
                    index -= adapter.mRules.size();
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
                intent.putExtra(EditNotificationSettingsActivity.ARG_USER_RULE_INDEX, getAdapterPosition() - NotificationManager.sDefaultTopRules.size());
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

}
