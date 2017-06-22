package io.mrarm.irc;

import android.view.View;
import android.widget.TextView;

import io.mrarm.irc.util.EntryRecyclerViewAdapter;

public class SettingsListAdapter extends EntryRecyclerViewAdapter {

    public static class SimpleEntry extends Entry {

        private static final int sHolder = registerViewHolder(SimpleEntryHolder.class, R.layout.settings_list_entry);

        String mName;
        String mValue;

        public SimpleEntry(String name, String value) {
            mName = name;
            mValue = value;
        }

        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class SimpleEntryHolder extends EntryHolder<SimpleEntry> {

        private TextView mName;
        private TextView mValue;

        public SimpleEntryHolder(View itemView) {
            super(itemView);

            mName = (TextView) itemView.findViewById(R.id.name);
            mValue = (TextView) itemView.findViewById(R.id.value);
            itemView.setOnClickListener((View v) -> {
                //
            });
        }

        @Override
        public void bind(SimpleEntry entry) {
            mName.setText(entry.mName);
            mValue.setText(entry.mValue);
        }

    }


}
