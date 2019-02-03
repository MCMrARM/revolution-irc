package io.mrarm.irc.chat.preview.cache;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ImageCacheDao {

    @Query("SELECT * FROM images WHERE url=(:url)")
    ImageCacheEntry findEntryFor(String url);

    @Query("UPDATE images SET last_used=(:time) WHERE url=(:url)")
    void updateLastUsed(String url, long time);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEntry(ImageCacheEntry entry);

    @Query("SELECT url FROM images ORDER BY last_used DESC LIMIT 100 OFFSET 100")
    List<String> getItemsToDelete();

    @Query("DELETE FROM images WHERE url IN (:items)")
    void deleteItems(List<String> items);

}
