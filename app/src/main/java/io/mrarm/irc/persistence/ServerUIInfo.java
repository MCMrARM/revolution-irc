package io.mrarm.irc.persistence;

import android.content.Context;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import io.mrarm.irc.R;

@Entity(tableName = "server_ui_info")
public class ServerUIInfo {

    public static final String INTERACTION_TYPE_ADD = "add";
    public static final String INTERACTION_TYPE_EDIT = "edit";
    public static final String INTERACTION_TYPE_CONNECT = "connect";


    @PrimaryKey
    @NonNull
    public UUID uuid;

    @ColumnInfo(name = "cached_name")
    @NonNull
    public String cachedName;

    @ColumnInfo(name = "last_interaction_time")
    public Date lastInteractionTime;

    @ColumnInfo(name = "last_interaction_type")
    @Nullable
    public String lastInteractionType;


    public ServerUIInfo(@NonNull UUID uuid, @NonNull String cachedName) {
        this.uuid = uuid;
        this.cachedName = cachedName;
    }

    public String getLastInteractionText(Context context) {
        if (lastInteractionType == null || lastInteractionTime == null)
            return "";
        String interactionTimeStr = DateUtils.getRelativeDateTimeString(context,
                lastInteractionTime.getTime(), DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS, 0).toString();
        switch (lastInteractionType) {
            case INTERACTION_TYPE_ADD:
                return context.getString(R.string.server_list_last_created, interactionTimeStr);
            case INTERACTION_TYPE_EDIT:
                return context.getString(R.string.server_list_last_edited, interactionTimeStr);
            case INTERACTION_TYPE_CONNECT:
                return context.getString(R.string.server_list_last_edited, interactionTimeStr);
        }
        return "";
    }

}
