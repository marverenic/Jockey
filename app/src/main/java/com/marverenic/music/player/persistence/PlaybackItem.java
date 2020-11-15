package com.marverenic.music.player.persistence;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.annotation.NonNull;

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
