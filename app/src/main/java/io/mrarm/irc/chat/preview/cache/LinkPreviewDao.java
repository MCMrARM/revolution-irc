package io.mrarm.irc.chat.preview.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface LinkPreviewDao {

    @Query("SELECT * FROM link_previews WHERE url=(:url)")
    LinkPreviewInfo findPreviewFor(String url);

    @Insert
    void insertPreview(LinkPreviewInfo preview);

    @Query("UPDATE link_previews SET last_used=(:time) WHERE url=(:url)")
    void updateLastUsed(String url, long time);

    @Query("DELETE FROM link_previews WHERE url IN (SELECT url FROM link_previews ORDER BY last_used DESC LIMIT 100 OFFSET 100)")
    int deleteLeastRecentlyUsed();

}
