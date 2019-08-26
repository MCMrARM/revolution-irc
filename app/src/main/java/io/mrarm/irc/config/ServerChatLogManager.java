package io.mrarm.irc.config;

import java.io.File;
import java.util.UUID;

/**
 * An interface to help manage the location of the server chat logs.
 *
 * Required to be thread safe.
 */
public interface ServerChatLogManager {

    File getChatLogDir();

    File getServerChatLogDir(UUID uuid);

    File getServerMiscDataFile(UUID uuid);

    default void deleteServerLogs(UUID uuid) {
        File file = getServerChatLogDir(uuid);
        if (file != null && file.exists()) {
            File[] files = file.listFiles();
            for (File f : files)
                f.delete();
            file.delete();
        }
    }

}
