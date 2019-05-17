package io.mrarm.irc.persistence;

import androidx.room.TypeConverter;

import java.util.Date;
import java.util.UUID;

public class RoomDataConverters {

    @TypeConverter
    public static UUID convertFrom(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    @TypeConverter
    public static String convertTo(UUID value) {
        return value == null ? null : value.toString();
    }


    @TypeConverter
    public static Date convertFrom(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long convertTo(Date value) {
        return value == null ? null : value.getTime();
    }

}
