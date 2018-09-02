package com.marverenic.music.player.persistence;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(
        entities = {PlaybackItem.class, PlaybackMetadataItem.class},
        version = 1,
        exportSchema = false
)
public abstract class PlaybackStateDatabase extends RoomDatabase {

    public abstract PlaybackItemDao getPlaybackItemDao();

}
