package io.mrarm.irc.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface ServerUIInfoDao {

    @Query("SELECT * FROM server_ui_info ORDER BY last_interaction_time DESC")
    Single<List<ServerUIInfo>> getList();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    Single<Long> maybeCreate(ServerUIInfo entry);

    @Delete
    Completable delete(ServerUIInfo entry);

    @Update
    Completable update(ServerUIInfo entry);

}
