package io.mrarm.irc.persistence;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {ServerUIInfo.class}, version = 1)
@TypeConverters({RoomDataConverters.class})
public abstract class UIDatabase extends RoomDatabase {

    public abstract ServerUIInfoDao serverInfoDao();

}
