package io.mrarm.irc.setting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.View;

import io.mrarm.irc.R;

public class RingtoneSetting extends SimpleSetting implements
        SettingsListAdapter.ActivityResultCallback {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    private Uri mValue;
    private int mRequestCode;

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

    public RingtoneSetting(SettingsListAdapter adapter, String name, Uri value) {
        super(name, null);
        mValue = value;
        mRequestCode = adapter.getRequestCodeCounter().next();
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
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == mRequestCode && data != null) {
            mValue = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            onUpdated();
        }
    }

    public static class Holder extends SimpleSetting.Holder<RingtoneSetting> {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(RingtoneSetting entry) {
            super.bind(entry);
            setValueText(getValueDisplayString(mAdapter.getActivity(), entry.mValue));
        }

        @Override
        public void onClick(View v) {
            RingtoneSetting entry = getEntry();
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, entry.mValue);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, entry.mName);
            intent.putExtra("android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS", 0x1 << 6); // Perhaps a bit of a hack, but imo needed
            mAdapter.startActivityForResult(intent, entry.mRequestCode);
        }

    }

}
