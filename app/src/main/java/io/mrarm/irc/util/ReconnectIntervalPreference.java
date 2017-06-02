package io.mrarm.irc.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.preference.Preference;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConfigManager;

public class ReconnectIntervalPreference extends Preference {

    private static List<Rule> mDefaultValue;
    private static Type mListRuleType = new TypeToken<List<Rule>>(){}.getType();

    static {
        mDefaultValue = new ArrayList<>();
        mDefaultValue.add(new Rule(5, 3));
        mDefaultValue.add(new Rule(30, -1));
    }

    public static List<Rule> getDefaultValue() {
        return mDefaultValue;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReconnectIntervalPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ReconnectIntervalPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ReconnectIntervalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReconnectIntervalPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onClick() {
        List<Rule> rules = getDefaultValue();
        try {
            ServerConfigManager.getGson().fromJson(getPersistedString(null), mListRuleType);
        } catch (Exception ignored) {
        }
        RulesAdapter adapter = new RulesAdapter(rules);

        View dialogView = buildDialogView(adapter);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setPositiveButton(R.string.action_ok, (DialogInterface dialogInterface, int which) -> {
                    String newValue = ServerConfigManager.getGson().toJson(rules);
                    if (callChangeListener(newValue))
                        persistString(newValue);
                })
                .setView(dialogView)
                .setTitle(getTitle())
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        adapter.setDialog(dialog);
        dialog.show();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    private View buildDialogView(RulesAdapter rules) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.settings_reconnect_dialog, null);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rules);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerView.setAdapter(rules);

        return view;
    }

    public static class Rule {

        public int reconnectDelay = -1;
        public int repeatCount = -1;

        public Rule() {
        }

        public Rule(int reconnectDelay, int repeatCount) {
            this.reconnectDelay = reconnectDelay;
            this.repeatCount = repeatCount;
        }

    }

    public static class RulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private AlertDialog mDialog;
        private List<Rule> mRules;
        private boolean mCurrentOkButtonState = true;

        public RulesAdapter(List<Rule> rules) {
            mRules = rules;
        }

        public void setDialog(AlertDialog dialog) {
            mDialog = dialog;
        }

        private void updateDialogOkButtonState() {
            boolean isDataValid = (mRules.size() > 0);
            for (int i = 0; i < mRules.size(); i++) {
                Rule rule = mRules.get(i);
                if (rule.reconnectDelay == -1 || (rule.repeatCount == -1 && i != mRules.size() - 1)) {
                    isDataValid = false;
                    break;
                }
            }
            mCurrentOkButtonState = isDataValid;
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isDataValid);
        }

        private void updateDialogOkButtonState(boolean newPotentialState) {
            if (newPotentialState != mCurrentOkButtonState)
                updateDialogOkButtonState();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.settings_reconnect_rule, viewGroup, false);
            return new RuleViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            ((RuleViewHolder) viewHolder).bind(mRules.get(position));
        }

        @Override
        public int getItemCount() {
            return mRules.size();
        }

        public static class RuleViewHolder extends RecyclerView.ViewHolder {

            private EditText mReconnectDelayText;
            private EditText mRepeatCountText;

            public RuleViewHolder(View v, RulesAdapter adapter) {
                super(v);

                Spinner spinner = (Spinner) v.findViewById(R.id.rule_duration_type);
                ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(v.getContext(),
                        R.array.duration_types, android.R.layout.simple_spinner_item);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(spinnerAdapter);

                mReconnectDelayText = (EditText) v.findViewById(R.id.rule_interval);
                mRepeatCountText = (EditText) v.findViewById(R.id.rule_repeat_times);

                View more = v.findViewById(R.id.rule_more);
                more.setOnClickListener((View view) -> {
                    PopupMenu menu = new PopupMenu(view.getContext(), view, GravityCompat.END);
                    MenuInflater inflater = menu.getMenuInflater();
                    inflater.inflate(R.menu.menu_reconnect_rule, menu.getMenu());
                    menu.setOnMenuItemClickListener((MenuItem item) -> {
                        if (item.getItemId() == R.id.action_add) {
                            adapter.mRules.add(getAdapterPosition() + 1, new Rule());
                            adapter.notifyItemInserted(getAdapterPosition() + 1);
                            return true;
                        } else if (item.getItemId() == R.id.action_delete) {
                            if (adapter.mRules.size() > 1) {
                                adapter.mRules.remove(getAdapterPosition());
                                adapter.notifyItemRemoved(getAdapterPosition());
                            }
                            return true;
                        }
                        return false;
                    });
                    menu.show();
                });

                mReconnectDelayText.addTextChangedListener(new StubTextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        Rule rule = adapter.mRules.get(getAdapterPosition());
                        try {
                            rule.reconnectDelay = Integer.parseInt(mReconnectDelayText.getText().toString()); // TODO: * the selected time type
                        } catch (NumberFormatException e) {
                            rule.reconnectDelay = -1;
                        }
                        adapter.updateDialogOkButtonState(rule.reconnectDelay > 0);
                    }
                });
                mRepeatCountText.addTextChangedListener(new StubTextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        Rule rule = adapter.mRules.get(getAdapterPosition());
                        try {
                            rule.repeatCount = Integer.parseInt(mRepeatCountText.getText().toString());
                        } catch (NumberFormatException e) {
                            rule.repeatCount = -1;
                        }
                        adapter.updateDialogOkButtonState(rule.repeatCount > 0);

                        if (getAdapterPosition() == adapter.mRules.size() - 1) { // last item
                            if (mRepeatCountText.getText().length() > 0) {
                                // add a new empty item
                                adapter.mRules.add(getAdapterPosition() + 1, new Rule());
                                adapter.notifyItemInserted(getAdapterPosition() + 1);
                            }
                        } else if (getAdapterPosition() == adapter.mRules.size() - 2) {
                            int ii = adapter.mRules.size() - 1;
                            Rule lastRule = adapter.mRules.get(ii);
                            if (lastRule.reconnectDelay == -1 && lastRule.repeatCount == -1) {
                                // remove last, empty rule
                                adapter.mRules.remove(ii);
                                adapter.notifyItemRemoved(ii);
                            }
                        }
                    }
                });
            }

            public void bind(Rule rule) {
                mReconnectDelayText.setText(rule.reconnectDelay == -1 ? "" : String.valueOf(rule.reconnectDelay));
                mRepeatCountText.setText(rule.repeatCount == -1 ? "" : String.valueOf(rule.repeatCount));
            }

        }

    }

}
