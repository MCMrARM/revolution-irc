package io.mrarm.irc.chat.preview.cache;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "images")
public class ImageCacheEntry {

    @PrimaryKey
    @ColumnInfo(name = "url")
    @NonNull
    private String mUrl;

    @ColumnInfo(name = "last_used")
    private long mLastUsed;

    public ImageCacheEntry(@NonNull String url, long lastUsed) {
        mUrl = url;
        mLastUsed = lastUsed;
    }

    @Ignore
    public ImageCacheEntry(@NonNull String url) {
        mUrl = url;
        mLastUsed = new Date().getTime();
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    public void setUrl(@NonNull String value) {
        mUrl = value;
    }

    public long getLastUsed() {
        return mLastUsed;
    }

    public void setLastUsed(long value) {
        mLastUsed = value;
    }

    public void updateLastUsed() {
        mLastUsed = new Date().getTime();
    }

}
