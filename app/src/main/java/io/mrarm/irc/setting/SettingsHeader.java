package io.mrarm.irc.setting;

import android.view.View;
import android.widget.TextView;

import io.mrarm.irc.R;

public class SettingsHeader extends SettingsListAdapter.Entry {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_header);

    protected String mTitle;

    public SettingsHeader(String title) {
        mTitle = title;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SettingsListAdapter.SettingsEntryHolder<SettingsHeader> {

        protected TextView mTitle;

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);

            mTitle = itemView.findViewById(R.id.title);
        }

        @Override
        public void bind(SettingsHeader entry) {
            mTitle.setText(entry.mTitle);
        }

        @Override
        public boolean hasDivider() {
            return false;
        }
    }

}