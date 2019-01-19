package io.mrarm.irc.setting;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.mrarm.irc.R;

public class ListWithCustomSetting extends ListSetting implements
        SettingsListAdapter.ActivityResultCallback {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    public static final String CUSTOM_VALUE_PREFIX = "custom:";
    public static final String FILES_DIR = "pref_values";
    public static final String FILENAME_PREFIX = "pref_";
    public static final String FILENAME_TEMP_PREFIX = "temp_";

    public static final int TYPE_STRING = 0;
    public static final int TYPE_NUMBER = 1;
    public static final int TYPE_FILE = 2;
    public static final int TYPE_FONT = 3;

    public static boolean isPrefCustomValue(String value) {
        return value != null && value.startsWith(CUSTOM_VALUE_PREFIX);
    }

    public static String getPrefCustomValue(String value) {
        if (!isPrefCustomValue(value))
            return null;
        return value.substring(CUSTOM_VALUE_PREFIX.length());
    }

    public static File getCustomFile(File customFilesDir, String key, String filename) {
        int iof = filename.lastIndexOf('.');
        if (iof == -1)
            return null;
        String ext = filename.substring(iof + 1);
        return new File(customFilesDir, FILENAME_PREFIX + key + "." + ext);
    }

    public static File getCustomFile(Context context, String key, String filename) {
        return getCustomFile(getCustomFilesDir(context), key, filename);
    }

    private int mCustomValueType = TYPE_STRING;
    private int mRequestCode = -1;
    private String mCustomValue;
    private String mInternalFileName;
    private File mCustomFilesDir;

    public ListWithCustomSetting(SettingsListAdapter adapter, String name, String[] options,
                                 int selectedOption, String internalFileName, int customValueType) {
        super(name, options, selectedOption);
        mInternalFileName = internalFileName;
        mCustomValueType = customValueType;
        mRequestCode = adapter.getRequestCodeCounter().next();
        mCustomFilesDir = getCustomFilesDir(adapter.getActivity());
    }

    public ListWithCustomSetting(SettingsListAdapter adapter, String name, String[] options,
                                 String[] optionValues, String selectedOption,
                                 String internalFileName, int customValueType) {
        super(name, options, optionValues, selectedOption);
        mInternalFileName = internalFileName;
        mCustomValueType = customValueType;
        mRequestCode = adapter.getRequestCodeCounter().next();
        mCustomFilesDir = getCustomFilesDir(adapter.getActivity());
    }

    public ListWithCustomSetting(String name, String[] options, int selectedOption,
                                 int customValueType) {
        super(name, options, selectedOption);
        mCustomValueType = customValueType;
    }

    @Override
    public ListSetting linkPreference(SharedPreferences prefs, String pref) {
        String s = prefs.getString(pref, null);
        if (isPrefCustomValue(s))
            setCustomValue(getPrefCustomValue(s));
        return super.linkPreference(prefs, pref);
    }

    @Override
    public void setSelectedOption(int index) {
        if (index != -1 && isValueTypeFile()) {
            File f = getCustomFile();
            if (f != null && f.exists())
                f.delete();
        }
        super.setSelectedOption(index);
    }

    public boolean hasCustomValue() {
        return mCustomValue != null;
    }

    public String getCustomValue() {
        return mCustomValue;
    }

    public void setCustomValue(String value) {
        mCustomValue = value;
        if (value != null && hasAssociatedPreference())
            mPreferences.edit()
                    .putString(mPreferenceName, CUSTOM_VALUE_PREFIX + value)
                    .apply();
        setSelectedOption(-1);
    }

    public boolean isValueTypeFile() {
        return mCustomValueType == TYPE_FILE || mCustomValueType == TYPE_FONT;
    }

    public static File getCustomFilesDir(Context context) {
        File dir = new File(context.getFilesDir(), FILES_DIR);
        if (!dir.exists())
            dir.mkdir();
        return dir;
    }

    public File getCustomFile() {
        if (!hasCustomValue() || mInternalFileName == null)
            return null;
        return getCustomFile(mCustomFilesDir, mInternalFileName, getCustomValue());
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (mRequestCode == requestCode && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                String name = cursor.getString(nameIndex);
                File outFile = getCustomFile(activity, mInternalFileName, name);
                if (outFile == null)
                    throw new IOException();
                File tempOutFile = new File(outFile.getParent(), FILENAME_TEMP_PREFIX + outFile.getName());

                ParcelFileDescriptor desc = activity.getContentResolver().openFileDescriptor(uri, "r");
                FileInputStream fis = new FileInputStream(desc.getFileDescriptor());
                FileOutputStream fos = new FileOutputStream(tempOutFile);
                byte[] buf = new byte[1024 * 16];
                int c;
                while ((c = fis.read(buf, 0, buf.length)) > 0) {
                    fos.write(buf, 0, c);
                }
                if (mCustomValueType == TYPE_FONT) {
                    try {
                        Typeface.createFromFile(tempOutFile);
                    } catch (Exception e) {
                        throw new IOException("Failed to load font", e);
                    }
                }
                if (outFile.exists())
                    outFile.delete();
                tempOutFile.renameTo(outFile);
                setCustomValue(name);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(activity, R.string.error_file_open, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Holder extends SimpleSetting.Holder<ListWithCustomSetting> {

        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(ListWithCustomSetting entry) {
            super.bind(entry);
            if (entry.hasCustomValue())
                setValueText(entry.getCustomValue());
        }

        protected void openCustomValueDialog() {
            if (getEntry().isValueTypeFile()) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                mAdapter.startActivityForResult(intent, getEntry().mRequestCode);
            }
        }

        @Override
        public void onClick(View v) {
            ListWithCustomSetting entry = getEntry();

            boolean custom = entry.hasCustomValue();
            CharSequence[] entries = entry.getOptions();
            CharSequence[] entriesWithCustom = new CharSequence[entries.length + (custom ? 2 : 1)];
            System.arraycopy(entries, 0, entriesWithCustom, 0, entries.length);
            if (custom)
                entriesWithCustom[entries.length] = v.getContext().getString(
                        R.string.value_custom_specific, entry.getCustomValue());
            entriesWithCustom[entriesWithCustom.length - 1] = v.getContext().getString(
                    R.string.value_custom);
            new AlertDialog.Builder(v.getContext())
                    .setTitle(entry.mName)
                    .setSingleChoiceItems(entriesWithCustom,
                            custom ? entries.length : entry.getSelectedOption(),
                            (DialogInterface dialog, int which) -> {
                                if (which < entries.length) {
                                    entry.setSelectedOption(which);
                                } else if (which == entriesWithCustom.length - 1) {
                                    openCustomValueDialog();
                                }
                                dialog.dismiss();
                            })
                    .show();

        }

    }

}
