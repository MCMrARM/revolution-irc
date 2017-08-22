package io.mrarm.irc.setting;

import android.content.Intent;
import android.view.View;

import io.mrarm.irc.R;

public class ClickableSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    private View.OnClickListener mListener;

    public ClickableSetting(String name, String desc) {
        super(name, desc);
    }

    public void setDescription(CharSequence desc) {
        mValue = desc;
        onUpdated();
    }

    public ClickableSetting setOnClickListener(View.OnClickListener listener) {
        mListener = listener;
        return this;
    }

    public ClickableSetting setIntent(Intent intent) {
        setOnClickListener((View v) -> {
            v.getContext().startActivity(intent);
        });
        return this;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<ClickableSetting> {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void onClick(View v) {
            getEntry().mListener.onClick(v);
        }

    }


}
