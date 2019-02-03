package io.mrarm.irc.chat.preview.cache;

import android.content.Context;

import androidx.room.Room;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

public class LinkPreviewCacheManager {

    private Context mContext;
    private LinkPreviewDatabase mDatabase;

    public LinkPreviewCacheManager(Context context) {
        mContext = context;
        mDatabase = Room
                .databaseBuilder(mContext, LinkPreviewDatabase.class, "link-preview-cache")
                .openHelperFactory(new FrameworkSQLiteOpenHelperFactory())
                .build();
    }

    public LinkPreviewDatabase getDatabase() {
        return mDatabase;
    }
}
