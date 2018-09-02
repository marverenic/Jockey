package com.marverenic.music.player.persistence;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

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
