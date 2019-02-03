package io.mrarm.irc.chat.preview.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface ImageCacheDao {

    @Query("SELECT * FROM images WHERE url=(:url)")
    ImageCacheEntry findEntryFor(String url);

    @Update
    void updateEntry(ImageCacheEntry entry);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEntry(ImageCacheEntry entry);

}
