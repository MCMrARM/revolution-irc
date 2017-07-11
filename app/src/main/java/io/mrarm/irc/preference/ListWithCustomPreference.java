package io.mrarm.irc.preference;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.ListPreference;
import android.provider.OpenableColumns;
import android.util.AttributeSet;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.mrarm.irc.R;
import io.mrarm.irc.SettingsActivity;

public class ListWithCustomPreference extends ListPreference
        implements SettingsActivity.ActivityResultCallback {

    public static final String CUSTOM_VALUE_PREFIX = "custom:";
    public static final String FILES_DIR = "pref_values";
    public static final String FILENAME_PREFIX = "pref_";
    public static final String FILENAME_TEMP_PREFIX = "temp_";

    public static final int TYPE_STRING = 0;
    public static final int TYPE_NUMBER = 1;
    public static final int TYPE_FILE = 2;
    public static final int TYPE_FONT = 3;

    private int mCustomValueType = TYPE_STRING;
    private int mRequestCode = -1;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListWithCustomPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListWithCustomPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public ListWithCustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ListWithCustomPreference(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.ListWithCustomPreference);
        mCustomValueType = ta.getInt(R.styleable.ListWithCustomPreference_customValueType, TYPE_STRING);
        ta.recycle();
    }

    @Override
    public String getValue() {
        return hasCustomValue() ? getCustomValue() : super.getValue();
    }

    public static boolean isCustomValue(String value) {
        return value.startsWith(CUSTOM_VALUE_PREFIX);
    }

    public boolean hasCustomValue() {
        return isCustomValue(super.getValue());
    }

    public static String getCustomValue(String value) {
        if (!isCustomValue(value))
            return null;
        return value.substring(CUSTOM_VALUE_PREFIX.length());
    }

    public String getCustomValue() {
        return getCustomValue(super.getValue());
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

    public static File getCustomFile(Context context, String key, String filename) {
        int iof = filename.lastIndexOf('.');
        if (iof == -1)
            return null;
        String ext = filename.substring(iof + 1);
        return new File(getCustomFilesDir(context), FILENAME_PREFIX + key + "." + ext);
    }

    public File getCustomFile() {
        if (!hasCustomValue())
            return null;
        return getCustomFile(getContext(), getKey(), getCustomValue());
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        boolean custom = hasCustomValue();
        CharSequence[] entries = getEntries();
        CharSequence[] entriesWithCustom = new CharSequence[entries.length + (custom ? 2 : 1)];
        System.arraycopy(entries, 0, entriesWithCustom, 0, entries.length);
        if (custom)
            entriesWithCustom[entries.length] = getContext().getString(
                    R.string.value_custom_specific, getCustomValue());
        entriesWithCustom[entriesWithCustom.length - 1] = getContext().getString(
                R.string.value_custom);
        builder.setSingleChoiceItems(entriesWithCustom,
                custom ? entries.length : findIndexOfValue(getValue()),
                (DialogInterface dialog, int which) -> {
            if (which < entries.length) {
                String value = getEntryValues()[which].toString();
                if (callChangeListener(value)) {
                    if (hasCustomValue() && isValueTypeFile())
                        getCustomFile().delete();
                    setValue(value);
                }
            } else if (which == entriesWithCustom.length - 1) {
                openCustomValueDialog();
            }
            dialog.dismiss();
        });
        builder.setPositiveButton(null, null);
    }

    protected void openCustomValueDialog() {
        if (isValueTypeFile()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            if (mRequestCode == -1)
                mRequestCode = ((SettingsActivity) getContext()).getRequestCodeCounter().next();
            ((SettingsActivity) getContext()).startActivityForResult(intent, mRequestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRequestCode == requestCode && data != null && data.getData() != null) {
            String prevValue = getValue();
            try {
                Uri uri = data.getData();
                Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                String name = cursor.getString(nameIndex);
                File outFile = getCustomFile(getContext(), getKey(), name);
                if (outFile == null)
                    throw new IOException();
                File tempOutFile = new File(outFile.getParent(), FILENAME_TEMP_PREFIX + outFile.getName());

                if (!callChangeListener(CUSTOM_VALUE_PREFIX + name))
                    return;

                ParcelFileDescriptor desc = getContext().getContentResolver().openFileDescriptor(uri, "r");
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
                setValue(CUSTOM_VALUE_PREFIX + name);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), R.string.error_file_open, Toast.LENGTH_SHORT).show();
                if (!callChangeListener(prevValue))
                    setValue(prevValue);
            }
        }
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        ((SettingsActivity) getContext()).addActivityResultCallback(this);
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        ((SettingsActivity) getContext()).removeActivityResultCallback(this);
    }
}
