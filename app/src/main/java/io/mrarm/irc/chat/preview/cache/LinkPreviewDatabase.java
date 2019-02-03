package io.mrarm.irc.chat.preview.cache;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 1, entities = {LinkPreviewInfo.class, ImageCacheEntry.class})
public abstract class LinkPreviewDatabase extends RoomDatabase {

    abstract public LinkPreviewDao linkPreviewDao();

    abstract public ImageCacheDao imageCacheDao();

}
