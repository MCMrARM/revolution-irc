package io.mrarm.irc.persistence;

import android.content.Context;

import androidx.room.Room;

public class UIStorage {

    private static UIStorage singleton;

    public static UIStorage getInstance(Context context) {
        if (singleton == null)
            singleton = new UIStorage(context.getApplicationContext());
        return singleton;
    }


    private UIDatabase database;

    public UIStorage(Context context) {
        database = Room.databaseBuilder(context, UIDatabase.class, "ui-storage").build();
    }

    public UIDatabase getDatabase() {
        return database;
    }

}
