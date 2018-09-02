package com.marverenic.music.player.persistence;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.support.annotation.NonNull;

@Entity(
        tableName = "queue_items",
        primaryKeys = { "list_name", "idx" }
)
public class PlaybackItem {

    @NonNull
    @ColumnInfo(name = "list_name")
    public String listName;

    @ColumnInfo(name = "idx")
    public int index;

    @NonNull
    @ColumnInfo(name = "uri")
    public String songUri;

    PlaybackItem(@NonNull String listName, int index, @NonNull String songUri) {
        this.listName = listName;
        this.index = index;
        this.songUri = songUri;
    }
}
