package com.marverenic.music.player.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface PlaybackItemDao {

    @Query("SELECT COUNT(*) FROM queue_items")
    int getPlaybackItemCount();

    @Query("SELECT COUNT(*) FROM playback_metadata")
    int getMetadataItemCount();

    @Query("SELECT * FROM queue_items where list_name IS :listName ORDER BY idx ASC")
    List<PlaybackItem> getPlaybackItems(String listName);

    @Query("DELETE FROM queue_items where list_name IS :listName")
    void clearPlaybackItems(String listName);

    @Insert
    void setPlaybackItems(List<PlaybackItem> playbackItems);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void putMetadataItem(PlaybackMetadataItem item);

    @Query("DELETE FROM playback_metadata where `key` is :key")
    void deleteMetadataItem(String key);

    @Query("SELECT * FROM playback_metadata WHERE `key` IS :key LIMIT 1")
    PlaybackMetadataItem getMetadataItem(String key);

}
