package io.mrarm.irc.chat.preview.cache;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

public class LinkPreviewCacheManager {

    private final LinkPreviewDatabase mDatabase;
    private final ImageCacheManager mImageCache;

    public LinkPreviewCacheManager(Context context) {
        mDatabase = Room
                .databaseBuilder(context, LinkPreviewDatabase.class, "link-preview-cache")
                .openHelperFactory(new FrameworkSQLiteOpenHelperFactory())
                .build();
        mImageCache = new ImageCacheManager(context, mDatabase);
    }

    public LinkPreviewDatabase getDatabase() {
        return mDatabase;
    }

    public ImageCacheManager getImageCache() {
        return mImageCache;
    }

    public void deleteLeastRecentlyUsedPreviews() {
        while (true) {
            int cnt = mDatabase.linkPreviewDao().deleteLeastRecentlyUsed();
            if (cnt <= 0)
                break;
            Log.d("LinkPreviewCacheManager", "Deleted " + cnt +
                    " least recently used items");
        }
    }

}
