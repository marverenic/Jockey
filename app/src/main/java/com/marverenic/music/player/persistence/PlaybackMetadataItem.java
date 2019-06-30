package com.marverenic.music.player.persistence;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "playback_metadata")
public class PlaybackMetadataItem {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "key")
    public String key;

    @ColumnInfo(name = "value")
    public long value;

    public PlaybackMetadataItem(@NonNull String key, long value) {
        this.key = key;
        this.value = value;
    }
}
