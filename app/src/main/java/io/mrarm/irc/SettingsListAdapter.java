package io.mrarm.irc;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.util.AdvancedDividerItemDecoration;
import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleCounter;

public class SettingsListAdapter extends EntryRecyclerViewAdapter {

    private Activity mActivity;
    private SimpleCounter mRequestCodeCounter;

    public SettingsListAdapter(Activity activity) {
        mActivity = activity;
    }

    public ItemDecoration createItemDecoration() {
        return new ItemDecoration(mActivity);
    }

    public void setRequestCodeCounter(SimpleCounter counter) {
        mRequestCodeCounter = counter;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Entry e : mEntries) {
            if (e instanceof ActivityResultCallback)
                ((ActivityResultCallback) e).onActivityResult(requestCode, resultCode, data);
        }
    }

    private interface ActivityResultCallback {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    public abstract static class SettingsEntryHolder<T extends Entry> extends EntryHolder<T> {

        protected SettingsListAdapter mAdapter;

        public SettingsEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView);
            mAdapter = adapter;
        }

        public boolean hasDivider() {
            return getAdapterPosition() != mAdapter.mEntries.size() - 1;
        }

    }

    public static class ItemDecoration extends AdvancedDividerItemDecoration {

        public ItemDecoration(Context context) {
            super(context);
        }

        @Override
        public boolean hasDivider(RecyclerView parent, View view) {
            return ((SettingsListAdapter.SettingsEntryHolder) parent.getChildViewHolder(view))
                    .hasDivider();
        }
    }

    public static class HeaderEntry extends Entry {

        private static final int sHolder = registerViewHolder(HeaderEntryHolder.class, R.layout.settings_list_header);

        protected String mTitle;

        public HeaderEntry(String title) {
            mTitle = title;
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class HeaderEntryHolder extends SettingsEntryHolder<HeaderEntry> {

        protected TextView mTitle;

        public HeaderEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);

            mTitle = (TextView) itemView.findViewById(R.id.title);
        }

        @Override
        public void bind(HeaderEntry entry) {
            mTitle.setText(entry.mTitle);
        }

        @Override
        public boolean hasDivider() {
            return false;
        }
    }

    public static class SimpleEntry extends Entry {

        private static final int sHolder = registerViewHolder(SimpleEntryHolder.class, R.layout.settings_list_entry);

        protected String mName;
        protected String mValue;
        protected boolean mEnabled = true;
        private List<SettingChangedListener> mListeners = new ArrayList<>();

        public SimpleEntry(String name, String value) {
            mName = name;
            mValue = value;
        }

        public boolean isEnabled() {
            return mEnabled;
        }

        public void setEnabled(boolean enabled) {
            this.mEnabled = enabled;
            onUpdated();
        }

        public void addListener(SettingChangedListener listener) {
            mListeners.add(listener);
        }

        public void removeListener(SettingChangedListener listener) {
            mListeners.remove(listener);
        }

        @Override
        protected void onUpdated() {
            super.onUpdated();
            for (SettingChangedListener listener : mListeners)
                listener.onSettingChanged(this);
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class SimpleEntryHolder extends SettingsEntryHolder<SimpleEntry>
            implements View.OnClickListener {

        protected TextView mName;
        protected TextView mValue;

        public SimpleEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);

            mName = (TextView) itemView.findViewById(R.id.name);
            mValue = (TextView) itemView.findViewById(R.id.value);
            itemView.setOnClickListener(this);
        }

        @Override
        public void bind(SimpleEntry entry) {
            itemView.setEnabled(entry.mEnabled);
            mName.setEnabled(entry.mEnabled);
            mValue.setEnabled(entry.mEnabled);
            mName.setText(entry.mName);
            setValueText(entry.mValue);
        }

        protected void setValueText(String text) {
            mValue.setVisibility(text == null ? View.GONE : View.VISIBLE);
            mValue.setText(text);
        }

        protected void setValueText(int textId) {
            mValue.setVisibility(View.VISIBLE);
            mValue.setText(textId);
        }

        @Override
        public void onClick(View v) {
            //
        }

    }

    public static class CheckBoxEntry extends SimpleEntry {

        private static final int sHolder = registerViewHolder(CheckBoxEntryHolder.class, R.layout.settings_list_checkbox_entry);

        boolean mChecked;

        public CheckBoxEntry(String name, boolean checked) {
            super(name, null);
            mChecked = checked;
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
            onUpdated();
        }

        public boolean isChecked() {
            return mChecked;
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class CheckBoxEntryHolder extends SimpleEntryHolder
            implements CompoundButton.OnCheckedChangeListener {

        private CheckBox mCheckBox;

        public CheckBoxEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.check);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            CheckBoxEntry entry = (CheckBoxEntry) getEntry();
            entry.setChecked(!entry.isChecked());
        }

        @Override
        public void bind(SimpleEntry entry) {
            super.bind(entry);
            mCheckBox.setOnCheckedChangeListener(null);
            mCheckBox.setChecked(((CheckBoxEntry) entry).isChecked());
            mCheckBox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onClick(View v) {
            mCheckBox.setChecked(!mCheckBox.isChecked());
        }

    }

    public static class ListEntry extends SimpleEntry {

        private static final int sHolder = registerViewHolder(ListEntryHolder.class, R.layout.settings_list_entry);

        String[] mOptions;
        int mSelectedOption;

        public ListEntry(String name, String[] options, int selectedOption) {
            super(name, null);
            mOptions = options;
            mSelectedOption = selectedOption;
        }

        public void setSelectedOption(int index) {
            mSelectedOption = index;
            onUpdated();
        }

        public int getSelectedOption() {
            return mSelectedOption;
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class ListEntryHolder extends SimpleEntryHolder {

        public ListEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(SimpleEntry entry) {
            super.bind(entry);
            ListEntry listEntry = (ListEntry) entry;
            setValueText(listEntry.mOptions[listEntry.mSelectedOption]);
        }

        @Override
        public void onClick(View v) {
            ListEntry listEntry = (ListEntry) getEntry();
            new AlertDialog.Builder(v.getContext())
                    .setTitle(listEntry.mName)
                    .setSingleChoiceItems(listEntry.mOptions, listEntry.mSelectedOption,
                            (DialogInterface i, int which) -> {
                                listEntry.setSelectedOption(which);
                        i.cancel();
                    })
                    .setPositiveButton(R.string.action_cancel, null)
                    .show();
        }

    }

    public static class RingtoneEntry extends SimpleEntry implements ActivityResultCallback {

        private static final int sHolder = registerViewHolder(RingtoneEntryHolder.class, R.layout.settings_list_entry);

        SettingsListAdapter mAdapter;
        Uri mValue;
        int mRequestCode;

        public static String getValueDisplayString(Context context, Uri uri) {
            if (uri == null)
                return context.getString(R.string.value_none);
            if (uri.equals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)))
                return context.getString(R.string.value_default);
            Ringtone ret = RingtoneManager.getRingtone(context, uri);
            if (ret != null)
                return ret.getTitle(context);
            return null;
        }

        public RingtoneEntry(SettingsListAdapter adapter, String name, Uri value) {
            super(name, null);
            mAdapter = adapter;
            mValue = value;
            mRequestCode = adapter.mRequestCodeCounter.next();
        }

        public Uri getValue() {
            return mValue;
        }

        public void setValue(Uri mValue) {
            this.mValue = mValue;
            onUpdated();
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == mRequestCode && data != null) {
                mValue = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                onUpdated();
            }
        }

    }

    public static class RingtoneEntryHolder extends SimpleEntryHolder {

        public RingtoneEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(SimpleEntry entry) {
            super.bind(entry);
            setValueText(RingtoneEntry.getValueDisplayString(mAdapter.mActivity, ((RingtoneEntry) entry).mValue));
        }

        @Override
        public void onClick(View v) {
            RingtoneEntry entry = (RingtoneEntry) getEntry();
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, entry.mValue);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, entry.mName);
            intent.putExtra("android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS", 0x1 << 6); // Perhaps a bit of a hack, but imo needed
            mAdapter.mActivity.startActivityForResult(intent, entry.mRequestCode);
        }

    }

    public static class ColorEntry extends SimpleEntry {

        private static final int sHolder = registerViewHolder(ColorEntryHolder.class, R.layout.settings_list_entry_color);

        int[] mColors;
        String[] mColorNames;
        int mSelectedIndex;
        boolean mHasDefaultOption = false;

        public ColorEntry(String name, int[] colors, String[] colorNames, int selectedIndex) {
            super(name, null);
            mColors = colors;
            mColorNames = colorNames;
            mSelectedIndex = selectedIndex;
        }

        public void setSelectedColorIndex(int index) {
            mSelectedIndex = index;
            onUpdated();
        }

        public void setSelectedColor(int color) {
            for (int i = mColors.length - 1; i >= 0; i--) {
                if (mColors[i] == color) {
                    setSelectedColorIndex(i);
                    return;
                }
            }
            if (!mHasDefaultOption)
                throw new IndexOutOfBoundsException();
            setSelectedColorIndex(-1);
        }

        public int getSelectedColorIndex() {
            return mSelectedIndex;
        }

        public int getSelectedColor() {
            return mColors[mSelectedIndex];
        }

        public void setHasDefaultOption(boolean hasDefaultOption) {
            mHasDefaultOption = hasDefaultOption;
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class ColorEntryHolder extends SimpleEntryHolder {

        private ImageView mColor;

        public ColorEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mColor = (ImageView) itemView.findViewById(R.id.color);
        }

        @Override
        public void bind(SimpleEntry entry) {
            super.bind(entry);
            ColorEntry colorEntry = (ColorEntry) entry;
            if (colorEntry.mSelectedIndex == -1) {
                mColor.setColorFilter(0x00000000, PorterDuff.Mode.MULTIPLY);
                setValueText(R.string.value_default);
            } else {
                mColor.setColorFilter(colorEntry.mColors[colorEntry.mSelectedIndex], PorterDuff.Mode.MULTIPLY);
                setValueText(colorEntry.mColorNames[colorEntry.mSelectedIndex]);
            }
        }

        @Override
        public void onClick(View v) {
            ColorEntry entry = (ColorEntry) getEntry();
            ColorPickerDialog dialog = new ColorPickerDialog(v.getContext());
            dialog.setTitle(entry.mName);
            dialog.setColors(entry.mColors, entry.mSelectedIndex);
            dialog.setPositiveButton(R.string.action_cancel, null);
            dialog.setNegativeButton(R.string.value_default, (DialogInterface d, int which) -> {
                entry.setSelectedColorIndex(-1);
                dialog.cancel();
            });
            dialog.setOnColorChangeListener((ColorPickerDialog d, int colorIndex, int color) -> {
                entry.setSelectedColorIndex(colorIndex);
                dialog.cancel();
            });
            dialog.show();
        }

    }

    public interface SettingChangedListener {

        void onSettingChanged(Entry entry);

    }


}
