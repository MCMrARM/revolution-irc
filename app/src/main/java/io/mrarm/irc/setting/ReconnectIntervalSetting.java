package io.mrarm.irc.setting;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.SimpleTextWatcher;

public class ReconnectIntervalSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    private static List<Rule> sDefaultValue;
    public static final Type sListRuleType = new TypeToken<List<Rule>>() {
    }.getType();

    static {
        sDefaultValue = new ArrayList<>();
        sDefaultValue.add(new Rule(5000, 3));
        sDefaultValue.add(new Rule(30000, -1));
        sDefaultValue = Collections.unmodifiableList(sDefaultValue);
    }

    public static List<Rule> getDefaultValue() {
        return sDefaultValue;
    }

    public static List<Rule> parseRules(String value) {
        try {
            List<Rule> ret = SettingsHelper.getGson().fromJson(value, sListRuleType);
            if (ret != null)
                return ret;
        } catch (Exception ignored) {
        }
        return new ArrayList<>(getDefaultValue());
    }

    private List<Rule> mRules;

    public ReconnectIntervalSetting(String name, List<Rule> rules) {
        super(name, null);
        mRules = rules;
    }

    public ReconnectIntervalSetting(String name) {
        super(name, null);
        mRules = getDefaultValue();
    }

    public ReconnectIntervalSetting linkPreference(SharedPreferences prefs, String pref) {
        List<Rule> rules = parseRules(prefs.getString(pref, null));
        if (rules != null)
            setRules(rules);
        setAssociatedPreference(prefs, pref);
        return this;
    }

    public void setRules(List<Rule> rules) {
        mRules = rules;
        if (hasAssociatedPreference())
            mPreferences.edit()
                    .putString(mPreferenceName, SettingsHelper.getGson().toJson(rules))
                    .apply();
        onUpdated();
    }

    @Override
    public int getViewHolder() {
        return sHolder;
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

        public Rule(Rule rule) {
            this.reconnectDelay = rule.reconnectDelay;
            this.repeatCount = rule.repeatCount;
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

            private RulesAdapter mAdapter;
            private EditText mReconnectDelayText;
            private Spinner mReconnectDelaySpinner;
            private EditText mRepeatCountText;

            public RuleViewHolder(View v, RulesAdapter adapter) {
                super(v);
                mAdapter = adapter;

                mReconnectDelaySpinner = v.findViewById(R.id.rule_duration_type);
                ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(itemView.getContext(),
                        R.layout.simple_spinner_item, android.R.id.text1,
                        itemView.getResources().getStringArray(R.array.duration_types));
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mReconnectDelaySpinner.setAdapter(spinnerAdapter);

                mReconnectDelayText = v.findViewById(R.id.rule_interval);
                mRepeatCountText = v.findViewById(R.id.rule_repeat_times);

                View more = v.findViewById(R.id.rule_more);
                more.setOnClickListener((View view) -> {
                    PopupMenu menu = new PopupMenu(view.getContext(), view, GravityCompat.END);
                    MenuInflater inflater = menu.getMenuInflater();
                    inflater.inflate(R.menu.menu_reconnect_rule, menu.getMenu());
                    menu.setOnMenuItemClickListener((MenuItem item) -> {
                        if (item.getItemId() == R.id.action_add) {
                            mAdapter.mRules.add(getAdapterPosition() + 1, new Rule());
                            mAdapter.notifyItemInserted(getAdapterPosition() + 1);
                            mAdapter.updateDialogOkButtonState(false);
                            return true;
                        } else if (item.getItemId() == R.id.action_delete) {
                            if (mAdapter.mRules.size() > 1) {
                                mAdapter.mRules.remove(getAdapterPosition());
                                mAdapter.notifyItemRemoved(getAdapterPosition());
                                mAdapter.updateDialogOkButtonState(true);
                            }
                            return true;
                        }
                        return false;
                    });
                    menu.show();
                });
            }

            private void updateReconnectDelay() {
                int delay = IntervalSetting.getInterval(mReconnectDelaySpinner, mReconnectDelayText);
                int pos = getAdapterPosition();
                if (pos == -1)
                    return;
                Rule rule = mAdapter.mRules.get(pos);
                rule.reconnectDelay = delay;
                mAdapter.updateDialogOkButtonState(rule.reconnectDelay > 0);
            }

            public void bind(Rule rule) {
                mReconnectDelayText.removeTextChangedListener(mReconnectDelayTextListener);
                mReconnectDelaySpinner.setOnItemSelectedListener(null);
                mRepeatCountText.removeTextChangedListener(mRepeatCountTextListener);

                IntervalSetting.setInterval(mReconnectDelaySpinner, mReconnectDelayText, rule.reconnectDelay);

                mRepeatCountText.setText(rule.repeatCount == -1 ? "" : String.valueOf(rule.repeatCount));

                mReconnectDelayText.addTextChangedListener(mReconnectDelayTextListener);
                mReconnectDelaySpinner.setOnItemSelectedListener(mReconnectDelaySpinnerListener);
                mRepeatCountText.addTextChangedListener(mRepeatCountTextListener);
            }

            private SimpleTextWatcher mReconnectDelayTextListener = new SimpleTextWatcher(
                    (Editable s) -> updateReconnectDelay());

            private AdapterView.OnItemSelectedListener mReconnectDelaySpinnerListener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> vp, View v, int p, long i) {
                    updateReconnectDelay();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };

            private SimpleTextWatcher mRepeatCountTextListener = new SimpleTextWatcher((Editable s) -> {
                Rule rule = mAdapter.mRules.get(getAdapterPosition());
                try {
                    rule.repeatCount = Integer.parseInt(mRepeatCountText.getText().toString());
                } catch (NumberFormatException e) {
                    rule.repeatCount = -1;
                }

                if (getAdapterPosition() == mAdapter.mRules.size() - 1) { // last item
                    if (mRepeatCountText.getText().length() > 0) {
                        // add a new empty item
                        mAdapter.mRules.add(getAdapterPosition() + 1, new Rule());
                        mAdapter.notifyItemInserted(getAdapterPosition() + 1);
                    }
                } else if (getAdapterPosition() == mAdapter.mRules.size() - 2) {
                    int ii = mAdapter.mRules.size() - 1;
                    Rule lastRule = mAdapter.mRules.get(ii);
                    if (lastRule.reconnectDelay == -1 && lastRule.repeatCount == -1) {
                        // remove last, empty rule
                        mAdapter.mRules.remove(ii);
                        mAdapter.notifyItemRemoved(ii);
                    }
                }
                mAdapter.updateDialogOkButtonState();
            });

        }

    }

    public static class Holder extends SimpleSetting.Holder<ReconnectIntervalSetting> {


        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(ReconnectIntervalSetting entry) {
            super.bind(entry);

            StringBuilder builder = new StringBuilder();
            boolean first = true;
            Context context = itemView.getContext();
            String delim = context.getString(R.string.text_comma);
            for (Rule rule : getEntry().mRules) {
                if (first)
                    first = false;
                else
                    builder.append(delim);
                builder.append(IntervalSetting.getIntervalAsString(context, rule.reconnectDelay));
                if (rule.repeatCount != -1)
                    builder.append(context.getResources().getQuantityString(R.plurals.reconnect_desc_tries, rule.repeatCount, rule.repeatCount));
            }
            setValueText(builder.length() > 0 ? builder.toString() : null);
        }

        @Override
        public void onClick(View v) {
            List<Rule> rules = new ArrayList<>();
            for (Rule rule : getEntry().mRules)
                rules.add(new Rule(rule));
            if (rules.size() == 0)
                rules.add(new Rule());

            View view = LayoutInflater.from(itemView.getContext()).inflate(
                    R.layout.settings_reconnect_dialog, null);

            RecyclerView recyclerView = view.findViewById(R.id.rules);
            RulesAdapter adapter = new RulesAdapter(rules);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerView.setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                    .setPositiveButton(R.string.action_ok, (DialogInterface dialogInterface, int which) -> {
                        getEntry().setRules(rules);
                    })
                    .setView(view)
                    .setTitle(getEntry().mName)
                    .create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            adapter.setDialog(dialog);
            dialog.show();
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

    }

}
