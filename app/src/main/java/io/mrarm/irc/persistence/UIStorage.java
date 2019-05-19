package io.mrarm.irc.persistence;

import android.content.Context;

import androidx.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.mrarm.irc.dagger.AppQualifier;

@Module
public class UIStorage {

    @Provides
    @Singleton
    public UIDatabase getDatabase(@AppQualifier Context context) {
        return Room.databaseBuilder(context, UIDatabase.class, "ui-storage").build();
    }

}
