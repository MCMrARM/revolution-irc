package io.mrarm.irc.config;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;

import io.mrarm.irc.dagger.AppQualifier;

public class AppServerChatLogManager implements ServerChatLogManager {

    private static final String SERVER_LOGS_PATH = "chat-logs";
    private static final String SERVER_MISC_DATA_FILENAME = "misc-data.db";

    private final File mServerLogsPath;
    private final File mFallbackServerLogsPath;

    @Inject
    public AppServerChatLogManager(@AppQualifier Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        mFallbackServerLogsPath = new File(context.getFilesDir(), SERVER_LOGS_PATH);
        mServerLogsPath = externalFilesDir != null ? new File(externalFilesDir, SERVER_LOGS_PATH)
                : mFallbackServerLogsPath;
        mServerLogsPath.mkdirs();

        if (externalFilesDir != null && mFallbackServerLogsPath.exists()) {
            migrateServerLogs(mFallbackServerLogsPath, mServerLogsPath);
            mFallbackServerLogsPath.delete();
        }
    }

    private void migrateServerLogs(File from, File to) {
        File[] files = from.listFiles();
        if (files == null)
            return;
        to.mkdir();
        for (File file : files) {
            if (file.isDirectory()) {
                migrateServerLogs(file, new File(to, file.getName()));
            } else {
                File toFile = new File(to, file.getName());
                File tempFile = new File(to, file.getName() + ".tmp");
                try {
                    FileInputStream fis = new FileInputStream(file);
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    byte[] buf = new byte[16 * 1024];
                    int n;
                    while ((n = fis.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                    }
                    fis.close();
                    fos.close();
                } catch (IOException e) {
                    throw new RuntimeException("Migration failed", e);
                }
                if (toFile.exists())
                    toFile.delete();
                tempFile.renameTo(toFile);
            }
            file.delete();
        }
    }

    @Override
    public File getChatLogDir() {
        return mServerLogsPath;
    }

    @Override
    public File getServerChatLogDir(UUID uuid) {
        return new File(mServerLogsPath, uuid.toString());
    }

    @Override
    public File getServerMiscDataFile(UUID uuid) {
        return new File(getServerChatLogDir(uuid), SERVER_MISC_DATA_FILENAME);
    }

}
