package io.mrarm.irc.chat.preview.cache;

import android.graphics.Bitmap;

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

    @ColumnInfo(name = "src_width")
    private int mSourceWidth;

    @ColumnInfo(name = "src_height")
    private int mSourceHeight;

    @ColumnInfo(name = "last_used")
    private long mLastUsed;

    private transient Bitmap mBitmap;

    public ImageCacheEntry(@NonNull String url, int sourceWidth, int sourceHeight, long lastUsed) {
        mUrl = url;
        mSourceWidth = sourceWidth;
        mSourceHeight = sourceHeight;
        mLastUsed = lastUsed;
    }

    @Ignore
    public ImageCacheEntry(@NonNull String url, int sourceWidth, int sourceHeight) {
        mUrl = url;
        mSourceWidth = sourceWidth;
        mSourceHeight = sourceHeight;
        mLastUsed = new Date().getTime();
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    public void setUrl(@NonNull String value) {
        mUrl = value;
    }

    public int getSourceWidth() {
        return mSourceWidth;
    }

    public void setSourceWidth(int value) {
        mSourceWidth = value;
    }

    public int getSourceHeight() {
        return mSourceHeight;
    }

    public void setSourceHeight(int value) {
        mSourceHeight = value;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap value) {
        mBitmap = value;
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
