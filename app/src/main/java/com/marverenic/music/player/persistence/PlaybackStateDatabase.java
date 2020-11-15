package com.marverenic.music.player.persistence;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {PlaybackItem.class, PlaybackMetadataItem.class},
        version = 1,
        exportSchema = false
)
public abstract class PlaybackStateDatabase extends RoomDatabase {

    public abstract PlaybackItemDao getPlaybackItemDao();

}
