package io.mrarm.irc.setup;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.mrarm.irc.BackupManager;
import io.mrarm.irc.R;

public class BackupProgressActivity extends SetupProgressActivity {

    private static final int BACKUP_FILE_REQUEST_CODE = 1000;

    public static final String ARG_USER_PASSWORD = "password";

    private File mBackupFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BackupRequest request = new BackupRequest();
        request.password = getIntent().getStringExtra(ARG_USER_PASSWORD);
        new BackupTask(this).execute(request);
    }

    public void setDone(int resId) {
        Intent intent = new Intent(BackupProgressActivity.this, BackupCompleteActivity.class);
        intent.putExtra(BackupCompleteActivity.ARG_DESC_TEXT, resId);
        startNextActivity(intent);
    }

    public void onBackupDone(File file) {
        if (file == null) {
            setDone(R.string.error_generic);
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            mBackupFile = file;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip"));
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            intent.putExtra(Intent.EXTRA_TITLE, "irc-client-backup-" + date + ".zip");
            startActivityForResult(intent, BACKUP_FILE_REQUEST_CODE);
            setSlideAnimation(false);
        } else {
            // TODO:
        }
    }

    public void onBackupCopyDone(boolean success) {
        if (success)
            setDone(R.string.backup_created);
        else
            setDone(R.string.error_generic);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BACKUP_FILE_REQUEST_CODE) {
            if (data != null && data.getData() != null) {
                try {
                    Uri uri = data.getData();
                    ParcelFileDescriptor desc = getContentResolver().openFileDescriptor(uri, "w");
                    new SaveBackupTask(this, mBackupFile).execute(desc);
                } catch (IOException e) {
                    e.printStackTrace();
                    setDone(R.string.error_generic);
                }
            } else {
                mBackupFile.delete();
                setDone(R.string.backup_cancelled);
                setSlideAnimation(true);
            }
        }
    }

    private static class BackupRequest {
        public String password;
    }

    private class BackupTask extends AsyncTask<BackupRequest, Void, File> {
        private WeakReference<BackupProgressActivity> mActivity;

        public BackupTask(BackupProgressActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected File doInBackground(BackupRequest... backupRequests) {
            BackupRequest request = backupRequests[0];
            File backupFile = new File(getCacheDir(), "temp-backup.zip");
            if (backupFile.exists())
                backupFile.delete();
            backupFile.deleteOnExit(); // in case something fails
            try {
                BackupManager.createBackup(BackupProgressActivity.this, backupFile, request.password);
            } catch (IOException e) {
                e.printStackTrace();
                backupFile.delete();
                return null;
            }
            return backupFile;
        }

        @Override
        protected void onPostExecute(File file) {
            BackupProgressActivity activity = mActivity.get();
            if (activity != null)
                activity.onBackupDone(file);
        }
    }

    private static class SaveBackupTask extends AsyncTask<ParcelFileDescriptor, Void, Boolean> {
        private WeakReference<BackupProgressActivity> mActivity;
        private File mBackupFile;

        public SaveBackupTask(BackupProgressActivity activity, File file) {
            mActivity = new WeakReference<>(activity);
            mBackupFile = file;
        }

        @Override
        protected Boolean doInBackground(ParcelFileDescriptor... files) {
            try {
                ParcelFileDescriptor desc = files[0];
                FileOutputStream fos = new FileOutputStream(desc.getFileDescriptor());
                FileInputStream fis = new FileInputStream(mBackupFile);
                byte[] buf = new byte[1024 * 16];
                int c;
                while ((c = fis.read(buf, 0, buf.length)) > 0) {
                    fos.write(buf, 0, c);
                }
                fis.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            BackupProgressActivity activity = mActivity.get();
            if (activity != null)
                activity.onBackupCopyDone(success);
        }
    }

}
