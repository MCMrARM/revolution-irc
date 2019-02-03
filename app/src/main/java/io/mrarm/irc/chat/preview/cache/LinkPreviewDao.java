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

}
